package org.miniopensource.miniclassisland

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ==================== 1. SUBJECTS EDITOR ====================
internal fun ProfileEditorComponent.buildSubjectsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    val subjectsObj = root.optJSONObject("Subjects") ?: JSONObject().also { root.put("Subjects", it) }

    val topBar = UiKit.hBox(ctx)
    topBar.addView(UiKit.label(ctx, "共 ${subjectsObj.length()} 门科目", 13f, UiKit.TEXT_SECONDARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    topBar.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "添加科目", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val id = UUID.randomUUID().toString()
        subjectsObj.put(id, JSONObject().apply {
            put("Name", "新科目")
            put("Initial", "新")
            put("TeacherName", "")
            put("IsOutDoor", false)
        })
        commitJsonAndRefresh(root)
    })
    container.addView(topBar)
    container.addView(UiKit.spacer(ctx, 8))

    val keys = subjectsObj.keys().asSequence().toList()
    keys.forEachIndexed { idx, key ->
        val item = subjectsObj.optJSONObject(key) ?: return@forEachIndexed
        val box = UiKit.subCard(ctx, 12)

        val r1 = UiKit.hBox(ctx)
        val curName = item.optString("Name", "")
        val curInit = item.optString("Initial", "").ifEmpty { curName.take(1).ifEmpty { "课" } }

        val initBadge = TextView(ctx).apply {
            text = curInit.take(2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(UiKit.ACCENT_BLUE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = UiKit.roundedBg(UiKit.BG_ACCENT_SUBTLE, 8, ctx)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 36), UiKit.dp(ctx, 36)).apply {
                rightMargin = UiKit.dp(ctx, 10)
            }
        }
        r1.addView(initBadge)

        val nameEdit = UiKit.input(ctx, "科目名称", curName).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        val initEdit = UiKit.input(ctx, "简称", item.optString("Initial")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        val teacherEdit = UiKit.input(ctx, "任课教师", item.optString("TeacherName")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        r1.addView(nameEdit)
        r1.addView(initEdit)
        r1.addView(teacherEdit)
        box.addView(r1)

        val r2 = UiKit.hBox(ctx)
        val outdoorCb = CheckBox(ctx).apply {
            text = "户外课程"
            setTextColor(UiKit.TEXT_SECONDARY)
            textSize = 12f
            isChecked = item.optBoolean("IsOutDoor", false)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        r2.addView(outdoorCb)

        if (idx > 0) {
            r2.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_UP, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                reorderJsonObjectKeys(subjectsObj, keys, idx, idx - 1)
                commitJsonAndRefresh(root)
            })
        }
        if (idx < keys.size - 1) {
            r2.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_DOWN, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                reorderJsonObjectKeys(subjectsObj, keys, idx, idx + 1)
                commitJsonAndRefresh(root)
            })
        }
        r2.addView(UiKit.button(ctx, "应用修改", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            val nVal = nameEdit.text.toString().trim()
            val iVal = initEdit.text.toString().trim().ifEmpty { nVal.take(1) }
            item.put("Name", nVal)
            item.put("Initial", iVal)
            item.put("TeacherName", teacherEdit.text.toString().trim())
            item.put("IsOutDoor", outdoorCb.isChecked)
            commitJsonAndRefresh(root)
        })
        r2.addView(UiKit.iconButton(ctx, MdsIcon.DELETE, UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG) {
            subjectsObj.remove(key)
            commitJsonAndRefresh(root)
        })
        box.addView(r2)
        container.addView(box)
    }
}

private fun reorderJsonObjectKeys(obj: JSONObject, keys: List<String>, fromIdx: Int, toIdx: Int) {
    val mut = keys.toMutableList()
    val moved = mut.removeAt(fromIdx)
    mut.add(toIdx, moved)
    val pairs = mut.map { k -> k to obj.optJSONObject(k) }
    for (k in keys) obj.remove(k)
    for ((k, v) in pairs) {
        if (v != null) obj.put(k, v)
    }
}

// ==================== 2. TIMELAYOUT EDITOR ====================
internal fun ProfileEditorComponent.buildTimeLayoutVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    if (subjectRefFiles.isNotEmpty()) {
        val refBox = UiKit.subCard(ctx, 10)
        refBox.addView(UiKit.label(ctx, "参考科目列表文件 (用于默认科目选择):", 12f, UiKit.TEXT_SECONDARY, true))
        val sOpts = subjectRefFiles.map { it to it }
        refBox.addView(UiKit.chipRow(ctx, sOpts, selectedSubjectRefFile) { f ->
            selectedSubjectRefFile = f
            reloadReferencesOnly()
        })
        container.addView(refBox)
    }

    val layoutsObj = root.optJSONObject("TimeLayouts") ?: JSONObject().also { root.put("TimeLayouts", it) }
    val keys = layoutsObj.keys().asSequence().toList()
    if (activeGroupKey.isEmpty() || !layoutsObj.has(activeGroupKey)) {
        activeGroupKey = keys.firstOrNull().orEmpty()
    }

    val groupRow = UiKit.hBox(ctx)
    groupRow.addView(UiKit.label(ctx, "作息时间表列表 (${keys.size}):", 12f, UiKit.TEXT_SECONDARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    groupRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "新增时间表", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val id = UUID.randomUUID().toString()
        layoutsObj.put(id, JSONObject().apply {
            put("Name", "新时间表")
            put("Layouts", JSONArray())
        })
        activeGroupKey = id
        commitJsonAndRefresh(root)
    })
    container.addView(groupRow)

    if (keys.isNotEmpty()) {
        val opts = keys.map { k ->
            k to (layoutsObj.optJSONObject(k)?.optString("Name").orEmpty().ifBlank { k.take(8) })
        }
        container.addView(UiKit.chipRow(ctx, opts, activeGroupKey) { k ->
            activeGroupKey = k
            act.renderUI()
        })
    }

    val activeGroup = layoutsObj.optJSONObject(activeGroupKey) ?: return
    val pts = activeGroup.optJSONArray("Layouts") ?: JSONArray().also { activeGroup.put("Layouts", it) }

    val gNameRow = UiKit.hBox(ctx)
    val gNameInput = UiKit.input(ctx, "时间表名称", activeGroup.optString("Name")).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(ctx, 6)
        }
    }
    gNameRow.addView(gNameInput)
    gNameRow.addView(UiKit.button(ctx, "保存名称", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        activeGroup.put("Name", gNameInput.text.toString().trim())
        commitJsonAndRefresh(root)
    })
    gNameRow.addView(UiKit.button(ctx, "按时间排序", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
        val list = mutableListOf<JSONObject>()
        for (i in 0 until pts.length()) {
            pts.optJSONObject(i)?.let { list.add(it) }
        }
        list.sortBy { extractTimePart(it.optString("StartSecond", it.optString("StartTime", "08:00:00"))) }
        val sortedArr = JSONArray()
        list.forEach { sortedArr.put(it) }
        activeGroup.put("Layouts", sortedArr)
        commitJsonAndRefresh(root)
    })
    gNameRow.addView(UiKit.button(ctx, "删除时间表", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
        layoutsObj.remove(activeGroupKey)
        activeGroupKey = ""
        commitJsonAndRefresh(root)
    })
    container.addView(gNameRow)

    val lastEndTime = if (pts.length() > 0) {
        val lastPt = pts.optJSONObject(pts.length() - 1)
        extractTimePart(lastPt?.optString("EndSecond", lastPt.optString("EndTime", "08:00:00")).orEmpty())
    } else {
        "08:00:00"
    }

    val addSlotRow = UiKit.hBox(ctx)
    addSlotRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "上课", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val endT = addMinutesToTimePart(lastEndTime, 40)
        pts.put(JSONObject().apply {
            put("StartSecond", lastEndTime)
            put("EndSecond", endT)
            put("TimeType", 0)
            put("DefaultClassId", "")
            put("IsHideDefault", false)
        })
        commitJsonAndRefresh(root)
    })
    addSlotRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "课间休息", UiKit.ACCENT_GREEN, Color.WHITE, true) {
        val endT = addMinutesToTimePart(lastEndTime, 10)
        pts.put(JSONObject().apply {
            put("StartSecond", lastEndTime)
            put("EndSecond", endT)
            put("TimeType", 1)
            put("BreakName", "课间休息")
            put("IsHideDefault", false)
        })
        commitJsonAndRefresh(root)
    })
    addSlotRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "分割线", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        pts.put(JSONObject().apply {
            put("StartSecond", lastEndTime)
            put("EndSecond", lastEndTime)
            put("TimeType", 2)
        })
        commitJsonAndRefresh(root)
    })
    addSlotRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "行动", UiKit.BADGE_WARNING_BG, UiKit.BADGE_WARNING_FG, true) {
        pts.put(JSONObject().apply {
            put("StartSecond", lastEndTime)
            put("EndSecond", lastEndTime)
            put("TimeType", 3)
            put("ActionSet", JSONObject().apply {
                put("Name", "自动行动")
                put("Guid", UUID.randomUUID().toString())
                put("IsEnabled", true)
                put("IsRevertEnabled", false)
                put("Actions", JSONArray())
            })
        })
        commitJsonAndRefresh(root)
    })
    container.addView(addSlotRow)

    val subjChipOpts = listOf("" to "无默认科目") + refSubjectsMap.entries.map { it.key to it.value }

    for (i in 0 until pts.length()) {
        val pt = pts.optJSONObject(i) ?: continue
        val tType = pt.optInt("TimeType", 0)
        val (typeTitle, badgeKind) = when (tType) {
            0 -> "上课" to "info"
            1 -> "课间休息" to "success"
            2 -> "分割线" to "warning"
            else -> "行动" to "neutral"
        }
        val startVal = extractTimePart(pt.optString("StartSecond", pt.optString("StartTime", "08:00:00")))
        val endVal = extractTimePart(pt.optString("EndSecond", pt.optString("EndTime", startVal)))

        val box = UiKit.subCard(ctx, 10)
        val header = UiKit.hBox(ctx)
        header.addView(UiKit.badge(ctx, "#${i + 1} $typeTitle", badgeKind))
        header.addView(UiKit.label(
            ctx,
            if (tType == 2 || tType == 3) "  $startVal" else "  $startVal - $endVal",
            12f,
            UiKit.TEXT_PRIMARY,
            true
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (i > 0) {
            header.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_UP, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                val prev = pts.optJSONObject(i - 1)
                pts.put(i - 1, pt)
                pts.put(i, prev)
                commitJsonAndRefresh(root)
            })
        }
        if (i < pts.length() - 1) {
            header.addView(UiKit.iconButton(ctx, MdsIcon.ARROW_DOWN, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY) {
                val next = pts.optJSONObject(i + 1)
                pts.put(i + 1, pt)
                pts.put(i, next)
                commitJsonAndRefresh(root)
            })
        }
        header.addView(UiKit.iconButton(ctx, MdsIcon.DELETE, UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG) {
            pts.remove(i)
            commitJsonAndRefresh(root)
        })
        box.addView(header)

        val timeRow = UiKit.hBox(ctx)
        val startEdit = UiKit.input(ctx, "开始时间 HH:mm:ss", startVal).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        timeRow.addView(startEdit)

        val endEdit = if (tType == 0 || tType == 1) {
            UiKit.input(ctx, "结束时间 HH:mm:ss", endVal).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 6)
                }
            }.also { timeRow.addView(it) }
        } else null

        val breakEdit = if (tType == 1) {
            UiKit.input(ctx, "课间名称", pt.optString("BreakName", "课间休息")).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 6)
                }
            }.also { timeRow.addView(it) }
        } else null

        timeRow.addView(UiKit.button(ctx, "更新时间", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            val sClean = extractTimePart(startEdit.text.toString())
            val eClean = if (endEdit != null) extractTimePart(endEdit.text.toString()) else sClean
            pt.put("StartSecond", sClean)
            pt.put("EndSecond", eClean)
            if (breakEdit != null) {
                pt.put("BreakName", breakEdit.text.toString().trim())
            }
            commitJsonAndRefresh(root)
        })
        box.addView(timeRow)

        if (tType == 0) {
            box.addView(UiKit.label(ctx, "默认关联科目:", 11f, UiKit.TEXT_SECONDARY))
            box.addView(UiKit.chipRow(ctx, subjChipOpts, pt.optString("DefaultClassId")) { defSubj ->
                pt.put("DefaultClassId", defSubj)
                commitJsonAndRefresh(root)
            })
        }
        if (tType == 0 || tType == 1) {
            box.addView(UiKit.switchRow(ctx, "默认隐藏该时间点", "不在常规主界面显示此时间段", pt.optBoolean("IsHideDefault", false)) { v ->
                pt.put("IsHideDefault", v)
                commitJsonAndRefresh(root)
            })
        }
        if (tType == 3) {
            buildTimePointActionSetEditor(ctx, box, root, pt)
        }
        container.addView(box)
    }
}

private fun ProfileEditorComponent.buildTimePointActionSetEditor(
    ctx: Context,
    box: LinearLayout,
    root: JSONObject,
    pt: JSONObject
) {
    val actionSet = pt.optJSONObject("ActionSet") ?: JSONObject().apply {
        put("Name", "自动行动")
        put("Guid", UUID.randomUUID().toString())
        put("IsEnabled", true)
        put("IsRevertEnabled", false)
        put("Actions", JSONArray())
    }.also { pt.put("ActionSet", it) }

    box.addView(UiKit.divider(ctx, 6))
    box.addView(UiKit.label(ctx, "行动组配置", 12f, UiKit.ACCENT_BLUE, true))

    val setRow = UiKit.hBox(ctx)
    val setNameEdit = UiKit.input(ctx, "行动组名称", actionSet.optString("Name", "自动行动")).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = UiKit.dp(ctx, 6)
        }
    }
    setRow.addView(setNameEdit)
    setRow.addView(UiKit.button(ctx, "保存名称", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
        actionSet.put("Name", setNameEdit.text.toString().trim())
        commitJsonAndRefresh(root)
    })
    box.addView(setRow)

    box.addView(UiKit.switchRow(ctx, "启用此行动时间点", "", actionSet.optBoolean("IsEnabled", true)) { v ->
        actionSet.put("IsEnabled", v)
        commitJsonAndRefresh(root)
    })
    box.addView(UiKit.switchRow(ctx, "允许恢复操作", "在行动结束时还原设置变更", actionSet.optBoolean("IsRevertEnabled", false)) { v ->
        actionSet.put("IsRevertEnabled", v)
        commitJsonAndRefresh(root)
    })

    val actionsArr = actionSet.optJSONArray("Actions") ?: JSONArray().also { actionSet.put("Actions", it) }
    box.addView(UiKit.label(ctx, "添加内置行动项:", 11f, UiKit.TEXT_SECONDARY))
    val addActOpts = BUILTIN_ACTIONS.map { it.id to it.name }
    box.addView(UiKit.chipRow(ctx, addActOpts, "") { actId ->
        val def = BUILTIN_ACTIONS.find { it.id == actId }
        val settingsObj = try {
            JSONObject(def?.defaultSettingsJson ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        actionsArr.put(JSONObject().apply {
            put("Id", actId)
            put("Settings", settingsObj)
        })
        commitJsonAndRefresh(root)
    })

    for (aIdx in 0 until actionsArr.length()) {
        val actItem = actionsArr.optJSONObject(aIdx) ?: continue
        val actId = actItem.optString("Id")
        val def = BUILTIN_ACTIONS.find { it.id == actId }
        val settingsObj = when (val rawS = actItem.opt("Settings")) {
            is JSONObject -> rawS
            is String -> try { JSONObject(rawS) } catch (_: Exception) { JSONObject() }
            else -> JSONObject()
        }.also { actItem.put("Settings", it) }

        val actCard = UiKit.card(ctx, 8)
        val ah = UiKit.hBox(ctx)
        ah.addView(UiKit.badge(ctx, def?.category ?: "自定义", "info"))
        ah.addView(UiKit.label(ctx, "  ${def?.name ?: actId}", 12f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        ah.addView(UiKit.button(ctx, "移除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            actionsArr.remove(aIdx)
            commitJsonAndRefresh(root)
        })
        actCard.addView(ah)

        when (actId) {
            "classisland.os.run" -> {
                val runTypes = listOf("0" to "应用程序", "1" to "命令", "2" to "文件", "3" to "文件夹", "4" to "网页链接")
                actCard.addView(UiKit.chipRow(ctx, runTypes, settingsObj.optInt("RunType", 0).toString()) { rt ->
                    settingsObj.put("RunType", rt.toIntOrNull() ?: 0)
                    commitJsonAndRefresh(root)
                })
                val valEdit = UiKit.input(ctx, "路径 / 命令 / URL", settingsObj.optString("Value"))
                val argsEdit = UiKit.input(ctx, "启动参数 (可选)", settingsObj.optString("Args"))
                actCard.addView(valEdit)
                actCard.addView(argsEdit)
                actCard.addView(UiKit.button(ctx, "保存运行参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    settingsObj.put("Value", valEdit.text.toString())
                    settingsObj.put("Args", argsEdit.text.toString())
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.showNotification" -> {
                val maskEdit = UiKit.input(ctx, "提醒遮罩标题", settingsObj.optString("Mask", "提醒"))
                val contEdit = UiKit.input(ctx, "提醒正文内容", settingsObj.optString("Content"))
                val durEdit = UiKit.input(ctx, "正文持续秒数", settingsObj.optInt("ContentDurationSeconds", 10).toString())
                actCard.addView(maskEdit)
                actCard.addView(contEdit)
                actCard.addView(durEdit)
                actCard.addView(UiKit.switchRow(ctx, "朗读遮罩与正文", "", settingsObj.optBoolean("IsContentSpeechEnabled", true)) { v ->
                    settingsObj.put("IsMaskSpeechEnabled", v)
                    settingsObj.put("IsContentSpeechEnabled", v)
                    commitJsonAndRefresh(root)
                })
                actCard.addView(UiKit.button(ctx, "保存提醒参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    settingsObj.put("Mask", maskEdit.text.toString())
                    settingsObj.put("Content", contEdit.text.toString())
                    settingsObj.put("ContentDurationSeconds", durEdit.text.toString().toIntOrNull() ?: 10)
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.notification.weather" -> {
                val kinds = listOf("0" to "今日天气预报", "1" to "气象灾害预警", "2" to "逐小时天气预报")
                actCard.addView(UiKit.chipRow(ctx, kinds, settingsObj.optInt("NotificationKind", 0).toString()) { k ->
                    settingsObj.put("NotificationKind", k.toIntOrNull() ?: 0)
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.broadcastSignal" -> {
                val sigEdit = UiKit.input(ctx, "信号名称", settingsObj.optString("SignalName"))
                actCard.addView(sigEdit)
                actCard.addView(UiKit.switchRow(ctx, "触发恢复信号", "", settingsObj.optBoolean("IsRevert", false)) { v ->
                    settingsObj.put("IsRevert", v)
                    commitJsonAndRefresh(root)
                })
                actCard.addView(UiKit.button(ctx, "保存信号参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    settingsObj.put("SignalName", sigEdit.text.toString())
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.action.sleep" -> {
                val secEdit = UiKit.input(ctx, "等待秒数", settingsObj.optDouble("Value", 5.0).toString())
                actCard.addView(secEdit)
                actCard.addView(UiKit.button(ctx, "保存等待时长", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    settingsObj.put("Value", secEdit.text.toString().toDoubleOrNull() ?: 5.0)
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.settings" -> {
                val nEdit = UiKit.input(ctx, "设置属性名", settingsObj.optString("Name", "Scale"))
                val vEdit = UiKit.input(ctx, "目标属性值", settingsObj.optString("Value", "1.0"))
                actCard.addView(nEdit)
                actCard.addView(vEdit)
                actCard.addView(UiKit.button(ctx, "保存设置变更项", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                    settingsObj.put("Name", nEdit.text.toString().trim())
                    settingsObj.put("Value", vEdit.text.toString())
                    commitJsonAndRefresh(root)
                })
            }
            "classisland.app.restart" -> {
                actCard.addView(UiKit.switchRow(ctx, "静默重启模式", "", settingsObj.optBoolean("Value", false)) { v ->
                    settingsObj.put("Value", v)
                    commitJsonAndRefresh(root)
                })
            }
        }
        box.addView(actCard)
    }
}

// ==================== 3. CLASSPLAN EDITOR ====================
internal fun ProfileEditorComponent.buildClassPlanVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
    val refCard = UiKit.subCard(ctx, 10)
    refCard.addView(UiKit.label(ctx, "关联参考文件选择:", 12f, UiKit.TEXT_SECONDARY, true))
    if (timeLayoutRefFiles.isNotEmpty()) {
        refCard.addView(UiKit.label(ctx, "时间表档案:", 11f, UiKit.TEXT_TERTIARY))
        refCard.addView(UiKit.chipRow(ctx, timeLayoutRefFiles.map { it to it }, selectedTimeLayoutRefFile) { f ->
            selectedTimeLayoutRefFile = f
            reloadReferencesOnly()
        })
    }
    if (subjectRefFiles.isNotEmpty()) {
        refCard.addView(UiKit.label(ctx, "科目档案:", 11f, UiKit.TEXT_TERTIARY))
        refCard.addView(UiKit.chipRow(ctx, subjectRefFiles.map { it to it }, selectedSubjectRefFile) { f ->
            selectedSubjectRefFile = f
            reloadReferencesOnly()
        })
    }
    container.addView(refCard)

    val groupsObj = root.optJSONObject("ClassPlanGroups") ?: JSONObject().apply {
        put(EMPTY_GUID, JSONObject().put("Name", "默认").put("IsGlobal", false))
    }.also { root.put("ClassPlanGroups", it) }
    if (!groupsObj.has(EMPTY_GUID)) {
        groupsObj.put(EMPTY_GUID, JSONObject().put("Name", "默认").put("IsGlobal", false))
    }

    val groupBox = UiKit.subCard(ctx, 10)
    val gh = UiKit.hBox(ctx)
    gh.addView(UiKit.label(ctx, "课表群组筛选:", 12f, UiKit.TEXT_SECONDARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    val newGrpEdit = UiKit.input(ctx, "新群组名称", newClassPlanGroupName).apply {
        layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 130), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            rightMargin = UiKit.dp(ctx, 6)
        }
    }
    gh.addView(newGrpEdit)
    gh.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "群组", UiKit.BTN_MUTED, UiKit.ACCENT_BLUE, true) {
        val gn = newGrpEdit.text.toString().trim()
        if (gn.isNotEmpty()) {
            val gid = UUID.randomUUID().toString()
            groupsObj.put(gid, JSONObject().put("Name", gn).put("IsGlobal", false))
            newClassPlanGroupName = ""
            selectedClassPlanGroupFilter = gid
            commitJsonAndRefresh(root)
        }
    })
    groupBox.addView(gh)

    val groupKeys = groupsObj.keys().asSequence().toList()
    val groupFilterOpts = listOf("all" to "全部群组") + groupKeys.map { gk ->
        gk to (groupsObj.optJSONObject(gk)?.optString("Name").orEmpty().ifBlank { "默认" })
    }
    groupBox.addView(UiKit.chipRow(ctx, groupFilterOpts, selectedClassPlanGroupFilter) { gf ->
        selectedClassPlanGroupFilter = gf
        act.renderUI()
    })
    container.addView(groupBox)

    val plansObj = root.optJSONObject("ClassPlans") ?: JSONObject().also { root.put("ClassPlans", it) }
    val allPlanKeys = plansObj.keys().asSequence().toList()
    val filteredPlanKeys = allPlanKeys.filter { pk ->
        if (selectedClassPlanGroupFilter == "all") true
        else {
            val ag = toUpstreamGuidOrEmpty(plansObj.optJSONObject(pk)?.optString("AssociatedGroup"))
            ag == selectedClassPlanGroupFilter
        }
    }

    if (activeGroupKey.isEmpty() || !plansObj.has(activeGroupKey)) {
        activeGroupKey = filteredPlanKeys.firstOrNull() ?: allPlanKeys.firstOrNull().orEmpty()
    }

    val planActionRow = UiKit.hBox(ctx)
    planActionRow.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "添加课表", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        val id = UUID.randomUUID().toString()
        val defTlId = refTimeLayoutsList.firstOrNull()?.first.orEmpty()
        plansObj.put(id, JSONObject().apply {
            put("Name", "新课表")
            put("TimeLayoutId", defTlId)
            put("AssociatedGroup", if (selectedClassPlanGroupFilter == "all") EMPTY_GUID else selectedClassPlanGroupFilter)
            put("IsEnabled", true)
            put("IsOverlay", false)
            put("TimeRule", JSONObject().put("WeekDay", 1).put("WeekCountDiv", 0).put("WeekCountDivTotal", 2))
            put("Classes", buildAlignedClassesArray(defTlId, null))
        })
        activeGroupKey = id
        selectedSlotIndex = 0
        commitJsonAndRefresh(root)
    })

    val currentPlan = plansObj.optJSONObject(activeGroupKey)
    if (currentPlan != null) {
        planActionRow.addView(UiKit.button(ctx, "复制课表", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            val id = UUID.randomUUID().toString()
            val copy = JSONObject(currentPlan.toString()).apply {
                put("Name", "${currentPlan.optString("Name")} - 副本")
            }
            plansObj.put(id, copy)
            activeGroupKey = id
            commitJsonAndRefresh(root)
        })
        if (!currentPlan.optBoolean("IsOverlay", false)) {
            planActionRow.addView(UiKit.button(ctx, "创建临时层", UiKit.BADGE_WARNING_BG, UiKit.BADGE_WARNING_FG, true) {
                val id = UUID.randomUUID().toString()
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val overlay = JSONObject(currentPlan.toString()).apply {
                    put("Name", "${currentPlan.optString("Name")} (临时层)")
                    put("IsOverlay", true)
                    put("OverlaySourceId", activeGroupKey)
                    put("OverlaySetupTime", nowIso)
                }
                plansObj.put(id, overlay)
                activeGroupKey = id
                commitJsonAndRefresh(root)
            })
        }
        planActionRow.addView(UiKit.button(ctx, "删除课表", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            plansObj.remove(activeGroupKey)
            activeGroupKey = ""
            commitJsonAndRefresh(root)
        })
    }
    container.addView(planActionRow)

    if (filteredPlanKeys.isNotEmpty()) {
        val weekLabels = mapOf(0 to "周日", 1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六")
        val planChipOpts = filteredPlanKeys.map { pk ->
            val pObj = plansObj.optJSONObject(pk)
            val pName = pObj?.optString("Name").orEmpty().ifBlank { pk.take(6) }
            val rule = pObj?.optJSONObject("TimeRule")
            val wd = weekLabels[rule?.optInt("WeekDay", 1) ?: 1] ?: "周一"
            val div = rule?.optInt("WeekCountDiv", 0) ?: 0
            val total = rule?.optInt("WeekCountDivTotal", 2) ?: 2
            val divTag = if (div > 0) " [$div/${total}周]" else ""
            val ovTag = if (pObj?.optBoolean("IsOverlay", false) == true) " [临时层]" else ""
            pk to "$pName ($wd$divTag$ovTag)"
        }
        container.addView(UiKit.chipRow(ctx, planChipOpts, activeGroupKey) { pk ->
            activeGroupKey = pk
            selectedSlotIndex = 0
            act.renderUI()
        })
    }

    val plan = plansObj.optJSONObject(activeGroupKey) ?: return
    val classesArr = plan.optJSONArray("Classes") ?: JSONArray().also { plan.put("Classes", it) }
    val tlId = plan.optString("TimeLayoutId")
    val tlSlots = refTimeLayoutSlotsMap[tlId]

    // Auto-align slot count with TimeLayout's TimeType==0 items if needed
    val classTimePoints = mutableListOf<JSONObject>()
    if (tlSlots != null) {
        for (i in 0 until tlSlots.length()) {
            val pt = tlSlots.optJSONObject(i) ?: continue
            if (pt.optInt("TimeType", 0) == 0) classTimePoints.add(pt)
        }
        while (classesArr.length() < classTimePoints.size) {
            val pt = classTimePoints[classesArr.length()]
            classesArr.put(JSONObject().apply {
                put("SubjectId", pt.optString("DefaultClassId", ""))
                put("IsEnabled", true)
            })
        }
    }

    if (selectedSlotIndex >= classesArr.length()) {
        selectedSlotIndex = (classesArr.length() - 1).coerceAtLeast(0)
    }

    // Bottom / Right Mode Tabs: 快速选课盘 vs 课表信息与触发规则
    val modeTabs = listOf("picker" to "快速选课与节次编排", "rules" to "课表信息与触发规则")
    container.addView(UiKit.chipRow(ctx, modeTabs, classPlanRightTab) { tab ->
        classPlanRightTab = tab
        act.renderUI()
    })

    if (classPlanRightTab == "rules") {
        val rule = plan.optJSONObject("TimeRule") ?: JSONObject().also { plan.put("TimeRule", it) }
        val rCard = UiKit.subCard(ctx, 12)
        rCard.addView(UiKit.label(ctx, "课表名称:", 12f, UiKit.TEXT_SECONDARY, true))
        val pNameEdit = UiKit.input(ctx, "课表名称", plan.optString("Name"))
        rCard.addView(pNameEdit)
        rCard.addView(UiKit.button(ctx, "更新课表名称", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            plan.put("Name", pNameEdit.text.toString().trim())
            commitJsonAndRefresh(root)
        })

        rCard.addView(UiKit.switchRow(ctx, "默认启用此课表", "按触发规则自动激活此课表", plan.optBoolean("IsEnabled", true)) { v ->
            plan.put("IsEnabled", v)
            commitJsonAndRefresh(root)
        })

        if (refTimeLayoutsList.isNotEmpty()) {
            rCard.addView(UiKit.label(ctx, "关联时间表:", 12f, UiKit.TEXT_SECONDARY, true))
            rCard.addView(UiKit.chipRow(ctx, refTimeLayoutsList, tlId) { newTlId ->
                plan.put("TimeLayoutId", newTlId)
                plan.put("Classes", buildAlignedClassesArray(newTlId, classesArr))
                commitJsonAndRefresh(root)
            })
        }

        val grpAssignOpts = groupKeys.map { gk ->
            gk to (groupsObj.optJSONObject(gk)?.optString("Name").orEmpty().ifBlank { "默认" })
        }
        rCard.addView(UiKit.label(ctx, "所属课表群组:", 12f, UiKit.TEXT_SECONDARY, true))
        rCard.addView(UiKit.chipRow(ctx, grpAssignOpts, toUpstreamGuidOrEmpty(plan.optString("AssociatedGroup"))) { gk ->
            plan.put("AssociatedGroup", gk)
            commitJsonAndRefresh(root)
        })

        rCard.addView(UiKit.label(ctx, "触发星期:", 12f, UiKit.TEXT_SECONDARY, true))
        val weekDays = listOf(
            "1" to "周一", "2" to "周二", "3" to "周三", "4" to "周四",
            "5" to "周五", "6" to "周六", "0" to "周日"
        )
        rCard.addView(UiKit.chipRow(ctx, weekDays, rule.optInt("WeekDay", 1).toString()) { w ->
            rule.put("WeekDay", w.toIntOrNull() ?: 1)
            commitJsonAndRefresh(root)
        })

        rCard.addView(UiKit.label(ctx, "多周轮换周期总周数:", 12f, UiKit.TEXT_SECONDARY, true))
        val totalOpts = listOf("2" to "双周轮换", "3" to "三周轮换", "4" to "四周轮换")
        val curTotal = rule.optInt("WeekCountDivTotal", 2).coerceIn(2, 4)
        rCard.addView(UiKit.chipRow(ctx, totalOpts, curTotal.toString()) { t ->
            rule.put("WeekCountDivTotal", t.toIntOrNull() ?: 2)
            commitJsonAndRefresh(root)
        })

        rCard.addView(UiKit.label(ctx, "生效周次:", 12f, UiKit.TEXT_SECONDARY, true))
        val divOpts = mutableListOf("0" to "不限 (每周生效)")
        for (wIdx in 1..curTotal) {
            divOpts.add("$wIdx" to "第 $wIdx 周 / 共 $curTotal 周")
        }
        rCard.addView(UiKit.chipRow(ctx, divOpts, rule.optInt("WeekCountDiv", 0).toString()) { d ->
            rule.put("WeekCountDiv", d.toIntOrNull() ?: 0)
            commitJsonAndRefresh(root)
        })
        container.addView(rCard)
        return
    }

    // Quick Subject Picker Card ("快速选课盘")
    val pickerCard = UiKit.subCard(ctx, 12)
    val pHeader = UiKit.hBox(ctx)
    pHeader.addView(UiKit.label(
        ctx,
        "快速选课盘 · 当前选中第 ${selectedSlotIndex + 1} 节",
        13f,
        UiKit.ACCENT_BLUE,
        true
    ).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    val autoNextCb = CheckBox(ctx).apply {
        text = "选完跳到下一节"
        textSize = 11f
        setTextColor(UiKit.TEXT_SECONDARY)
        isChecked = autoMoveNextSlot
        setOnCheckedChangeListener { _, checked -> autoMoveNextSlot = checked }
    }
    pHeader.addView(autoNextCb)
    pickerCard.addView(pHeader)

    val subjOpts = refSubjectsMap.entries.map { it.key to it.value }
    if (subjOpts.isEmpty()) {
        pickerCard.addView(UiKit.label(ctx, "当前参考科目文件为空，请先在「科目管理」中添加科目", 12f, UiKit.TEXT_TERTIARY))
    } else {
        val activeSlotObj = classesArr.optJSONObject(selectedSlotIndex)
        val activeSubjId = activeSlotObj?.optString("SubjectId").orEmpty()
        pickerCard.addView(UiKit.chipRow(ctx, subjOpts, activeSubjId) { chosenSubjId ->
            if (classesArr.length() == 0) {
                classesArr.put(JSONObject().put("SubjectId", chosenSubjId).put("IsEnabled", true))
            } else {
                val targetSlot = classesArr.optJSONObject(selectedSlotIndex) ?: JSONObject().also {
                    classesArr.put(selectedSlotIndex, it)
                }
                targetSlot.put("SubjectId", chosenSubjId)
            }
            if (autoMoveNextSlot && selectedSlotIndex < classesArr.length() - 1) {
                selectedSlotIndex++
            }
            commitJsonAndRefresh(root)
        })
    }
    container.addView(pickerCard)

    // Schedule Table Header
    val sHeader = UiKit.hBox(ctx)
    sHeader.addView(UiKit.label(ctx, "课程节次列表 (共 ${classesArr.length()} 节)", 13f, UiKit.TEXT_PRIMARY, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    })
    sHeader.addView(UiKit.iconTextButton(ctx, MdsIcon.ADD, "新增一节", UiKit.ACCENT_BLUE, Color.WHITE, true) {
        classesArr.put(JSONObject().put("SubjectId", "").put("IsEnabled", true))
        selectedSlotIndex = classesArr.length() - 1
        commitJsonAndRefresh(root)
    })
    container.addView(sHeader)

    for (i in 0 until classesArr.length()) {
        val cItem = classesArr.optJSONObject(i) ?: continue
        val curSubjId = cItem.optString("SubjectId")
        val curSubjName = refSubjectsMap[curSubjId] ?: curSubjId.takeIf { it.isNotBlank() } ?: "未排课"
        val isSel = (i == selectedSlotIndex)
        val timePt = classTimePoints.getOrNull(i)
        val timeRangeStr = if (timePt != null) {
            val s = extractTimePart(timePt.optString("StartSecond", timePt.optString("StartTime")))
            val e = extractTimePart(timePt.optString("EndSecond", timePt.optString("EndTime")))
            "$s - $e"
        } else {
            "手动节次"
        }

        val slotRow = UiKit.hBox(ctx, 10).apply {
            background = UiKit.roundedBg(
                if (isSel) UiKit.BG_ACCENT_SUBTLE else UiKit.BG_SUBTLE,
                8,
                ctx,
                if (isSel) UiKit.ACCENT_BLUE else UiKit.BORDER_COLOR,
                1
            )
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(ctx, 6)
            }
            setOnClickListener {
                selectedSlotIndex = i
                act.renderUI()
            }
        }
        slotRow.addView(UiKit.badge(ctx, "第 ${i + 1} 节", if (isSel) "info" else "neutral"))
        slotRow.addView(UiKit.label(ctx, "  $timeRangeStr", 11f, UiKit.TEXT_SECONDARY).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 115), ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        slotRow.addView(UiKit.label(ctx, curSubjName, 13f, if (isSel) UiKit.ACCENT_BLUE else UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (cItem.optBoolean("IsChangedClass", false)) {
            slotRow.addView(UiKit.badge(ctx, "换课", "warning"))
        }
        slotRow.addView(UiKit.iconButton(ctx, MdsIcon.SWAP, UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY) {
            cItem.put("IsChangedClass", !cItem.optBoolean("IsChangedClass", false))
            commitJsonAndRefresh(root)
        })
        slotRow.addView(UiKit.iconButton(ctx, MdsIcon.DELETE, UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG) {
            classesArr.remove(i)
            commitJsonAndRefresh(root)
        })
        container.addView(slotRow)
    }
}

private fun ProfileEditorComponent.buildAlignedClassesArray(tlId: String, existing: JSONArray?): JSONArray {
    val result = JSONArray()
    val tlSlots = refTimeLayoutSlotsMap[tlId]
    val classPts = mutableListOf<JSONObject>()
    if (tlSlots != null) {
        for (i in 0 until tlSlots.length()) {
            val pt = tlSlots.optJSONObject(i) ?: continue
            if (pt.optInt("TimeType", 0) == 0) classPts.add(pt)
        }
    }
    val targetCount = if (classPts.isNotEmpty()) classPts.size else (existing?.length()?.coerceAtLeast(1) ?: 1)
    for (i in 0 until targetCount) {
        val old = existing?.optJSONObject(i)
        val defSubj = classPts.getOrNull(i)?.optString("DefaultClassId", "").orEmpty()
        result.put(JSONObject().apply {
            put("SubjectId", old?.optString("SubjectId")?.takeIf { it.isNotEmpty() } ?: defSubj)
            put("IsEnabled", old?.optBoolean("IsEnabled", true) ?: true)
            if (old?.optBoolean("IsChangedClass", false) == true) {
                put("IsChangedClass", true)
            }
        })
    }
    return result
}
