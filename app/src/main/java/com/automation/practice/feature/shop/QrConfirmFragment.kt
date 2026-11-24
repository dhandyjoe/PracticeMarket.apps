package com.automation.practice.feature.shop

import androidx.core.os.bundleOf
import com.automation.practice.R
import com.automation.practice.domain.merchantOf
import com.automation.practice.domain.qrTitle

/** PAY-05 Konfirmasi bayar QR: merchant + nominal (bebas atau tetap) → PIN → "Pembayaran berhasil" (FR-PAY-10). */
class QrConfirmFragment : PayEntryFragment() {
    private val merchant get() = merchantOf(requireArguments().getString(ARG_MERCHANT))!!
    private val given get() = requireArguments().getLong(ARG_AMOUNT)

    override val title = R.string.pay_pay
    override val payeeName get() = merchant.name
    override val payeeSub get() = getString(R.string.pay_merchant_sub)
    override val fixedAmount get() = merchant.fixedAmount
    override val initialAmount get() = if (given > 0) given else 0L
    override val isTransfer = false
    override val buttonRes = R.string.pay_pay_fmt
    override val successTitle = R.string.pay_qr_ok
    override val successBodyRes = R.string.pay_qr_ok_body_fmt

    override suspend fun commit(amount: Long, note: String) = vm.failureText(vm.session.spend(amount, qrTitle(merchant.name)))

    companion object {
        private const val ARG_MERCHANT = "merchant"
        private const val ARG_AMOUNT = "amount"
        fun newInstance(merchant: String, amount: Long) = QrConfirmFragment().apply { arguments = bundleOf(ARG_MERCHANT to merchant, ARG_AMOUNT to amount) }
    }
}
