package com.automation.practice.feature.shop

import androidx.core.os.bundleOf
import com.automation.practice.R
import com.automation.practice.domain.toRupiah
import com.automation.practice.domain.transferTitle

/** PAY-04 Transfer · nominal & PIN (FR-PAY-05/06/07). */
class AmountFragment : PayEntryFragment() {
    override val title = R.string.pay_transfer
    override val payeeName get() = requireArguments().getString(ARG_NAME).orEmpty()
    override val payeeSub get() = getString(R.string.pay_payee_sub_fmt, requireArguments().getString(ARG_PHONE).orEmpty())
    override val fixedAmount: Long? = null
    override val initialAmount = 0L
    override val isTransfer = true
    override val buttonRes = R.string.pay_next
    override val successTitle = R.string.pay_transfer_ok
    override val successBodyRes = R.string.pay_transfer_ok_body_fmt

    override suspend fun commit(amount: Long, note: String): String? {
        val error = vm.failureText(vm.session.spend(amount, transferTitle(payeeName)))
        if (error == null) OrderNotifier.postText(
            requireContext(), "transfer-$payeeName-${System.currentTimeMillis()}",
            getString(R.string.pay_notif_received_fmt, payeeName, amount.toRupiah()), "practiceapp://pay",
        )
        return error
    }

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_PHONE = "phone"
        fun newInstance(name: String, phone: String) = AmountFragment().apply { arguments = bundleOf(ARG_NAME to name, ARG_PHONE to phone) }
    }
}
