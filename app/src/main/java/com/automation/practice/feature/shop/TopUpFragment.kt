package com.automation.practice.feature.shop

import com.automation.practice.R
import com.automation.practice.domain.TX_TOPUP_TITLE

/** PAY-02 Top Up · nominal langsung menambah saldo, tanpa PIN dan tanpa pilih bank. */
class TopUpFragment : PayEntryFragment() {
    override val title = R.string.pay_topup
    override val payeeName get() = getString(R.string.pay_topup_payee)
    override val payeeSub get() = getString(R.string.pay_topup_sub)
    override val fixedAmount: Long? = null
    override val initialAmount = 0L
    override val isTransfer = false
    override val topUp = true
    override val buttonRes = R.string.pay_topup_button_fmt
    override val successTitle = R.string.pay_topup_ok
    override val successBodyRes = R.string.pay_topup_ok_body_fmt

    override suspend fun commit(amount: Long, note: String): String? =
        vm.failureText(vm.session.topUp(amount, TX_TOPUP_TITLE))
}
