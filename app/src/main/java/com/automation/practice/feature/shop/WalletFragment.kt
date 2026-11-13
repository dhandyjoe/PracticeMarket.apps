package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentWalletBinding
import com.automation.practice.databinding.ItemPayTxBinding
import com.automation.practice.domain.Tx
import com.automation.practice.domain.formatDateShort
import com.automation.practice.domain.formatHm
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** PAY-01 PracticePay: saldo dengan toggle sembunyikan, aksi cepat, 4 transaksi terakhir (FR-PAY-01). */
class WalletFragment : ShopFragment(R.layout.fragment_wallet, lightStatusBar = false) {
    private var hidden = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentWalletBinding.bind(view)
        b.payHeader.applySystemInsets(top = true)
        hidden = savedInstanceState?.getBoolean(STATE_HIDDEN) ?: false

        b.payBtnToggleBalance.setOnClickListener { hidden = !hidden; renderBalance(b, vm.session.balance.value) }
        b.payBtnTopup.setOnClickListener { Snackbar.make(view, R.string.pay_soon, Snackbar.LENGTH_SHORT).show() }
        b.payBtnTransfer.setOnClickListener { nav.openContacts() }
        b.payBtnScan.setOnClickListener { nav.openScanQr() }
        b.payBtnHistory.setOnClickListener { nav.openHistory() }
        b.payLinkSeeAll.setOnClickListener { nav.openHistory() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(vm.session.balance, vm.session.transactions) { bal, tx -> bal to tx }.collect { (bal, tx) ->
                    renderBalance(b, bal)
                    renderTx(b, tx.take(RECENT))
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HIDDEN, hidden)
    }

    private fun renderBalance(b: FragmentWalletBinding, balance: Long) {
        b.payTxtBalance.text = if (hidden) getString(R.string.pay_balance_hidden) else balance.toRupiah()
        b.payBtnToggleBalance.contentDescription = getString(if (hidden) R.string.pay_show_balance else R.string.pay_hide_balance)
        b.payBtnToggleBalance.setImageResource(if (hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
    }

    private fun renderTx(b: FragmentWalletBinding, list: List<Tx>) {
        b.payListRecent.removeAllViews()
        b.payListRecent.isVisible = list.isNotEmpty()
        b.payTxtNoTx.isVisible = list.isEmpty()
        list.forEach { t ->
            val r = ItemPayTxBinding.inflate(layoutInflater, b.payListRecent, false)
            r.payTxtTxTitle.text = t.title
            r.payTxtTxTime.text = getString(R.string.pay_tx_time_fmt, formatDateShort(t.at), formatHm(t.at))
            r.payTxtTxAmount.text = (if (t.incoming) "+" else "−") + kotlin.math.abs(t.amount).toRupiah()
            r.payTxtTxAmount.setTextColor(requireContext().getColor(if (t.incoming) R.color.pm_primary_pressed else R.color.pm_ink))
            r.payImgTx.setImageResource(if (t.incoming) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up)
            r.payImgTx.setBackgroundResource(R.drawable.bg_status_ok.takeIf { t.incoming } ?: R.drawable.bg_status_muted)
            r.payImgTx.imageTintList = requireContext().getColorStateList(if (t.incoming) R.color.pm_primary_pressed else R.color.pm_ink_secondary)
            b.payListRecent.addView(r.root)
        }
    }

    private companion object {
        const val RECENT = 4
        const val STATE_HIDDEN = "balance_hidden"
    }
}
