package org.miniopensource.miniclassisland

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object UiKit {
    var themeMode: String = "light" // "system" | "light" | "dark"
    var isSystemDark: Boolean = false

    val isDark: Boolean
        get() = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemDark
        }

    // Exact Web CSS Tokens (web/app/globals.css & admin/app/globals.css)
    val BG_ROOT: Int get() = if (isDark) 0xFF121212.toInt() else 0xFFF5F5F5.toInt()
    val BG_SIDEBAR: Int get() = if (isDark) 0xFF181818.toInt() else 0xFFFAFAFA.toInt()
    val BG_HEADER: Int get() = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
    val BG_CARD: Int get() = if (isDark) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
    val BG_SUBTLE: Int get() = if (isDark) 0xFF252525.toInt() else 0xFFFAFAFA.toInt()
    val BG_INPUT: Int get() = if (isDark) 0xFF252525.toInt() else 0xFFFFFFFF.toInt()
    val BG_ACCENT_SUBTLE: Int get() = if (isDark) 0xFF15293E.toInt() else 0xFFE1EFFF.toInt()

    val TEXT_PRIMARY: Int get() = if (isDark) 0xFFF0F0F0.toInt() else 0xFF242424.toInt()
    val TEXT_SECONDARY: Int get() = if (isDark) 0xFFA6A6A6.toInt() else 0xFF616161.toInt()
    val TEXT_TERTIARY: Int get() = if (isDark) 0xFF6E6E6E.toInt() else 0xFF999999.toInt()

    val BORDER_COLOR: Int get() = if (isDark) 0xFF2E2E2E.toInt() else 0xFFE0E0E0.toInt()
    val BORDER_SUBTLE: Int get() = if (isDark) 0xFF262626.toInt() else 0xFFF0F0F0.toInt()

    val ACCENT_BLUE: Int get() = if (isDark) 0xFF4AA3FF.toInt() else 0xFF0078D4.toInt()
    val ACCENT_CYAN: Int get() = if (isDark) 0xFF4AA3FF.toInt() else 0xFF0078D4.toInt()
    val ACCENT_GREEN: Int get() = if (isDark) 0xFF54B054.toInt() else 0xFF107C10.toInt()
    val ACCENT_AMBER: Int get() = if (isDark) 0xFFFCE100.toInt() else 0xFFFFB900.toInt()
    val ACCENT_RED: Int get() = if (isDark) 0xFFF1707B.toInt() else 0xFFD13438.toInt()
    val BTN_MUTED: Int get() = if (isDark) 0xFF2D2D2D.toInt() else 0xFFFFFFFF.toInt()
    val LOGIN_BG: Int get() = if (isDark) 0xFF0D1520.toInt() else 0xFF0078D4.toInt()

    val BADGE_SUCCESS_BG: Int get() = if (isDark) 0xFF132A13.toInt() else 0xFFDFF6DD.toInt()
    val BADGE_SUCCESS_FG: Int get() = if (isDark) 0xFF54B054.toInt() else 0xFF107C10.toInt()
    val BADGE_DANGER_BG: Int get() = if (isDark) 0xFF321518.toInt() else 0xFFFDE7E9.toInt()
    val BADGE_DANGER_FG: Int get() = if (isDark) 0xFFF1707B.toInt() else 0xFFD13438.toInt()
    val BADGE_WARNING_BG: Int get() = if (isDark) 0xFF332B00.toInt() else 0xFFFFF4CE.toInt()
    val BADGE_WARNING_FG: Int get() = if (isDark) 0xFFFCE100.toInt() else 0xFFA17C00.toInt()
    val BADGE_INFO_BG: Int get() = if (isDark) 0xFF15293E.toInt() else 0xFFE1EFFF.toInt()
    val BADGE_INFO_FG: Int get() = if (isDark) 0xFF4AA3FF.toInt() else 0xFF0078D4.toInt()

    fun syncSystemNightMode(ctx: Context) {
        val uiMode = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        isSystemDark = (uiMode == Configuration.UI_MODE_NIGHT_YES)
    }

    fun dp(ctx: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()

    fun roundedBg(color: Int, radiusDp: Int, ctx: Context, strokeColor: Int = 0, strokeWidthDp: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(ctx, radiusDp).toFloat()
            if (strokeWidthDp > 0) {
                setStroke(dp(ctx, strokeWidthDp), strokeColor)
            }
        }

    fun leftBorderBg(fillColor: Int, borderLeftColor: Int, radiusDp: Int, ctx: Context): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(ctx, radiusDp).toFloat()
            setStroke(dp(ctx, 1), borderLeftColor)
        }

    fun vBox(ctx: Context, padDp: Int = 0): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            if (padDp > 0) {
                val p = dp(ctx, padDp)
                setPadding(p, p, p, p)
            }
        }

    fun hBox(ctx: Context, padDp: Int = 0): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (padDp > 0) {
                val p = dp(ctx, padDp)
                setPadding(p, p, p, p)
            }
        }

    fun card(ctx: Context, padDp: Int = 16): LinearLayout =
        vBox(ctx, padDp).apply {
            background = roundedBg(BG_CARD, 8, ctx, BORDER_COLOR, 1)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(ctx, 12)
            layoutParams = lp
        }

    fun subCard(ctx: Context, padDp: Int = 12): LinearLayout =
        vBox(ctx, padDp).apply {
            background = roundedBg(BG_SUBTLE, 6, ctx, BORDER_COLOR, 1)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(ctx, 8)
            layoutParams = lp
        }

    fun divider(ctx: Context, marginVerticalDp: Int = 8): View =
        View(ctx).apply {
            setBackgroundColor(BORDER_COLOR)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1))
            lp.topMargin = dp(ctx, marginVerticalDp)
            lp.bottomMargin = dp(ctx, marginVerticalDp)
            layoutParams = lp
        }

    fun label(ctx: Context, text: String, sizeSp: Float = 13f, color: Int = TEXT_PRIMARY, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    fun badge(ctx: Context, text: String, kind: String = "info"): TextView {
        val (bg, fg) = when (kind) {
            "success" -> BADGE_SUCCESS_BG to BADGE_SUCCESS_FG
            "danger" -> BADGE_DANGER_BG to BADGE_DANGER_FG
            "warning" -> BADGE_WARNING_BG to BADGE_WARNING_FG
            "neutral" -> BG_SUBTLE to TEXT_SECONDARY
            else -> BADGE_INFO_BG to BADGE_INFO_FG
        }
        return TextView(ctx).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(fg)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBg(bg, 99, ctx)
            setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2))
        }
    }

    fun pageHeader(ctx: Context, title: String, subtitle: String, actionsBuilder: (LinearLayout.() -> Unit)? = null): LinearLayout =
        vBox(ctx).apply {
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(ctx, 16)
            layoutParams = lp

            val row = hBox(ctx)
            val leftCol = vBox(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(label(ctx, title, 20f, TEXT_PRIMARY, true))
                if (subtitle.isNotEmpty()) {
                    addView(label(ctx, subtitle, 12f, TEXT_SECONDARY).apply {
                        setPadding(0, dp(ctx, 3), 0, 0)
                    })
                }
            }
            row.addView(leftCol)

            if (actionsBuilder != null) {
                val actionsRow = hBox(ctx)
                actionsRow.actionsBuilder()
                row.addView(actionsRow)
            }
            addView(row)
        }

    fun statCard(ctx: Context, labelText: String, valueText: String, valueColor: Int = TEXT_PRIMARY, weight: Float = 1f): LinearLayout =
        vBox(ctx, 14).apply {
            background = roundedBg(BG_CARD, 8, ctx, BORDER_COLOR, 1)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            lp.rightMargin = dp(ctx, 8)
            lp.bottomMargin = dp(ctx, 8)
            layoutParams = lp
            addView(label(ctx, labelText, 12f, TEXT_SECONDARY))
            addView(label(ctx, valueText, 24f, valueColor, true).apply {
                setPadding(0, dp(ctx, 4), 0, 0)
            })
        }

    fun input(ctx: Context, hintText: String, initial: String = "", isPassword: Boolean = false, multiLine: Boolean = false): EditText =
        EditText(ctx).apply {
            hint = hintText
            setText(initial)
            setHintTextColor(TEXT_TERTIARY)
            setTextColor(TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = roundedBg(BG_INPUT, 4, ctx, BORDER_COLOR, 1)
            val px = dp(ctx, 10)
            val py = dp(ctx, 8)
            setPadding(px, py, px, py)
            inputType = when {
                isPassword -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                multiLine -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                else -> InputType.TYPE_CLASS_TEXT
            }
            if (multiLine) {
                minLines = 6
                gravity = Gravity.TOP or Gravity.START
            } else {
                setSingleLine(true)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(ctx, 4)
            lp.bottomMargin = dp(ctx, 4)
            layoutParams = lp
        }

    fun button(
        ctx: Context,
        text: String,
        bgColor: Int = ACCENT_BLUE,
        textColor: Int = Color.WHITE,
        small: Boolean = false,
        onClick: () -> Unit
    ): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            val isNeutralBtn = (bgColor == BTN_MUTED || bgColor == BG_CARD || bgColor == BG_SUBTLE)
            val resolvedBg = if (isNeutralBtn) BG_CARD else bgColor
            val resolvedFg = if (isNeutralBtn && textColor == Color.WHITE) TEXT_PRIMARY else textColor
            val strokeCol = if (isNeutralBtn) BORDER_COLOR else resolvedBg
            setTextColor(resolvedFg)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (small) 12f else 13f)
            background = roundedBg(resolvedBg, 4, ctx, strokeCol, 1)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            val px = dp(ctx, if (small) 10 else 14)
            val py = dp(ctx, if (small) 5 else 8)
            setPadding(px, py, px, py)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = dp(ctx, 6)
            lp.topMargin = dp(ctx, 2)
            lp.bottomMargin = dp(ctx, 2)
            layoutParams = lp
            setOnClickListener { onClick() }
        }

    fun chipRow(
        ctx: Context,
        options: List<Pair<String, String>>,
        selectedKey: String,
        onSelect: (String) -> Unit
    ): HorizontalScrollView =
        HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            val row = hBox(ctx)
            options.forEach { (key, title) ->
                val active = key == selectedKey
                val btn = Button(ctx).apply {
                    text = title
                    isAllCaps = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(if (active) ACCENT_BLUE else TEXT_SECONDARY)
                    if (active) setTypeface(typeface, Typeface.BOLD)
                    background = roundedBg(
                        if (active) BG_ACCENT_SUBTLE else BG_CARD,
                        4,
                        ctx,
                        if (active) ACCENT_BLUE else BORDER_COLOR,
                        1
                    )
                    minHeight = 0
                    minimumHeight = 0
                    minWidth = 0
                    minimumWidth = 0
                    setPadding(dp(ctx, 10), dp(ctx, 5), dp(ctx, 10), dp(ctx, 5))
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    lp.rightMargin = dp(ctx, 6)
                    layoutParams = lp
                    setOnClickListener { onSelect(key) }
                }
                row.addView(btn)
            }
            addView(row)
        }

    fun switchRow(
        ctx: Context,
        title: String,
        subtitle: String = "",
        checked: Boolean,
        onToggle: (Boolean) -> Unit
    ): LinearLayout =
        vBox(ctx).apply {
            val row = hBox(ctx).apply {
                setPadding(dp(ctx, 4), dp(ctx, 10), dp(ctx, 4), dp(ctx, 10))
                val textBox = vBox(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    addView(label(ctx, title, 13f, TEXT_PRIMARY, true))
                    if (subtitle.isNotEmpty()) {
                        addView(label(ctx, subtitle, 11f, TEXT_SECONDARY).apply {
                            setPadding(0, dp(ctx, 2), 0, 0)
                        })
                    }
                }
                addView(textBox)
                val sw = Switch(ctx).apply {
                    isChecked = checked
                    setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
                }
                addView(sw)
            }
            addView(row)
            addView(View(ctx).apply {
                setBackgroundColor(BORDER_SUBTLE)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1))
            })
        }

    fun spacer(ctx: Context, heightDp: Int = 8): View =
        View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, heightDp))
        }
}

class ProfileEditorComponent(private val act: MainActivity) {
    private val resourceTypes = listOf(
        "Subjects" to "科目管理",
        "TimeLayout" to "时间表管理",
        "ClassPlan" to "课表管理",
        "Policy" to "策略管理",
        "DefaultSettings" to "默认设置",
        "Components" to "组件设置",
        "Credentials" to "凭据管理"
    )

    private val resourceSubtitles = mapOf(
        "Subjects" to "配置并向终端同步教学科目列表、科目简称与任课教师信息",
        "TimeLayout" to "维护标准作息时间表，设置上课、课间休息与分割线时间点",
        "ClassPlan" to "编排每日课程表、轮周规则并关联对应作息时间表",
        "Policy" to "控制终端允许用户修改的功能权限及是否允许退出集控",
        "DefaultSettings" to "配置窗口停靠位置、界面缩放、提醒特效、语音及主题外观",
        "Components" to "管理主界面组件行布局与内置组件编排",
        "Credentials" to "配置操作授权密码与各项敏感操作的权限验证等级"
    )

    var selectedType: String = "Subjects"
    var resourcesArray: JSONArray = JSONArray()
    var selectedResourceName: String = ""
    var editMode: String = "visual" // "visual" | "raw"
    var rawJsonContent: String = "{}"
    var newFileName: String = ""
    var showCreateBar: Boolean = false

    // Reference lookup caches for ClassPlan / TimeLayout
    var refSubjectsMap: Map<String, String> = emptyMap()
    var refTimeLayoutsList: List<Pair<String, String>> = emptyList()

    // Selected inner group key for TimeLayout / ClassPlan
    var activeGroupKey: String = ""

    fun selectResourceTypeFromSidebar(type: String) {
        selectedType = type
        selectedResourceName = ""
        showCreateBar = false
        loadResourcesList()
    }

    fun loadResourcesList() {
        val base = act.apiBase()
        val type = selectedType
        act.runAsync({
            val res = ApiClient.request(base, "/api/v1/profiles/resources?type=${ApiClient.enc(type)}")
            val subjRes = ApiClient.request(base, "/api/v1/profiles/resources?type=Subjects")
            val tlRes = ApiClient.request(base, "/api/v1/profiles/resources?type=TimeLayout")
            Triple(res, subjRes, tlRes)
        }) { (res, subjRes, tlRes) ->
            buildReferenceMaps(subjRes, tlRes)
            if (res.ok) {
                val arr = res.json.optJSONArray("resources") ?: JSONArray()
                resourcesArray = arr
                if (arr.length() > 0) {
                    var found: JSONObject? = null
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null && obj.optString("name") == selectedResourceName) {
                            found = obj
                            break
                        }
                    }
                    val target = found ?: arr.optJSONObject(0)
                    if (target != null) {
                        selectResourceObject(target)
                    }
                } else {
                    selectedResourceName = ""
                    rawJsonContent = ApiClient.getDefaultResourceJson(selectedType)
                    activeGroupKey = ""
                }
            } else {
                act.showBanner("加载资源失败: ${res.error}", true)
            }
            act.renderUI()
        }
    }

    private fun buildReferenceMaps(subjRes: HttpRes, tlRes: HttpRes) {
        val sMap = mutableMapOf<String, String>()
        val sArr = subjRes.json.optJSONArray("resources") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val contentStr = sArr.optJSONObject(i)?.optString("content") ?: continue
            try {
                val obj = JSONObject(contentStr)
                val subs = obj.optJSONObject("Subjects") ?: continue
                val keys = subs.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val name = subs.optJSONObject(k)?.optString("Name") ?: k
                    sMap[k] = name
                }
            } catch (_: Exception) {}
        }
        refSubjectsMap = sMap

        val tlList = mutableListOf<Pair<String, String>>()
        val tArr = tlRes.json.optJSONArray("resources") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val fileObj = tArr.optJSONObject(i) ?: continue
            val fileName = fileObj.optString("name")
            val contentStr = fileObj.optString("content")
            try {
                val obj = JSONObject(contentStr)
                val layouts = obj.optJSONObject("TimeLayouts") ?: continue
                val keys = layouts.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val name = layouts.optJSONObject(k)?.optString("Name") ?: fileName
                    tlList.add(k to "$fileName / $name")
                }
            } catch (_: Exception) {}
        }
        refTimeLayoutsList = tlList
    }

    private fun selectResourceObject(obj: JSONObject) {
        selectedResourceName = obj.optString("name")
        val content = obj.optString("content", "{}")
        rawJsonContent = try {
            JSONObject(content).toString(2)
        } catch (_: Exception) {
            content.ifBlank { ApiClient.getDefaultResourceJson(selectedType) }
        }
        activeGroupKey = ""
    }

    private fun parseCurrentJson(): JSONObject =
        try {
            JSONObject(rawJsonContent)
        } catch (_: Exception) {
            JSONObject(ApiClient.getDefaultResourceJson(selectedType))
        }

    private fun commitJsonAndRefresh(root: JSONObject) {
        rawJsonContent = root.toString(2)
        act.renderUI()
    }

    private fun saveCurrentResource(nameToSave: String = selectedResourceName) {
        val targetName = nameToSave.trim()
        if (targetName.isEmpty()) {
            act.showBanner("请输入资源文件名称", true)
            return
        }
        val validJson = try {
            JSONObject(rawJsonContent).toString()
        } catch (e: Exception) {
            act.showBanner("JSON 语法错误: ${e.message}", true)
            return
        }
        val body = JSONObject().apply {
            put("name", targetName)
            put("type", selectedType)
            put("content", validJson)
        }
        val base = act.apiBase()
        act.runAsync({
            ApiClient.request(base, "/api/v1/profiles/resources", "POST", body)
        }) { res ->
            if (res.ok) {
                selectedResourceName = targetName
                showCreateBar = false
                act.showBanner("已保存并发布 $selectedType / $targetName (版本 v${res.json.optInt("version", 1)})", false)
                loadResourcesList()
            } else {
                act.showBanner("保存失败: ${res.error}", true)
                act.renderUI()
            }
        }
    }

    private fun deleteCurrentResource() {
        val targetName = selectedResourceName.trim()
        if (targetName.isEmpty()) return
        val base = act.apiBase()
        val type = selectedType
        act.runAsync({
            ApiClient.request(
                base,
                "/api/v1/profiles/resources?name=${ApiClient.enc(targetName)}&type=${ApiClient.enc(type)}",
                "DELETE"
            )
        }) { res ->
            if (res.ok) {
                act.showBanner("已删除资源 $targetName", false)
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

        // Web Page Header (.page-header)
        root.addView(UiKit.pageHeader(ctx, typeTitle, typeDesc) {
            addView(UiKit.button(ctx, "+ 新建文件", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                showCreateBar = !showCreateBar
                act.renderUI()
            })
            addView(UiKit.button(ctx, "刷新", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                loadResourcesList()
            })
        })

        // Create file bar (if toggled)
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
                val n = newNameEdit.text.toString().trim()
                if (n.isNotEmpty()) {
                    newFileName = ""
                    selectedResourceName = n
                    rawJsonContent = ApiClient.getDefaultResourceJson(selectedType)
                    saveCurrentResource(n)
                } else {
                    act.showBanner("请输入新配置文件名称", true)
                }
            })
            row.addView(UiKit.button(ctx, "取消", UiKit.BTN_MUTED, UiKit.TEXT_SECONDARY, true) {
                showCreateBar = false
                act.renderUI()
            })
            createCard.addView(row)
            root.addView(createCard)
        }

        // Split Pane on Tablet/Landscape vs Stacked on Phone
        if (isTablet) {
            val split = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val leftCol = UiKit.vBox(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 240), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = UiKit.dp(ctx, 16)
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

    private fun buildResourceListCard(ctx: Context): LinearLayout {
        val card = UiKit.card(ctx, 12)
        val headerRow = UiKit.hBox(ctx)
        headerRow.addView(UiKit.label(ctx, "资源文件", 13f, UiKit.TEXT_SECONDARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(UiKit.badge(ctx, "${resourcesArray.length()}", "neutral"))
        card.addView(headerRow)
        card.addView(UiKit.spacer(ctx, 8))

        if (resourcesArray.length() == 0) {
            card.addView(UiKit.label(ctx, "暂无该类型资源，点击上方“+ 新建文件”创建", 12f, UiKit.TEXT_TERTIARY).apply {
                setPadding(UiKit.dp(ctx, 4), UiKit.dp(ctx, 12), UiKit.dp(ctx, 4), UiKit.dp(ctx, 12))
            })
        } else {
            for (i in 0 until resourcesArray.length()) {
                val obj = resourcesArray.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val ver = obj.optInt("version", 1)
                val active = (name == selectedResourceName)

                val itemRow = UiKit.hBox(ctx, 10).apply {
                    background = UiKit.roundedBg(
                        if (active) UiKit.BG_ACCENT_SUBTLE else Color.TRANSPARENT,
                        6,
                        ctx,
                        if (active) UiKit.ACCENT_BLUE else Color.TRANSPARENT,
                        if (active) 1 else 0
                    )
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = UiKit.dp(ctx, 4)
                    layoutParams = lp
                    setOnClickListener {
                        selectResourceObject(obj)
                        act.renderUI()
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
                itemRow.addView(UiKit.badge(ctx, "v$ver", if (active) "info" else "neutral"))
                card.addView(itemRow)
            }
        }
        return card
    }

    private fun buildResourceEditorCard(ctx: Context): LinearLayout {
        val card = UiKit.card(ctx, 16)
        if (selectedResourceName.isEmpty() && resourcesArray.length() == 0) {
            card.addView(UiKit.label(ctx, "请先在左侧选择或新建一个资源文件", 13f, UiKit.TEXT_SECONDARY))
            return card
        }

        // Top toolbar matching Web Profile Editor
        val bar = UiKit.hBox(ctx)
        val titleCol = UiKit.vBox(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(UiKit.label(
                ctx,
                selectedResourceName.ifEmpty { "未命名资源" },
                15f,
                UiKit.TEXT_PRIMARY,
                true
            ))
            addView(UiKit.label(ctx, "类型: $selectedType", 11f, UiKit.TEXT_SECONDARY))
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
            rawBtnRow.addView(UiKit.button(ctx, "格式化并应用", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                try {
                    rawJsonContent = JSONObject(rawInput.text.toString()).toString(2)
                    act.showBanner("JSON 语法校验通过", false)
                    act.renderUI()
                } catch (e: Exception) {
                    act.showBanner("JSON 格式有误: ${e.message}", true)
                }
            })
            rawBtnRow.addView(UiKit.button(ctx, "直接保存发布", UiKit.ACCENT_BLUE, Color.WHITE, true) {
                rawJsonContent = rawInput.text.toString()
                saveCurrentResource(selectedResourceName)
            })
            card.addView(rawBtnRow)
            return card
        }

        // Visual Editor by Type
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

    private fun buildSubjectsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        val subjectsObj = root.optJSONObject("Subjects") ?: JSONObject().also { root.put("Subjects", it) }

        val topBar = UiKit.hBox(ctx)
        topBar.addView(UiKit.label(ctx, "共 ${subjectsObj.length()} 门科目", 13f, UiKit.TEXT_SECONDARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        topBar.addView(UiKit.button(ctx, "+ 添加科目", UiKit.ACCENT_BLUE, Color.WHITE, true) {
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
        keys.forEach { key ->
            val item = subjectsObj.optJSONObject(key) ?: return@forEach
            val box = UiKit.subCard(ctx, 12)

            val r1 = UiKit.hBox(ctx)
            val initBadge = TextView(ctx).apply {
                text = item.optString("Initial", "课").take(2)
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

            val nameEdit = UiKit.input(ctx, "科目名称 (如 数学)", item.optString("Name")).apply {
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
                text = "户外课程 (体育/实验等)"
                setTextColor(UiKit.TEXT_SECONDARY)
                textSize = 12f
                isChecked = item.optBoolean("IsOutDoor", false)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            r2.addView(outdoorCb)
            r2.addView(UiKit.button(ctx, "更新项", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                item.put("Name", nameEdit.text.toString())
                item.put("Initial", initEdit.text.toString())
                item.put("TeacherName", teacherEdit.text.toString())
                item.put("IsOutDoor", outdoorCb.isChecked)
                commitJsonAndRefresh(root)
            })
            r2.addView(UiKit.button(ctx, "删除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                subjectsObj.remove(key)
                commitJsonAndRefresh(root)
            })
            box.addView(r2)
            container.addView(box)
        }
    }

    private fun buildTimeLayoutVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        val layoutsObj = root.optJSONObject("TimeLayouts") ?: JSONObject().also { root.put("TimeLayouts", it) }
        val keys = layoutsObj.keys().asSequence().toList()
        if (activeGroupKey.isEmpty() || !layoutsObj.has(activeGroupKey)) {
            activeGroupKey = keys.firstOrNull() ?: ""
        }

        val groupRow = UiKit.hBox(ctx)
        groupRow.addView(UiKit.label(ctx, "作息时间表分组:", 12f, UiKit.TEXT_SECONDARY, true))
        groupRow.addView(UiKit.button(ctx, "+ 新增时间表组", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val id = UUID.randomUUID().toString()
            layoutsObj.put(id, JSONObject().apply {
                put("Name", "新作息组")
                put("Layouts", JSONArray())
            })
            activeGroupKey = id
            commitJsonAndRefresh(root)
        })
        container.addView(groupRow)

        if (keys.isNotEmpty()) {
            val opts = keys.map { k ->
                k to (layoutsObj.optJSONObject(k)?.optString("Name") ?: k.take(8))
            }
            container.addView(UiKit.chipRow(ctx, opts, activeGroupKey) { k ->
                activeGroupKey = k
                act.renderUI()
            })
        }

        val activeGroup = layoutsObj.optJSONObject(activeGroupKey) ?: return
        val gNameRow = UiKit.hBox(ctx)
        val gNameInput = UiKit.input(ctx, "时间表组名称", activeGroup.optString("Name")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        gNameRow.addView(gNameInput)
        gNameRow.addView(UiKit.button(ctx, "改名", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            activeGroup.put("Name", gNameInput.text.toString())
            commitJsonAndRefresh(root)
        })
        gNameRow.addView(UiKit.button(ctx, "删除组", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            layoutsObj.remove(activeGroupKey)
            activeGroupKey = ""
            commitJsonAndRefresh(root)
        })
        container.addView(gNameRow)

        val addSlotRow = UiKit.hBox(ctx)
        val pts = activeGroup.optJSONArray("Layouts") ?: JSONArray().also { activeGroup.put("Layouts", it) }
        addSlotRow.addView(UiKit.button(ctx, "+ 上课时间段", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            pts.put(JSONObject().apply {
                put("StartSecond", "08:00:00")
                put("EndSecond", "08:45:00")
                put("TimeType", 0)
            })
            commitJsonAndRefresh(root)
        })
        addSlotRow.addView(UiKit.button(ctx, "+ 课间休息", UiKit.ACCENT_GREEN, Color.WHITE, true) {
            pts.put(JSONObject().apply {
                put("StartSecond", "08:45:00")
                put("EndSecond", "08:55:00")
                put("TimeType", 1)
                put("BreakName", "课间休息")
            })
            commitJsonAndRefresh(root)
        })
        addSlotRow.addView(UiKit.button(ctx, "+ 分割线", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            pts.put(JSONObject().apply {
                put("StartSecond", "12:00:00")
                put("EndSecond", "12:00:00")
                put("TimeType", 2)
            })
            commitJsonAndRefresh(root)
        })
        container.addView(addSlotRow)

        for (i in 0 until pts.length()) {
            val pt = pts.optJSONObject(i) ?: continue
            val tType = pt.optInt("TimeType", 0)
            val (typeTitle, badgeKind) = when (tType) {
                0 -> "上课" to "info"
                1 -> "课间休息" to "success"
                2 -> "分割线" to "warning"
                else -> "行动" to "neutral"
            }
            val box = UiKit.subCard(ctx, 10)
            val header = UiKit.hBox(ctx)
            header.addView(UiKit.badge(ctx, "#${i + 1} $typeTitle", badgeKind))
            header.addView(UiKit.label(
                ctx,
                "  ${extractTimePart(pt.optString("StartSecond"))} - ${extractTimePart(pt.optString("EndSecond"))}",
                12f,
                UiKit.TEXT_PRIMARY,
                true
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            header.addView(UiKit.button(ctx, "删除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                pts.remove(i)
                commitJsonAndRefresh(root)
            })
            box.addView(header)

            val editRow = UiKit.hBox(ctx)
            val startEdit = UiKit.input(ctx, "开始 HH:mm:ss", extractTimePart(pt.optString("StartSecond"))).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 6)
                }
            }
            val endEdit = UiKit.input(ctx, "结束 HH:mm:ss", extractTimePart(pt.optString("EndSecond"))).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 6)
                }
            }
            val breakEdit = UiKit.input(ctx, "课间名称/默认科目ID", pt.optString("BreakName", pt.optString("DefaultClassId"))).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = UiKit.dp(ctx, 6)
                }
            }
            editRow.addView(startEdit)
            editRow.addView(endEdit)
            editRow.addView(breakEdit)
            editRow.addView(UiKit.button(ctx, "应用", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                pt.put("StartSecond", startEdit.text.toString().trim())
                pt.put("EndSecond", endEdit.text.toString().trim())
                if (tType == 1) pt.put("BreakName", breakEdit.text.toString().trim())
                else pt.put("DefaultClassId", breakEdit.text.toString().trim())
                commitJsonAndRefresh(root)
            })
            box.addView(editRow)
            container.addView(box)
        }
    }

    private fun buildClassPlanVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        val plansObj = root.optJSONObject("ClassPlans") ?: JSONObject().also { root.put("ClassPlans", it) }
        val keys = plansObj.keys().asSequence().toList()
        if (activeGroupKey.isEmpty() || !plansObj.has(activeGroupKey)) {
            activeGroupKey = keys.firstOrNull() ?: ""
        }

        val topRow = UiKit.hBox(ctx)
        topRow.addView(UiKit.label(ctx, "课表群组:", 12f, UiKit.TEXT_SECONDARY, true))
        topRow.addView(UiKit.button(ctx, "+ 新增课表", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val id = UUID.randomUUID().toString()
            plansObj.put(id, JSONObject().apply {
                put("Name", "新课表")
                put("TimeLayoutId", refTimeLayoutsList.firstOrNull()?.first ?: "")
                put("IsEnabled", true)
                put("IsOverlay", false)
                put("TimeRule", JSONObject().put("WeekDay", 1).put("WeekCountDiv", 0).put("WeekCountDivTotal", 2))
                put("Classes", JSONArray())
            })
            activeGroupKey = id
            commitJsonAndRefresh(root)
        })
        container.addView(topRow)

        if (keys.isNotEmpty()) {
            val opts = keys.map { k -> k to (plansObj.optJSONObject(k)?.optString("Name") ?: k.take(8)) }
            container.addView(UiKit.chipRow(ctx, opts, activeGroupKey) { k ->
                activeGroupKey = k
                act.renderUI()
            })
        }

        val plan = plansObj.optJSONObject(activeGroupKey) ?: return
        val rule = plan.optJSONObject("TimeRule") ?: JSONObject().also { plan.put("TimeRule", it) }

        val nameRow = UiKit.hBox(ctx)
        val pNameEdit = UiKit.input(ctx, "课表名称", plan.optString("Name")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        nameRow.addView(pNameEdit)
        nameRow.addView(UiKit.button(ctx, "保存名称", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            plan.put("Name", pNameEdit.text.toString())
            commitJsonAndRefresh(root)
        })
        nameRow.addView(UiKit.button(ctx, "删除课表", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
            plansObj.remove(activeGroupKey)
            activeGroupKey = ""
            commitJsonAndRefresh(root)
        })
        container.addView(nameRow)

        container.addView(UiKit.label(ctx, "触发星期:", 12f, UiKit.TEXT_SECONDARY))
        val weekDays = listOf(
            "1" to "周一", "2" to "周二", "3" to "周三", "4" to "周四",
            "5" to "周五", "6" to "周六", "0" to "周日"
        )
        container.addView(UiKit.chipRow(ctx, weekDays, rule.optInt("WeekDay", 1).toString()) { w ->
            rule.put("WeekDay", w.toIntOrNull() ?: 1)
            commitJsonAndRefresh(root)
        })

        container.addView(UiKit.label(ctx, "轮周规则:", 12f, UiKit.TEXT_SECONDARY))
        val divOptions = listOf("0" to "每周生效", "1" to "单周 (第1周)", "2" to "双周 (第2周)", "3" to "第3周", "4" to "第4周")
        container.addView(UiKit.chipRow(ctx, divOptions, rule.optInt("WeekCountDiv", 0).toString()) { d ->
            rule.put("WeekCountDiv", d.toIntOrNull() ?: 0)
            commitJsonAndRefresh(root)
        })

        if (refTimeLayoutsList.isNotEmpty()) {
            container.addView(UiKit.label(ctx, "关联时间表:", 12f, UiKit.TEXT_SECONDARY))
            container.addView(UiKit.chipRow(ctx, refTimeLayoutsList, plan.optString("TimeLayoutId")) { tlId ->
                plan.put("TimeLayoutId", tlId)
                commitJsonAndRefresh(root)
            })
        }

        val classesArr = plan.optJSONArray("Classes") ?: JSONArray().also { plan.put("Classes", it) }
        val cHeader = UiKit.hBox(ctx)
        cHeader.addView(UiKit.label(ctx, "课程节次列表 (${classesArr.length()} 节)", 13f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        cHeader.addView(UiKit.button(ctx, "+ 添加一节课", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            val defaultSubj = refSubjectsMap.keys.firstOrNull() ?: ""
            classesArr.put(JSONObject().put("SubjectId", defaultSubj))
            commitJsonAndRefresh(root)
        })
        container.addView(cHeader)

        val subjChipOptions = if (refSubjectsMap.isNotEmpty()) {
            refSubjectsMap.entries.map { it.key to it.value }
        } else {
            listOf("" to "未配置科目")
        }

        for (i in 0 until classesArr.length()) {
            val cItem = classesArr.optJSONObject(i) ?: continue
            val curSubjId = cItem.optString("SubjectId")
            val curSubjName = refSubjectsMap[curSubjId] ?: curSubjId.ifEmpty { "未指定科目" }

            val box = UiKit.subCard(ctx, 10)
            val row = UiKit.hBox(ctx)
            row.addView(UiKit.badge(ctx, "第 ${i + 1} 节", "info"))
            row.addView(UiKit.label(ctx, "  $curSubjName", 13f, UiKit.TEXT_PRIMARY, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(UiKit.button(ctx, "移除", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                classesArr.remove(i)
                commitJsonAndRefresh(root)
            })
            box.addView(row)
            box.addView(UiKit.chipRow(ctx, subjChipOptions, curSubjId) { newSubjId ->
                cItem.put("SubjectId", newSubjId)
                commitJsonAndRefresh(root)
            })
            container.addView(box)
        }
    }

    private fun buildPolicyVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        container.addView(UiKit.label(ctx, "客户端集控限制策略", 14f, UiKit.TEXT_PRIMARY, true))
        val items = listOf(
            Triple("DisableProfileClassPlanEditing", "禁止终端修改课表", "锁定课表编排，仅允许集控下发"),
            Triple("DisableProfileTimeLayoutEditing", "禁止终端修改时间表", "锁定作息时间点"),
            Triple("DisableProfileSubjectsEditing", "禁止终端修改科目", "锁定科目与任课教师列表"),
            Triple("DisableProfileEditing", "完全禁止编辑档案", "锁定全部档案设置页"),
            Triple("DisableSettingsEditing", "禁止修改应用设置", "锁定客户端设置菜单"),
            Triple("DisableSplashCustomize", "禁止自定义启动画面", "强制使用标准启动屏"),
            Triple("DisableDebugMenu", "禁用调试菜单", "防止通过调试入口绕过管控"),
            Triple("AllowExitManagement", "允许主动退出集控", "开启后终端可在设置中脱离集控"),
            Triple("DisableEasterEggs", "禁用彩蛋特性", "关闭应用内置彩蛋")
        )
        items.forEach { (key, title, sub) ->
            container.addView(
                UiKit.switchRow(ctx, title, sub, root.optBoolean(key, false)) { v ->
                    root.put(key, v)
                    commitJsonAndRefresh(root)
                }
            )
        }
    }

    private fun buildDefaultSettingsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        container.addView(UiKit.label(ctx, "窗口停靠位置 (WindowDockingLocation)", 12f, UiKit.TEXT_SECONDARY, true))
        val dockOpts = listOf(
            "0" to "左上", "1" to "顶部居中", "2" to "右上",
            "3" to "左下", "4" to "底部居中", "5" to "右下"
        )
        container.addView(UiKit.chipRow(ctx, dockOpts, root.optInt("WindowDockingLocation", 1).toString()) { v ->
            root.put("WindowDockingLocation", v.toIntOrNull() ?: 1)
            commitJsonAndRefresh(root)
        })

        container.addView(UiKit.label(ctx, "界面主题模式 (Theme)", 12f, UiKit.TEXT_SECONDARY, true))
        val themeOpts = listOf("0" to "跟随系统", "1" to "浅色模式", "2" to "深色模式")
        container.addView(UiKit.chipRow(ctx, themeOpts, root.optInt("Theme", 0).toString()) { v ->
            root.put("Theme", v.toIntOrNull() ?: 0)
            commitJsonAndRefresh(root)
        })

        val numRow = UiKit.hBox(ctx)
        val scaleEdit = UiKit.input(ctx, "缩放比例 (Scale)", root.optDouble("Scale", 1.0).toString()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        val colorEdit = UiKit.input(ctx, "主题色 (#0078D4)", root.optString("PrimaryColor", "#0078D4")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        val offsetEdit = UiKit.input(ctx, "时间偏移(秒)", root.optDouble("TimeOffsetSeconds", 0.0).toString()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        numRow.addView(scaleEdit)
        numRow.addView(colorEdit)
        numRow.addView(offsetEdit)
        numRow.addView(UiKit.button(ctx, "应用参数", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            root.put("Scale", scaleEdit.text.toString().toDoubleOrNull() ?: 1.0)
            root.put("PrimaryColor", colorEdit.text.toString().ifBlank { "#0078D4" })
            root.put("TimeOffsetSeconds", offsetEdit.text.toString().toDoubleOrNull() ?: 0.0)
            commitJsonAndRefresh(root)
        })
        container.addView(numRow)

        val switches = listOf(
            "IsNotificationEnabled" to "启用全局课堂提醒通知",
            "IsNotificationEffectEnabled" to "启用全屏提醒波纹/光效",
            "IsNotificationSoundEnabled" to "启用提醒音效",
            "IsSpeechEnabled" to "启用 TTS 语音播报"
        )
        switches.forEach { (k, label) ->
            container.addView(UiKit.switchRow(ctx, label, "", root.optBoolean(k, true)) { v ->
                root.put(k, v)
                commitJsonAndRefresh(root)
            })
        }
    }

    private fun buildComponentsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        val lines = root.optJSONArray("Lines") ?: JSONArray().also { root.put("Lines", it) }

        val topRow = UiKit.hBox(ctx)
        topRow.addView(UiKit.label(ctx, "主界面组件行 (${lines.length()} 行)", 13f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        topRow.addView(UiKit.button(ctx, "+ 新增组件行", UiKit.ACCENT_BLUE, Color.WHITE, true) {
            lines.put(JSONObject().apply {
                put("IsMainLine", lines.length() == 0)
                put("IsEnabled", true)
                put("IslandSeparationMode", 0)
                put("Children", JSONArray())
            })
            commitJsonAndRefresh(root)
        })
        container.addView(topRow)

        for (i in 0 until lines.length()) {
            val line = lines.optJSONObject(i) ?: continue
            val children = line.optJSONArray("Children") ?: JSONArray().also { line.put("Children", it) }
            val lineBox = UiKit.subCard(ctx, 12)

            val lHeader = UiKit.hBox(ctx)
            lHeader.addView(UiKit.label(
                ctx,
                "第 ${i + 1} 行 ${if (line.optBoolean("IsMainLine")) "(主行)" else ""}",
                13f,
                UiKit.ACCENT_BLUE,
                true
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            lHeader.addView(UiKit.button(ctx, "设为主行", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
                for (j in 0 until lines.length()) {
                    lines.optJSONObject(j)?.put("IsMainLine", j == i)
                }
                commitJsonAndRefresh(root)
            })
            lHeader.addView(UiKit.button(ctx, "删除行", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                lines.remove(i)
                commitJsonAndRefresh(root)
            })
            lineBox.addView(lHeader)

            for (cIdx in 0 until children.length()) {
                val child = children.optJSONObject(cIdx) ?: continue
                val cRow = UiKit.hBox(ctx, 6)
                cRow.addView(UiKit.label(
                    ctx,
                    "• ${child.optString("NameCache", "组件")} (${child.optString("Id").take(8)})",
                    12f,
                    UiKit.TEXT_PRIMARY
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
                val alignOpts = listOf("0" to "左", "1" to "中", "2" to "右")
                cRow.addView(UiKit.chipRow(ctx, alignOpts, child.optInt("HorizontalAlignment", 1).toString()) { a ->
                    child.put("HorizontalAlignment", a.toIntOrNull() ?: 1)
                    commitJsonAndRefresh(root)
                })
                cRow.addView(UiKit.button(ctx, "×", UiKit.BADGE_DANGER_BG, UiKit.BADGE_DANGER_FG, true) {
                    children.remove(cIdx)
                    commitJsonAndRefresh(root)
                })
                lineBox.addView(cRow)
            }

            lineBox.addView(UiKit.label(ctx, "点击添加内置组件到第 ${i + 1} 行:", 11f, UiKit.TEXT_SECONDARY))
            val compOpts = BUILTIN_COMPONENTS.map { it.id to "+ ${it.name}" }
            lineBox.addView(UiKit.chipRow(ctx, compOpts, "") { compId ->
                val def = BUILTIN_COMPONENTS.find { it.id == compId }
                children.put(JSONObject().apply {
                    put("Id", compId)
                    put("NameCache", def?.name ?: "组件")
                    put("HorizontalAlignment", 1)
                })
                commitJsonAndRefresh(root)
            })

            container.addView(lineBox)
        }
    }

    private fun buildCredentialsVisual(ctx: Context, container: LinearLayout, root: JSONObject) {
        container.addView(UiKit.label(ctx, "客户端本地授权凭据配置", 13f, UiKit.TEXT_PRIMARY, true))
        val credRow = UiKit.hBox(ctx)
        val userCredEdit = UiKit.input(ctx, "普通用户密码/凭据", root.optString("UserCredential")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        val adminCredEdit = UiKit.input(ctx, "管理员密码/凭据", root.optString("AdminCredential")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(ctx, 6)
            }
        }
        credRow.addView(userCredEdit)
        credRow.addView(adminCredEdit)
        credRow.addView(UiKit.button(ctx, "保存凭据串", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            root.put("UserCredential", userCredEdit.text.toString())
            root.put("AdminCredential", adminCredEdit.text.toString())
            commitJsonAndRefresh(root)
        })
        container.addView(credRow)

        val authLevels = listOf("0" to "无须验证", "1" to "用户凭据", "2" to "管理员凭据")
        val actions = listOf(
            "EditProfileAuthorizeLevel" to "编辑档案授权等级",
            "EditPolicyAuthorizeLevel" to "编辑集控策略授权等级",
            "ExitManagementAuthorizeLevel" to "退出集控授权等级",
            "EditSettingsAuthorizeLevel" to "修改应用设置授权等级",
            "SwitchProfileAuthorizeLevel" to "切换档案配置授权等级",
            "TempOverlayClassPlanAuthorizeLevel" to "创建临时课表层授权等级",
            "ChangeTimeOffsetAuthorizeLevel" to "调整时间偏移授权等级",
            "ExitApplicationAuthorizeLevel" to "退出 ClassIsland 授权等级",
            "RestartApplicationAuthorizeLevel" to "重启 ClassIsland 授权等级"
        )
        actions.forEach { (key, title) ->
            val box = UiKit.subCard(ctx, 10)
            box.addView(UiKit.label(ctx, title, 12f, UiKit.TEXT_PRIMARY, true))
            box.addView(UiKit.chipRow(ctx, authLevels, root.optInt(key, 0).toString()) { lv ->
                root.put(key, lv.toIntOrNull() ?: 0)
                commitJsonAndRefresh(root)
            })
            container.addView(box)
        }
    }
}
