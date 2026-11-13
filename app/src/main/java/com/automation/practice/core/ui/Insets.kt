package com.automation.practice.core.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Edge-to-edge: beri padding setinggi status bar, nav bar, dan keyboard. */
fun View.applySystemBarsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Tambah padding status bar ([top]) dan/atau nav bar + keyboard ([bottom]) di atas padding awal view.
 * [ime] false = abaikan keyboard (mis. tombol Simpan Alamat yang sengaja tertutup keyboard, FR-ACC-04).
 * Insets tidak dikonsumsi, jadi view lain di layar yang sama tetap menerimanya.
 */
fun View.applySystemInsets(top: Boolean = false, bottom: Boolean = false, ime: Boolean = true) {
    val startTop = paddingTop
    val startBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val withKeyboard = insets.getInsets(WindowInsetsCompat.Type.systemBars() or if (ime) WindowInsetsCompat.Type.ime() else 0)
        v.updatePadding(
            top = startTop + if (top) bars.top else 0,
            bottom = startBottom + if (bottom) withKeyboard.bottom else 0,
        )
        insets
    }
    if (isAttachedToWindow) requestApplyInsets() else addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { v.removeOnAttachStateChangeListener(this); v.requestApplyInsets() }
        override fun onViewDetachedFromWindow(v: View) = Unit
    })
}
