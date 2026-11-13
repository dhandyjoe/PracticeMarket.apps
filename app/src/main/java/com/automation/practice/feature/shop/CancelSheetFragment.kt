package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.SheetCancelBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** ORD-03 Batalkan Pesanan: tombol disabled sampai alasan dipilih; "Lainnya" meminta teks (FR-ORD-03). */
class CancelSheetFragment : BottomSheetDialogFragment() {
    private val vm: ShopViewModel by activityViewModels()
    private val nav get() = requireActivity() as ShopNavigator
    private val orderId get() = requireArguments().getString(ARG_ID).orEmpty()
    private var reason = ""
    private var other = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val b = SheetCancelBinding.inflate(inflater, container, false)
        b.shopSheetCancel.applySystemInsets(bottom = true)
        b.shopTxtCancelSub.text = getString(R.string.cancel_sub_fmt, orderId)

        val reasons = listOf(R.string.cancel_reason_address, R.string.cancel_reason_payment, R.string.cancel_reason_cheaper, R.string.cancel_reason_other)
        reasons.forEach { res ->
            b.shopGroupCancelReason.addView(
                RadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    setText(res)
                    tag = res
                    setTextAppearance(R.style.TextAppearance_PracticeMart_Body)
                    setTextColor(context.getColor(R.color.pm_ink))
                    buttonTintList = context.getColorStateList(R.color.pm_primary)
                    minHeight = resources.getDimensionPixelSize(R.dimen.pm_touch_min)
                    contentDescription = getString(res)
                },
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        fun refresh() {
            b.shopBtnCancelConfirm.isEnabled = reason.isNotEmpty() && (!other || b.shopInputCancelNote.text.isNotBlank())
        }
        b.shopGroupCancelReason.setOnCheckedChangeListener { group, checkedId ->
            val res = group.findViewById<View>(checkedId).tag as Int
            other = res == R.string.cancel_reason_other
            reason = getString(res)
            b.shopLabelCancelNote.isVisible = other
            b.shopInputCancelNote.isVisible = other
            refresh()
        }
        b.shopInputCancelNote.doAfterTextChanged { refresh() }
        b.shopBtnCancelBack.setOnClickListener { dismiss() }
        b.shopBtnCancelConfirm.setOnClickListener {
            val text = if (other) b.shopInputCancelNote.text.toString().trim() else reason
            vm.cancelOrder(orderId, text)
            b.shopPanelCancelForm.isVisible = false
            b.shopPanelCancelDone.isVisible = true
        }
        b.shopBtnCancelDone.setOnClickListener {
            dismiss()
            nav.openOrders()
        }
        return b.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    companion object {
        private const val ARG_ID = "id"
        fun newInstance(id: String) = CancelSheetFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
