package com.automation.practice.core.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView

/**
 * Locator Hint Overlay (PRD DEV-03): menggambar resource-id kecil di atas tiap view interaktif atau teks yang punya id.
 * Hanya mencakup View di jendela utama; elemen Compose (testTag) dan sheet/dialog berjendela terpisah tidak ikut.
 */
class LocatorHints(private val root: ViewGroup) {
    private val bg = Paint().apply { color = Color.argb(215, 18, 22, 28) }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(124, 227, 164); textSize = root.resources.displayMetrics.density * 8f }
    private val invalidate = ViewTreeObserver.OnGlobalLayoutListener { drawable.invalidateSelf() }
    private val scrolled = ViewTreeObserver.OnScrollChangedListener { drawable.invalidateSelf() }
    private var on = false

    private val drawable = object : Drawable() {
        override fun draw(canvas: Canvas) {
            val origin = IntArray(2).also { root.getLocationInWindow(it) }
            val loc = IntArray(2)
            fun walk(v: View) {
                if (v.visibility != View.VISIBLE) return
                if (v.id != View.NO_ID && (v.isClickable || v is TextView) && v.width > 0) {
                    val name = runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull()
                    if (name != null) {
                        v.getLocationInWindow(loc)
                        val x = (loc[0] - origin[0]).toFloat()
                        val y = (loc[1] - origin[1]).toFloat()
                        val w = fg.measureText(name)
                        canvas.drawRect(x, y, x + w + 6f, y + fg.textSize + 4f, bg)
                        canvas.drawText(name, x + 3f, y + fg.textSize, fg)
                    }
                }
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(root)
        }

        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    fun set(enabled: Boolean) {
        if (enabled == on) return
        on = enabled
        if (enabled) {
            drawable.setBounds(0, 0, root.width, root.height)
            root.overlay.add(drawable)
            root.viewTreeObserver.addOnGlobalLayoutListener(invalidate)
            root.viewTreeObserver.addOnScrollChangedListener(scrolled)
            root.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ -> drawable.setBounds(0, 0, r - l, b - t) }
        } else {
            root.overlay.remove(drawable)
            root.viewTreeObserver.removeOnGlobalLayoutListener(invalidate)
            root.viewTreeObserver.removeOnScrollChangedListener(scrolled)
        }
        root.invalidate()
    }
}
