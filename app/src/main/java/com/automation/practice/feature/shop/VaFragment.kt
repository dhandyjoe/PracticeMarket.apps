package com.automation.practice.feature.shop

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentVaBinding
import com.automation.practice.databinding.ItemVaGuideBinding
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** SHOP-14 Instruksi Virtual Account: nomor VA, salin, hitung mundur 15 menit, cara bayar. */
class VaFragment : ShopFragment(R.layout.fragment_va) {
    private var openGuide = 0
    private var guideRows: List<ItemVaGuideBinding> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pending = vm.va ?: return nav.back()
        val b = FragmentVaBinding.bind(view)
        b.shopHeaderVa.applySystemInsets(top = true)
        b.shopBarVa.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }

        b.shopTxtVaBank.text = getString(R.string.va_account_title_fmt, pending.bank)
        b.shopTxtVaNumber.text = pending.number
        b.shopTxtVaTotal.text = pending.total.toRupiah()

        b.shopBtnCopyVa.setOnClickListener {
            requireContext().getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("va", pending.number.replace(" ", "")))
            Snackbar.make(view, R.string.va_copied, Snackbar.LENGTH_SHORT).setAnchorView(b.shopBarVa).show()
        }
        // shortcut: status selalu "belum dibayar"; FR-PAYM-09 (gagal memuat) menunggu "Peluang error server" di Developer Options.
        b.shopBtnCheckStatus.setOnClickListener {
            Snackbar.make(view, R.string.va_not_paid, Snackbar.LENGTH_SHORT).setAnchorView(b.shopBarVa).show()
        }
        b.shopBtnPaid.setOnClickListener { vm.vaPaid() }

        val guides = listOf(
            R.string.va_guide_mobile to listOf(R.string.va_step_mobile_1, R.string.va_step_mobile_2, R.string.va_step_mobile_3, R.string.va_step_mobile_4),
            R.string.va_guide_atm to listOf(R.string.va_step_atm_1, R.string.va_step_atm_2, R.string.va_step_atm_3),
            R.string.va_guide_internet to listOf(R.string.va_step_net_1, R.string.va_step_net_2, R.string.va_step_net_3),
        )
        guideRows = guides.mapIndexed { index, (title, steps) ->
            val row = ItemVaGuideBinding.inflate(layoutInflater, b.shopListVaGuides, false)
            row.shopTxtVaGuideTitle.setText(title)
            row.shopTxtVaGuideSteps.text = steps.mapIndexed { i, s -> "${i + 1}. ${getString(s)}" }.joinToString("\n")
            row.shopBtnVaGuide.setOnClickListener { openGuide = if (openGuide == index) -1 else index; renderGuides() }
            b.shopListVaGuides.addView(row.root)
            row
        }
        renderGuides()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val left = ((pending.deadline - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                    b.shopTxtVaTimer.text = "%02d:%02d".format(left / 60, left % 60)
                    if (left == 0L) { vm.vaExpired(); break }
                    delay(1000)
                }
            }
        }
    }

    private fun renderGuides() {
        guideRows.forEachIndexed { i, row ->
            val open = i == openGuide
            row.shopTxtVaGuideSteps.isVisible = open
            row.shopBtnVaGuide.contentDescription = row.shopTxtVaGuideTitle.text
            row.shopBtnVaGuide.isSelected = open
            row.shopImgVaGuideChevron.rotation = if (open) 180f else 0f
        }
    }
}
