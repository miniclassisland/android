package org.miniopensource.miniclassisland

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.LinearLayout

enum class MdsIcon(val resName: String) {
    ADD("ic_add"),
    DELETE("ic_delete"),
    ARROW_UP("ic_arrow_up"),
    ARROW_DOWN("ic_arrow_down"),
    ARROW_LEFT("ic_arrow_left"),
    ARROW_RIGHT("ic_arrow_right"),
    CHEVRON_DOWN("ic_chevron_down"),
    MENU("ic_menu"),
    SETTINGS("ic_settings"),
    REFRESH("ic_refresh"),
    HISTORY("ic_history"),
    SWAP("ic_swap"),
    COPY("ic_copy"),
    FOLDER("ic_folder"),
    CODE("ic_code"),
    SAVE("ic_save"),
    KEY("ic_key"),
    SHIELD("ic_shield"),
    QR_CODE("ic_qr_code"),
    CLOSE("ic_close")
}

object OfficialIcons {
    fun getResId(ctx: Context, icon: MdsIcon): Int {
        return ctx.resources.getIdentifier(icon.resName, "drawable", ctx.packageName)
    }

    fun drawable(ctx: Context, icon: MdsIcon, tintColor: Int): Drawable? {
        val id = getResId(ctx, icon)
        if (id == 0) return null
        val d = ctx.getDrawable(id)?.mutate() ?: return null
        d.setTintList(ColorStateList.valueOf(tintColor))
        return d
    }

    fun imageView(ctx: Context, icon: MdsIcon, tintColor: Int, sizeDp: Int = 18): ImageView {
        return ImageView(ctx).apply {
            setImageDrawable(drawable(ctx, icon, tintColor))
            val px = UiKit.dp(ctx, sizeDp)
            layoutParams = LinearLayout.LayoutParams(px, px)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }
}
