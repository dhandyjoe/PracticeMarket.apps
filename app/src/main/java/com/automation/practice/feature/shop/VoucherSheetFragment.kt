package com.automation.practice.feature.shop

import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.ItemVoucherBinding
import com.automation.practice.databinding.SheetVoucherBinding
import com.automation.practice.domain.VOUCHERS
import com.automation.practice.domain.voucherError
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** SHOP-10 Voucher: input kode + daftar voucher; hanya satu voucher aktif (FR-SHOP-18). */
class VoucherSheetFragment : BottomSheetDialogFragment() {
    private val vm: ShopViewModel by activityViewModels()
    private var binding: SheetVoucherBinding? = null
    private val rows = mutableListOf<View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val b = SheetVoucherBinding.inflate(inflater, container, false)
        binding = b
        b.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * SHEET_HEIGHT_RATIO).toInt(),
        )
        b.shopBarVoucherActions.applySystemInsets(bottom = true)
        b.shopBtnCloseVoucher.setOnClickListener { dismiss() }
        b.shopBtnVoucherDone.setOnClickListener { dismiss() }
        b.shopBtnPasteVoucher.setOnClickListener {
            val text = requireContext().getSystemService(ClipboardManager::class.java)
                .primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty()
            b.shopInputVoucherCode.setText(text.trim().uppercase())
            b.shopInputVoucherCode.setSelection(b.shopInputVoucherCode.text.length)
        }
        b.shopBtnApplyVoucher.setOnClickListener { applyCode(b.shopInputVoucherCode.text.toString()) }
        b.shopInputVoucherCode.setOnEditorActionListener { v, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) applyCode(v.text.toString()).let { true } else false
        }
        b.shopInputVoucherCode.doAfterTextChanged { showError(null) }
        render()
        return b.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        binding = null
        rows.clear()
        super.onDestroyView()
    }

    private fun subtotal() = vm.subtotal(vm.selectedLines())

    private fun applyCode(raw: String) {
        val code = raw.trim().uppercase()
        val error = voucherError(code, subtotal())
        if (error != null) return showError(error)
        vm.session.setVoucher(code)
        binding?.shopInputVoucherCode?.setText("")
        render()
    }

    private fun showError(message: String?) {
        val b = binding ?: return
        b.shopTxtVoucherError.isVisible = message != null
        b.shopTxtVoucherError.text = message.orEmpty()
        b.shopBoxVoucherCode.isSelected = message != null
    }

    private fun render() {
        val b = binding ?: return
        val active = vm.session.voucher.value
        rows.forEach(b.shopListVouchers::removeView)
        rows.clear()
        VOUCHERS.forEach { v ->
            val row = ItemVoucherBinding.inflate(layoutInflater, b.shopListVouchers, false)
            val on = v.code == active
            row.shopTxtVoucherCode.text = v.code
            row.shopTxtVoucherRowTitle.text = v.title
            row.shopTxtVoucherRowNote.text = v.note
            row.shopItemVoucherRow.setBackgroundResource(
                when { on -> R.drawable.bg_voucher_active; v.expired -> R.drawable.bg_voucher_off; else -> R.drawable.bg_voucher_idle },
            )
            row.shopItemVoucherRow.isEnabled = !v.expired
            row.shopTxtVoucherCode.isEnabled = !v.expired
            row.shopTxtVoucherRowTitle.isEnabled = !v.expired
            row.shopBtnVoucherApply.apply {
                isEnabled = !v.expired
                setText(if (on) R.string.voucher_remove else R.string.voucher_apply)
                if (on) {
                    backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.pm_surface))
                    setTextColor(context.getColor(R.color.pm_primary))
                    strokeColor = ColorStateList.valueOf(context.getColor(R.color.pm_primary))
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.pm_stroke_field)
                }
                setOnClickListener {
                    if (on) { vm.session.setVoucher(null); showError(null); render() }
                    else voucherError(v.code, subtotal())?.let(::showError) ?: run { vm.session.setVoucher(v.code); showError(null); render() }
                }
            }
            b.shopListVouchers.addView(row.root)
            rows += row.root
        }
        b.shopBtnVoucherDone.text = if (active == null) getString(R.string.voucher_done) else getString(R.string.voucher_done_fmt, active)
    }

    private companion object {
        const val SHEET_HEIGHT_RATIO = 0.82
    }
}
