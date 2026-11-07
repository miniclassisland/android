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
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

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

    fun iconButton(
        ctx: Context,
        icon: MdsIcon,
        bgColor: Int = BTN_MUTED,
        iconTint: Int = TEXT_PRIMARY,
        sizeDp: Int = 28,
        onClick: () -> Unit
    ): LinearLayout =
        hBox(ctx).apply {
            val isNeutralBtn = (bgColor == BTN_MUTED || bgColor == BG_CARD || bgColor == BG_SUBTLE)
            val resolvedBg = if (isNeutralBtn) BG_CARD else bgColor
            val strokeCol = if (isNeutralBtn) BORDER_COLOR else resolvedBg
            background = roundedBg(resolvedBg, 4, ctx, strokeCol, 1)
            gravity = Gravity.CENTER
            val px = dp(ctx, sizeDp)
            val lp = LinearLayout.LayoutParams(px, px)
            lp.rightMargin = dp(ctx, 6)
            lp.topMargin = dp(ctx, 2)
            lp.bottomMargin = dp(ctx, 2)
            layoutParams = lp
            addView(OfficialIcons.imageView(ctx, icon, iconTint, (sizeDp * 0.6f).toInt()))
            setOnClickListener { onClick() }
        }

    fun iconTextButton(
        ctx: Context,
        icon: MdsIcon,
        text: String,
        bgColor: Int = ACCENT_BLUE,
        textColor: Int = Color.WHITE,
        small: Boolean = false,
        onClick: () -> Unit
    ): LinearLayout =
        hBox(ctx).apply {
            val isNeutralBtn = (bgColor == BTN_MUTED || bgColor == BG_CARD || bgColor == BG_SUBTLE)
            val resolvedBg = if (isNeutralBtn) BG_CARD else bgColor
            val resolvedFg = if (isNeutralBtn && textColor == Color.WHITE) TEXT_PRIMARY else textColor
            val strokeCol = if (isNeutralBtn) BORDER_COLOR else resolvedBg
            background = roundedBg(resolvedBg, 4, ctx, strokeCol, 1)
            gravity = Gravity.CENTER_VERTICAL
            val px = dp(ctx, if (small) 10 else 14)
            val py = dp(ctx, if (small) 5 else 8)
            setPadding(px, py, px, py)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = dp(ctx, 6)
            lp.topMargin = dp(ctx, 2)
            lp.bottomMargin = dp(ctx, 2)
            layoutParams = lp
            addView(OfficialIcons.imageView(ctx, icon, resolvedFg, if (small) 15 else 17).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(ctx, 5)
            })
            addView(label(ctx, text, if (small) 12f else 13f, resolvedFg, true))
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
