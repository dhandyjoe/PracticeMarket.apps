package com.automation.practice.feature.shop

import androidx.annotation.LayoutRes
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.annotation.StringRes
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.LayoutTopbarBinding
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Product
import com.google.android.material.snackbar.Snackbar

/** Dasar layar Belanja: akses navigator, ViewModel bersama, dan warna ikon status bar. */
abstract class ShopFragment(
    @LayoutRes layout: Int,
    private val lightStatusBar: Boolean = true,
) : Fragment(layout) {
    protected val vm: ShopViewModel by activityViewModels()
    protected val nav get() = requireActivity() as ShopNavigator

    override fun onStart() {
        super.onStart()
        val window = requireActivity().window
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = lightStatusBar && resources.getBoolean(R.bool.pm_light_bars)
    }

    /** Top app bar standar: inset status bar, judul, dan tombol kembali. */
    protected fun LayoutTopbarBinding.setup(@StringRes title: Int) {
        root.applySystemInsets(top = true)
        shopTxtTitle.setText(title)
        shopBtnBack.setOnClickListener { nav.back() }
    }

    /** Snackbar `Ditambahkan ke keranjang` + aksi `Lihat` (FR-SHOP-10). */
    protected fun showAdded(anchor: android.view.View? = null) {
        Snackbar.make(requireView(), R.string.cart_added, Snackbar.LENGTH_LONG)
            .setAction(R.string.cart_view) { nav.selectTab(Tab.CART) }
            .setAnchorView(anchor)
            .show()
    }

    /** Tambah 1 pcs varian pertama yang tersedia, dari tombol "+ Keranjang" di kartu. */
    protected fun quickAdd(p: Product) {
        val v = p.firstAvailable() ?: return
        vm.session.addToCart(CartLine(p.id, v.color, v.size, 1))
        showAdded()
    }
}
