package com.automation.practice.feature.shop

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentResultBinding
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayOutcome
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar

/** SHOP-15 Hasil Pembayaran: Berhasil / Gagal / Menunggu Pembayaran. */
class ResultFragment : ShopFragment(R.layout.fragment_result) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val result = vm.result ?: return nav.selectTab(Tab.HOME)
        val b = FragmentResultBinding.bind(view)
        b.shopContentResult.applySystemInsets(top = true)
        b.shopBarResult.applySystemInsets(bottom = true)
        val ctx = requireContext()

        val (bg, icon, tint) = when (result.outcome) {
            PayOutcome.SUCCESS -> Triple(R.drawable.bg_result_ok, R.drawable.ic_check, R.color.pm_primary)
            PayOutcome.FAILED -> Triple(R.drawable.bg_result_fail, R.drawable.ic_error, R.color.pm_error)
            PayOutcome.PENDING -> Triple(R.drawable.bg_result_pending, R.drawable.ic_schedule, R.color.pm_warning)
        }
        b.shopImgResult.setBackgroundResource(bg)
        b.shopImgResult.setImageResource(icon)
        b.shopImgResult.imageTintList = ctx.getColorStateList(tint)

        b.shopTxtResultTitle.setText(
            when (result.outcome) { PayOutcome.SUCCESS -> R.string.result_ok_title; PayOutcome.FAILED -> R.string.result_fail_title; PayOutcome.PENDING -> R.string.result_pending_title },
        )
        b.shopTxtResultBody.text = result.reason.ifEmpty {
            ctx.getString(
                when (result.outcome) { PayOutcome.SUCCESS -> R.string.result_ok_body; PayOutcome.FAILED -> R.string.result_fail_body; PayOutcome.PENDING -> R.string.result_pending_body },
            )
        }

        val order = result.order
        b.shopCardResult.isVisible = order != null || result.outcome == PayOutcome.FAILED
        b.shopRowResultOrder.isVisible = order != null
        b.shopRowResultStatus.isVisible = order != null
        b.shopTxtResultTotal.text = result.total.toRupiah()
        b.shopTxtResultMethod.text = result.methodLabel
        if (order != null) {
            b.shopTxtOrderId.text = order.id
            val (label, color) = when (order.status) {
                OrderStatus.PAID -> R.string.result_status_paid to R.color.pm_primary_pressed
                OrderStatus.WAITING -> R.string.result_status_waiting to R.color.pm_warning_strong
                OrderStatus.CANCELLED -> R.string.result_status_cancelled to R.color.pm_error
                else -> R.string.result_status_paid to R.color.pm_primary_pressed
            }
            b.shopTxtResultStatus.setText(label)
            b.shopTxtResultStatus.setTextColor(ctx.getColor(color))
            b.shopBtnCopyOrder.setOnClickListener {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("order", order.id))
                Snackbar.make(view, R.string.result_order_copied, Snackbar.LENGTH_SHORT).setAnchorView(b.shopBarResult).show()
            }
        }

        b.shopBtnResultPrimary.setText(if (result.outcome == PayOutcome.FAILED) R.string.result_try_again else R.string.result_view_order)
        b.shopBtnResultPrimary.setOnClickListener {
            if (result.outcome == PayOutcome.FAILED) nav.openPayMethod()
            else nav.openOrders()
        }
        b.shopBtnResultHome.setOnClickListener { nav.selectTab(Tab.HOME) }
    }
}
