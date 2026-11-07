package org.miniopensource.miniclassisland

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject

data class ContainerBreadcrumb(
    val title: String,
    val lineIndex: Int,
    val pathIndices: List<Int>
)

class ProfileEditorComponent(internal val act: MainActivity) {
    internal val resourceTypes = listOf(
        "Subjects" to "科目管理",
        "TimeLayout" to "时间表管理",
        "ClassPlan" to "课表管理",
        "Policy" to "策略管理",
        "DefaultSettings" to "默认设置",
        "Components" to "组件设置",
        "Credentials" to "凭据管理"
    )

    internal val resourceSubtitles = mapOf(
        "Subjects" to "编辑科目列表、科目简称、任课教师与户外课程属性",
        "TimeLayout" to "维护标准作息时间表，管理上课、课间、分割线与自动行动时间点",
        "ClassPlan" to "编排每日课程表、课表群组、多周轮换规则并关联对应作息时间表",
        "Policy" to "控制客户端允许用户修改的功能权限及是否允许退出集控",
        "DefaultSettings" to "对照上游应用设置导航，统一管理终端默认设置与参数覆盖",
        "Components" to "管理主界面多行组件布局、嵌套容器编排与组件专属参数",
        "Credentials" to "配置客户端操作授权密码凭据与 9 项敏感操作的权限验证等级"
    )

    var selectedType: String = "Subjects"
    var resourceFiles: List<String> = emptyList()
    var selectedResourceName: String = ""
    var editMode: String = "visual" // "visual" | "raw"
    var rawJsonContent: String = "{}"
    var newFileName: String = ""
    var showCreateBar: Boolean = false

    // Version History state
    var showHistoryPanel: Boolean = false
    var historyItems: JSONArray = JSONArray()
    var previewHistoryVersion: Int = -1

    // Reference lookup caches for ClassPlan / TimeLayout
    var subjectRefFiles: List<String> = emptyList()
    var selectedSubjectRefFile: String = ""
    var refSubjectsMap: Map<String, String> = emptyMap()

    var timeLayoutRefFiles: List<String> = emptyList()
    var selectedTimeLayoutRefFile: String = ""
    var refTimeLayoutsList: List<Pair<String, String>> = emptyList()
    var refTimeLayoutSlotsMap: Map<String, JSONArray> = emptyMap()

    // Inner state for TimeLayout / ClassPlan
    var activeGroupKey: String = ""
    var selectedClassPlanGroupFilter: String = "all"
    var newClassPlanGroupName: String = ""
    var showBreakAndActionSlots: Boolean = false
    var selectedSlotIndex: Int = 0
    var autoMoveNextSlot: Boolean = true
    var classPlanRightTab: String = "picker" // "picker" | "rules"

    // Inner state for DefaultSettings (15 upstream tabs)
    var activeSettingsTab: String = "general"
    var presetIdentityInput: String = ""

    // Inner state for Components
    var selectedLineIndex: Int = 0
    var selectedCompIndex: Int = 0
    var containerStack: MutableList<ContainerBreadcrumb> = mutableListOf()
    var activeComponentBottomTab: String = "library" // "library" | "settings" | "advanced" | "line"
    var customPluginComponentId: String = ""
    var customPluginComponentName: String = ""

    // Inner state for Credentials
    var plainUserPasswordInput: String = ""
    var plainAdminPasswordInput: String = ""

    fun selectResourceTypeFromSidebar(type: String) {
        selectedType = type
        selectedResourceName = ""
        showCreateBar = false
        showHistoryPanel = false
        previewHistoryVersion = -1
        containerStack.clear()
        loadResourcesList()
    }

    fun loadResourcesList() {
        val accId = act.currentAccountId
        if (accId.isBlank()) {
            act.renderUI()
            return
        }
        val type = selectedType
        act.runAsync({
            val files = ApiClient.listResourceFiles(accId, type)
            val sFiles = ApiClient.listResourceFiles(accId, "Subjects")
            val tlFiles = ApiClient.listResourceFiles(accId, "TimeLayout")

            val sRefName = selectedSubjectRefFile.takeIf { sFiles.contains(it) } ?: sFiles.firstOrNull().orEmpty()
            val tlRefName = selectedTimeLayoutRefFile.takeIf { tlFiles.contains(it) } ?: tlFiles.firstOrNull().orEmpty()

            val sContent = if (sRefName.isNotEmpty()) ApiClient.readResourceFile(accId, "Subjects", sRefName) else null
            val tlContent = if (tlRefName.isNotEmpty()) ApiClient.readResourceFile(accId, "TimeLayout", tlRefName) else null

            val targetFile = selectedResourceName.takeIf { files.contains(it) } ?: files.firstOrNull().orEmpty()
            val fileRes = if (targetFile.isNotEmpty()) ApiClient.readResourceFile(accId, type, targetFile) else null

            listOf(files, sFiles, tlFiles, sRefName, tlRefName, sContent, tlContent, targetFile, fileRes)
        }) { result ->
            @Suppress("UNCHECKED_CAST")
            val files = result[0] as List<String>
            @Suppress("UNCHECKED_CAST")
            val sFiles = result[1] as List<String>
            @Suppress("UNCHECKED_CAST")
            val tlFiles = result[2] as List<String>
            val sRefName = result[3] as String
            val tlRefName = result[4] as String
            val sContent = result[5] as HttpRes?
            val tlContent = result[6] as HttpRes?
            val targetFile = result[7] as String
            val fileRes = result[8] as HttpRes?

            resourceFiles = files
            subjectRefFiles = sFiles
            timeLayoutRefFiles = tlFiles
            selectedSubjectRefFile = sRefName
            selectedTimeLayoutRefFile = tlRefName

            parseReferenceData(sContent, tlContent)

            if (targetFile.isNotEmpty() && fileRes != null && fileRes.ok) {
                selectedResourceName = targetFile
                rawJsonContent = try {
                    fileRes.json.toString(2)
                } catch (_: Exception) {
                    fileRes.rawText.ifBlank { ApiClient.getDefaultResourceJson(selectedType) }
                }
                activeGroupKey = ""
                previewHistoryVersion = -1
            } else if (files.isEmpty()) {
                selectedResourceName = ""
                rawJsonContent = ApiClient.getDefaultResourceJson(selectedType)
                activeGroupKey = ""
            }
            act.renderUI()
        }
    }

    internal fun reloadReferencesOnly() {
        val accId = act.currentAccountId
        if (accId.isBlank()) return
        val sRefName = selectedSubjectRefFile
        val tlRefName = selectedTimeLayoutRefFile
        act.runAsync({
            val sContent = if (sRefName.isNotEmpty()) ApiClient.readResourceFile(accId, "Subjects", sRefName) else null
            val tlContent = if (tlRefName.isNotEmpty()) ApiClient.readResourceFile(accId, "TimeLayout", tlRefName) else null
            Pair(sContent, tlContent)
        }) { (sContent, tlContent) ->
            parseReferenceData(sContent, tlContent)
            act.renderUI()
        }
    }

    private fun parseReferenceData(sContent: HttpRes?, tlContent: HttpRes?) {
        val sMap = linkedMapOf<String, String>()
        if (sContent != null && sContent.ok) {
            val subs = sContent.json.optJSONObject("Subjects")
            if (subs != null) {
                val keys = subs.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val name = subs.optJSONObject(k)?.optString("Name").orEmpty().ifBlank { k.take(8) }
                    sMap[k] = name
                }
            }
        }
        refSubjectsMap = sMap

        val tlList = mutableListOf<Pair<String, String>>()
        val slotsMap = mutableMapOf<String, JSONArray>()
        if (tlContent != null && tlContent.ok) {
            val layouts = tlContent.json.optJSONObject("TimeLayouts")
            if (layouts != null) {
                val keys = layouts.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val itemObj = layouts.optJSONObject(k)
                    val name = itemObj?.optString("Name").orEmpty().ifBlank { k.take(8) }
                    tlList.add(k to name)
                    slotsMap[k] = itemObj?.optJSONArray("Layouts") ?: JSONArray()
                }
            }
        }
        refTimeLayoutsList = tlList
        refTimeLayoutSlotsMap = slotsMap
    }

    internal fun selectFileByName(name: String) {
        val accId = act.currentAccountId
        if (accId.isBlank() || name.isBlank()) return
        val type = selectedType
        act.runAsync({
            ApiClient.readResourceFile(accId, type, name)
        }) { res ->
            if (res.ok) {
                selectedResourceName = name
                rawJsonContent = try {
                    res.json.toString(2)
                } catch (_: Exception) {
                    res.rawText.ifBlank { ApiClient.getDefaultResourceJson(selectedType) }
                }
                activeGroupKey = ""
                showHistoryPanel = false
                previewHistoryVersion = -1
                containerStack.clear()
            } else {
                act.showBanner("读取文件失败: ${res.error}", true)
            }
            act.renderUI()
        }
    }

    internal fun loadFileHistory() {
        val accId = act.currentAccountId
        val name = selectedResourceName
        if (accId.isBlank() || name.isBlank()) return
        val type = selectedType
        act.runAsync({
            ApiClient.listResourceHistory(accId, type, name)
        }) { arr ->
            historyItems = arr
            showHistoryPanel = true
            act.renderUI()
        }
    }

    internal fun loadHistoryVersionContent(version: Int) {
        val accId = act.currentAccountId
        val name = selectedResourceName
        if (accId.isBlank() || name.isBlank()) return
        val type = selectedType
        act.runAsync({
            ApiClient.readResourceHistoryVersion(accId, type, name, version)
        }) { res ->
            if (res.ok) {
                val contentObj = res.json.optJSONObject("content") ?: res.json
                rawJsonContent = contentObj.toString(2)
                previewHistoryVersion = version
                act.showBanner("已载入历史版本 v$version，点击「保存」可将其恢复为当前最新版本", false)
            } else {
                act.showBanner("读取历史版本失败: ${res.error}", true)
            }
            act.renderUI()
        }
    }

    internal fun parseCurrentJson(): JSONObject =
        try {
            JSONObject(rawJsonContent)
        } catch (_: Exception) {
            JSONObject(ApiClient.getDefaultResourceJson(selectedType))
        }

    internal fun commitJsonAndRefresh(root: JSONObject) {
        rawJsonContent = root.toString(2)
        act.renderUI()
    }

    internal fun createAndSaveNewFile(nameToCreate: String) {
        val targetName = nameToCreate.trim()
        if (targetName.isEmpty()) {
            act.showBanner("请输入新配置文件名称", true)
            return
        }
        val accId = act.currentAccountId
        val type = selectedType
        val initialPayload = JSONObject(ApiClient.getDefaultResourceJson(type))
        act.runAsync({
            val createRes = ApiClient.createResourceFile(accId, type, targetName)
            val writeRes = ApiClient.writeResourceFile(accId, type, targetName, initialPayload)
            Pair(createRes, writeRes)
        }) { (_, writeRes) ->
            if (writeRes.ok) {
                selectedResourceName = targetName
                showCreateBar = false
                newFileName = ""
                act.showBanner("已创建并初始化 $type / $targetName", false)
                loadResourcesList()
            } else {
                act.showBanner("创建失败: ${writeRes.error}", true)
                act.renderUI()
            }
        }
    }

    internal fun saveCurrentResource(nameToSave: String = selectedResourceName) {
        val targetName = nameToSave.trim()
        if (targetName.isEmpty()) {
            act.showBanner("请先选择或新建资源文件", true)
            return
        }
        val payloadObj = try {
            JSONObject(rawJsonContent)
        } catch (e: Exception) {
            act.showBanner("JSON 语法错误: ${e.message}", true)
            return
        }
        val accId = act.currentAccountId
        val type = selectedType
        act.runAsync({
            ApiClient.writeResourceFile(accId, type, targetName, payloadObj)
        }) { res ->
            if (res.ok) {
                selectedResourceName = targetName
                showCreateBar = false
                previewHistoryVersion = -1
                act.showBanner("已保存并发布 $type / $targetName", false)
                loadResourcesList()
            } else {
                act.showBanner("保存失败: ${res.error}", true)
                act.renderUI()
            }
        }
    }

    internal fun deleteCurrentResource() {
        val targetName = selectedResourceName.trim()
        if (targetName.isEmpty()) return
        val accId = act.currentAccountId
        val type = selectedType
        act.runAsync({
            ApiClient.deleteResourceFile(accId, type, targetName)
        }) { res ->
            if (res.ok) {
                act.showBanner("已删除档案文件 $targetName", false)
                selectedResourceName = ""
                loadResourcesList()
            } else {
                act.showBanner("删除失败: ${res.error}", true)
                act.renderUI()
            }
        }
    }

    fun buildView(ctx: Context, isTablet: Boolean): View {
        val root = UiKit.vBox(ctx)
        val typeTitle = resourceTypes.find { it.first == selectedType }?.second ?: selectedType
        val typeDesc = resourceSubtitles[selectedType] ?: "管理并向客户端下发集控档案资源"

        root.addView(UiKit.pageHeader(ctx, typeTitle, typeDesc) {
            addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "新建文件", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                showCreateBar = !showCreateBar
                act.renderUI()
            })
            if (selectedResourceName.isNotEmpty()) {
                addView(UiKit.iconTextButton(ctx, MdsIcon.HISTORY, "历史版本", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    if (showHistoryPanel) {
                        showHistoryPanel = false
                        act.renderUI()
                    } else {
                        loadFileHistory()
                    }
                })
            }
            addView(UiKit.iconTextButton(ctx, MdsIcon.REFRESH, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadResourcesList()
            })
        })

        if (showCreateBar) {
            val createCard = UiKit.card(ctx, 12)
            createCard.addView(UiKit.label(ctx, "新建 $typeTitle 资源文件", 13f, UiKit.TEXT_PRIMARY, true))
            val row = UiKit.hBox(ctx)
            val newNameEdit = UiKit.input(ctx, "输入新配置名称 (如 default / 理科班)", newFileName).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 8)
                }
            }
            row.addView(newNameEdit)
            row.addView(UiKit.button(ctx, "创建", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                createAndSaveNewFile(newNameEdit.text.toString())
            })
            row.addView(UiKit.button(ctx, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                showCreateBar = false
                act.renderUI()
            })
            createCard.addView(row)
            root.addView(createCard)
        }

        if (showHistoryPanel && selectedResourceName.isNotEmpty()) {
            root.addView(buildHistoryPanelCard(ctx))
        }

        if (isTablet) {
            val split = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val leftCol = UiKit.vBox(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 220), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(ctx, 14)
                }
            }
            val rightCol = UiKit.vBox(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            leftCol.addView(buildResourceListCard(ctx))
            rightCol.addView(buildResourceEditorCard(ctx))
            split.addView(leftCol)
            split.addView(rightCol)
            root.addView(split)
        } else {
            root.addView(buildResourceListCard(ctx))
            root.addView(buildResourceEditorCard(ctx))
        }
        return root
    }

    private fun buildHistoryPanelCard(ctx: Context): LinearLayout {
        val card = UiKit.card(ctx, 12)
        val hRow = UiKit.hBox(ctx)
        hRow.addView(UiKit.label(ctx, "文件历史版本 · $selectedResourceName", 13f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        hRow.addView(UiKit.button(ctx, "关闭历史", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
            showHistoryPanel = false
            act.renderUI()
        })
        card.addView(hRow)
        card.addView(UiKit.spacer(ctx, 6))

        if (historyItems.length() == 0) {
            card.addView(UiKit.label(ctx, "暂无历史版本记录", 12f, UiKit.TEXT_TERTIARY))
        } else {
            for (i in 0 until historyItems.length()) {
                val item = historyItems.optJSONObject(i) ?: continue
                val ver = item.optInt("version", 0)
                val updatedAt = item.optLong("updated_at", 0L)
                val sizeBytes = item.optInt("size_bytes", 0)
                val isCurrent = item.optBoolean("is_current", false)

                val r = UiKit.subCard(ctx, 8)
                val line = UiKit.hBox(ctx)
                line.addView(UiKit.badge(ctx, "v$ver", if (isCurrent) "success" else "info"))
                line.addView(UiKit.label(
                    ctx,
                    "  ${formatTs(updatedAt)} · ${sizeBytes}B" + if (isCurrent) " (当前版本)" else "",
                    12f,
                    UiKit.TEXT_PRIMARY,
                    isCurrent
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                line.addView(UiKit.button(ctx, "载入此版本", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
                    loadHistoryVersionContent(ver)
                })
                r.addView(line)
                card.addView(r)
            }
        }
        return card
    }

    private fun buildResourceListCard(ctx: Context): LinearLayout {
        val card = UiKit.card(ctx, 12)
        val headerRow = UiKit.hBox(ctx)
        headerRow.addView(UiKit.label(ctx, "档案文件列表", 13f, UiKit.TEXT_SECONDARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(UiKit.badge(ctx, "${resourceFiles.size}", "neutral"))
        card.addView(headerRow)
        card.addView(UiKit.spacer(ctx, 8))

        if (resourceFiles.isEmpty()) {
            card.addView(UiKit.label(ctx, "暂无该类型资源，点击上方「新建文件」创建", 12f, UiKit.TEXT_TERTIARY).apply {
                setPadding(UiKit.dp(ctx, 4), UiKit.dp(ctx, 12), UiKit.dp(ctx, 4), UiKit.dp(ctx, 12))
            })
        } else {
            for (name in resourceFiles) {
                val active = (name == selectedResourceName)
                val itemRow = UiKit.hBox(ctx, 10).apply {
                    background = UiKit.roundedBg(
                        if (active) UiKit.BG_ACCENT_SUBTLE else Color.TRANSPARENT,
                        6,
                        ctx,
                        if (active) UiKit.ACCENT_BLUE else Color.TRANSPARENT,
                        if (active) 1 else 0
                    )
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = UiKit.dp(ctx, 4)
                    }
                    setOnClickListener {
                        selectFileByName(name)
                    }
                }
                itemRow.addView(UiKit.label(
                    ctx,
                    name,
                    13f,
                    if (active) UiKit.ACCENT_BLUE else UiKit.TEXT_PRIMARY,
                    active
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (active) {
                    itemRow.addView(UiKit.badge(ctx, "当前", "info"))
                }
                card.addView(itemRow)
            }
        }
        return card
    }

    private fun buildResourceEditorCard(ctx: Context): LinearLayout {
        val card = UiKit.card(ctx, 16)
        if (selectedResourceName.isEmpty() && resourceFiles.isEmpty()) {
            card.addView(UiKit.label(ctx, "请先在左侧选择或点击「新建文件」创建一个档案资源文件", 13f, UiKit.TEXT_SECONDARY))
            return card
        }

        val bar = UiKit.hBox(ctx)
        val titleCol = UiKit.vBox(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(UiKit.label(
                ctx,
                selectedResourceName.ifEmpty { "未命名资源" } +
                    (if (previewHistoryVersion > 0) " (预览历史版本 v$previewHistoryVersion)" else ""),
                15f,
                UiKit.TEXT_PRIMARY,
                true
            ))
            addView(UiKit.label(ctx, "资源类型: $selectedType", 11f, UiKit.TEXT_SECONDARY))
        }
        bar.addView(titleCol)

        bar.addView(
            UiKit.button(
                ctx,
                if (editMode == "visual") "JSON 源码" else "可视化编辑",
                UiKit.BTN_MUTED,
                UiKit.TEXT_PRIMARY,
                true
            ) {
                editMode = if (editMode == "visual") "raw" else "visual"
                act.renderUI()
            }
        )
        bar.addView(UiKit.button(ctx, "保存", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            saveCurrentResource(selectedResourceName)
        })
        if (selectedResourceName.isNotEmpty()) {
            bar.addView(UiKit.button(ctx, "删除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                deleteCurrentResource()
            })
        }
        card.addView(bar)
        card.addView(UiKit.divider(ctx, 12))

        if (editMode == "raw") {
            card.addView(UiKit.label(ctx, "JSON 源码编辑器", 12f, UiKit.TEXT_SECONDARY, true))
            val rawInput = UiKit.input(ctx, "{ ... }", rawJsonContent, isPassword = false, multiLine = true).apply {
                minLines = 14
                typeface = Typeface.MONOSPACE
            }
            card.addView(rawInput)
            val rawBtnRow = UiKit.hBox(ctx)
            rawBtnRow.addView(UiKit.button(ctx, "格式化并校验", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                try {
                    rawJsonContent = JSONObject(rawInput.text.toString()).toString(2)
                    act.showBanner("JSON 语法校验通过", false)
                    act.renderUI()
                } catch (e: Exception) {
                    act.showBanner("JSON 格式有误: ${e.message}", true)
                }
            })
            rawBtnRow.addView(UiKit.button(ctx, "保存并发布", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                rawJsonContent = rawInput.text.toString()
                saveCurrentResource(selectedResourceName)
            })
            card.addView(rawBtnRow)
            return card
        }

        val json = parseCurrentJson()
        when (selectedType) {
            "Subjects" -> buildSubjectsVisual(ctx, card, json)
            "TimeLayout" -> buildTimeLayoutVisual(ctx, card, json)
            "ClassPlan" -> buildClassPlanVisual(ctx, card, json)
            "Policy" -> buildPolicyVisual(ctx, card, json)
            "DefaultSettings" -> buildDefaultSettingsVisual(ctx, card, json)
            "Components" -> buildComponentsVisual(ctx, card, json)
            "Credentials" -> buildCredentialsVisual(ctx, card, json)
        }
        return card
    }
}
