package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentPlaceholderBinding

/** Tab yang layarnya belum dibangun (Kategori, PracticePay, Keranjang). */
class PlaceholderFragment : ShopFragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.applySystemInsets(top = true)
        FragmentPlaceholderBinding.bind(view).shopTxtPlaceholderTitle.setText(requireArguments().getInt(ARG_TITLE))
    }

    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(@StringRes title: Int) = PlaceholderFragment().apply { arguments = bundleOf(ARG_TITLE to title) }
    }
}
