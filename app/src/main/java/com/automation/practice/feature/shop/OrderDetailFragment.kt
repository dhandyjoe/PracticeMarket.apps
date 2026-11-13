package com.automation.practice.feature.shop

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentOrderDetailBinding
import com.automation.practice.databinding.ItemTimelineStepBinding
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.formatHms
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** ORD-02 Detail & Tracking: status maju otomatis (atau manual), salin Order ID, batal bila ≤ Dikemas. */
class OrderDetailFragment : ShopFragment(R.layout.fragment_order_detail) {
    private val orderId get() = requireArguments().getString(ARG_ID).orEmpty()
    private var steps = emptyList<ItemTimelineStepBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentOrderDetailBinding.bind(view)
        b.shopTopbar.setup(R.string.ord_detail_title)
        b.shopBarDetail.applySystemInsets(bottom = true)

        steps = OrderStatus.entries.filter { it != OrderStatus.CANCELLED }.map {
            ItemTimelineStepBinding.inflate(layoutInflater, b.shopListTimeline, false).also { row ->
                row.shopTxtStepLabel.setText(it.label())
                b.shopListTimeline.addView(row.root)
            }
        }
        b.shopBtnCancelOrder.setOnClickListener { nav.openCancel(orderId) }
        b.shopBtnNextStatus.setOnClickListener { vm.session.advance(orderId)?.let { s -> notifyStatus(s) } }
        b.shopBtnReview.setOnClickListener { Snackbar.make(view, R.string.ord_review_soon, Snackbar.LENGTH_SHORT).show() }
        b.shopBtnCopyOrderId.setOnClickListener {
            requireContext().getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("order", orderId))
            Snackbar.make(view, R.string.result_order_copied, Snackbar.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.session.orders.collect { list -> list.firstOrNull { it.id == orderId }?.let { render(b, it) } ?: nav.back() } }
                launch { requireContext().app.container.settings.prefs.collect { renderMode(b) } }
            }
        }
    }

    /** Majukan manual tidak lewat simulator, jadi kabari lewat notifikasi seperti mode otomatis. */
    private fun notifyStatus(s: OrderStatus) =
        vm.session.order(orderId)?.let { OrderNotifier.postStatus(requireContext(), it, s) }

    private fun renderMode(b: FragmentOrderDetailBinding) {
        val ms = requireContext().app.container.settings.prefs.value.statusIntervalMs
        b.shopTxtDetailInterval.text = if (ms == 0L) getString(R.string.ord_interval_manual) else getString(R.string.ord_interval_fmt, ms / 1000)
        b.shopBtnNextStatus.isVisible = ms == 0L && vm.session.order(orderId)?.status?.next() != null
    }

    private fun render(b: FragmentOrderDetailBinding, o: Order) {
        b.shopTxtDetailOrderId.text = o.id
        b.shopTxtDetailMethod.text = o.methodLabel
        b.shopTxtDetailTotal.text = o.total.toRupiah()
        val cancelled = o.status == OrderStatus.CANCELLED
        b.shopListTimeline.isVisible = !cancelled
        b.shopTxtDetailCancelled.isVisible = cancelled
        if (cancelled) b.shopTxtDetailCancelled.text = o.cancelReason.ifEmpty { getString(R.string.ord_cancelled_body) }

        val at = o.status.ordinal
        steps.forEachIndexed { i, row ->
            val done = i < at || (i == at && o.status == OrderStatus.DONE)
            val current = i == at && !done
            row.shopImgStep.setBackgroundResource(if (done) R.drawable.bg_step_done else if (current) R.drawable.bg_step_current else R.drawable.bg_step_todo)
            row.shopImgStep.imageTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.pm_white))
            row.shopImgStep.setImageResource(if (done) R.drawable.ic_check else 0)
            row.shopLineStep.isVisible = i < steps.size - 1
            row.shopLineStep.setBackgroundColor(requireContext().getColor(if (i < at) R.color.pm_primary else R.color.pm_outline))
            row.shopTxtStepLabel.setTextColor(requireContext().getColor(if (i <= at) R.color.pm_ink else R.color.pm_ink_muted))
            row.shopTxtStepLabel.setTypeface(row.shopTxtStepLabel.typeface, if (current) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            row.shopTxtStepSub.text = o.stamps[OrderStatus.entries[i]]?.let(::formatHms) ?: getString(if (i <= at) R.string.ord_step_passed else R.string.ord_step_not_yet)
        }

        b.shopBtnCancelOrder.isVisible = o.status.cancellable
        b.shopTxtCannotCancel.isVisible = o.status == OrderStatus.SHIPPED
        b.shopBtnReview.isVisible = o.status == OrderStatus.DONE
        b.shopBarDetail.isVisible = !cancelled
        renderMode(b)
    }

    companion object {
        private const val ARG_ID = "id"
        fun newInstance(id: String) = OrderDetailFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
