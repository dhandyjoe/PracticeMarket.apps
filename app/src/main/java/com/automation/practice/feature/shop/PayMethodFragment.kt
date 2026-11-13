package com.automation.practice.feature.shop

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentPayMethodBinding
import com.automation.practice.domain.COD_LIMIT
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.digitsOnly
import com.automation.practice.domain.expiryError
import com.automation.practice.domain.formatCardNumber
import com.automation.practice.domain.isCvvValid
import com.automation.practice.domain.isLuhnValid
import com.automation.practice.domain.toRupiah
import kotlinx.coroutines.launch
import java.util.Calendar

private const val CARD_MIN_DIGITS_FOR_ERROR = 13
private const val EXPIRY_DIGITS = 4

/** SHOP-12 Metode Pembayaran: kartu, Virtual Account, PracticePay, COD. */
class PayMethodFragment : ShopFragment(R.layout.fragment_pay_method) {
    private var _b: FragmentPayMethodBinding? = null
    private val b get() = _b!!

    private val total get() = vm.quote().total

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentPayMethodBinding.bind(view)
        b.shopHeaderPay.applySystemInsets(top = true)
        b.shopBarPay.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }

        METHODS.forEach { (method, views) ->
            val (row, radio) = views(b)
            row.setOnClickListener { choose(method) }
            radio.setOnClickListener { choose(method) }
        }
        b.shopGroupBank.setOnCheckedStateChangeListener { _, ids ->
            vm.chosenBank = when (ids.firstOrNull()) {
                R.id.shop_chip_bank_bni -> "BNI"
                R.id.shop_chip_bank_mandiri -> "Mandiri"
                else -> "BCA"
            }
        }
        b.shopGroupBank.check(
            when (vm.chosenBank) { "BNI" -> R.id.shop_chip_bank_bni; "Mandiri" -> R.id.shop_chip_bank_mandiri; else -> R.id.shop_chip_bank_bca },
        )

        b.shopInputCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                val formatted = formatCardNumber(s.toString())
                if (formatted != s.toString()) { s.replace(0, s.length, formatted) }
                render()
            }
        })
        b.shopInputCardExp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                val d = s.toString().digitsOnly().take(EXPIRY_DIGITS)
                val formatted = if (d.length > 2) d.substring(0, 2) + "/" + d.substring(2) else d
                if (formatted != s.toString()) s.replace(0, s.length, formatted)
                render()
            }
        })
        b.shopInputCardCvv.doAfterTextChanged { render() }
        b.shopBtnPay.setOnClickListener { onPayClicked() }

        requireActivity().supportFragmentManager.setFragmentResultListener(PinSheetFragment.RESULT_KEY, viewLifecycleOwner) { _, r ->
            if (r.getBoolean(PinSheetFragment.RESULT_OK)) startPay()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.processing.collect { render() }
            }
        }
        render()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun choose(method: PayMethod) {
        val (row, _) = METHODS.getValue(method)(b)
        if (!row.isEnabled) return
        vm.chosenMethod = method
        render()
    }

    private fun cardNumber() = b.shopInputCardNumber.text.toString()

    private fun expiryProblem(): String? {
        val cal = Calendar.getInstance()
        return expiryError(b.shopInputCardExp.text.toString(), cal.get(Calendar.YEAR) % 100, cal.get(Calendar.MONTH) + 1)
    }

    private fun cardValid() = isLuhnValid(cardNumber()) && expiryProblem() == null && isCvvValid(b.shopInputCardCvv.text.toString())

    private fun render() {
        val binding = _b ?: return
        val total = total
        val balance = vm.session.balance.value
        val walletOk = balance >= total
        val codOk = total <= COD_LIMIT

        // Metode terpilih yang ternyata mati (saldo/COD) kembali ke kartu sebelum digambar.
        if (!walletOk && vm.chosenMethod == PayMethod.WALLET || !codOk && vm.chosenMethod == PayMethod.COD) vm.chosenMethod = PayMethod.CARD

        METHODS.forEach { (method, views) ->
            val (row, radio) = views(binding)
            val enabled = when (method) { PayMethod.WALLET -> walletOk; PayMethod.COD -> codOk; else -> true }
            val box = BOXES.getValue(method)(binding)
            box.isEnabled = enabled
            row.isEnabled = enabled
            radio.isEnabled = enabled
            box.isSelected = vm.chosenMethod == method && enabled
            radio.isChecked = vm.chosenMethod == method && enabled
        }

        binding.shopFormCard.isVisible = vm.chosenMethod == PayMethod.CARD
        binding.shopGroupBank.isVisible = vm.chosenMethod == PayMethod.VA
        binding.shopTxtPayWalletNote.apply {
            text = if (walletOk) getString(R.string.pay_wallet_balance_fmt, balance.toRupiah()) else getString(R.string.pay_wallet_insufficient)
            setTextColor(requireContext().getColor(if (walletOk) R.color.pm_ink_muted else R.color.pm_error))
        }

        val digits = cardNumber().digitsOnly()
        val numberBad = digits.length >= CARD_MIN_DIGITS_FOR_ERROR && !isLuhnValid(digits)
        showError(binding.shopInputCardNumber, binding.shopTxtCardNumberError, if (numberBad) getString(R.string.pay_card_number_error) else null)
        val expDigits = binding.shopInputCardExp.text.toString().digitsOnly()
        showError(binding.shopInputCardExp, binding.shopTxtCardExpError, if (expDigits.length == EXPIRY_DIGITS) expiryProblem() else null)

        binding.shopTxtPayTotal.text = total.toRupiah()
        binding.shopBtnPay.text = getString(R.string.pay_submit_fmt, total.toRupiah())
        val processing = vm.processing.value
        binding.shopOverlayProcessing.isVisible = processing
        binding.shopBtnPay.isEnabled = !processing && vm.checkoutLines().isNotEmpty() && when (vm.chosenMethod) {
            PayMethod.CARD -> cardValid()
            PayMethod.VA -> true
            PayMethod.WALLET -> walletOk
            PayMethod.COD -> codOk
        }
    }

    private fun showError(input: EditText, label: android.widget.TextView, message: String?) {
        label.isVisible = message != null
        label.text = message.orEmpty()
        input.isSelected = message != null
    }

    private fun onPayClicked() {
        // Bug glitch_user: tap pertama "Bayar" tidak melakukan apa-apa (PRD §8.8 no. 3).
        if (vm.glitch && !vm.session.glitchPayTapUsed) { vm.session.glitchPayTapUsed = true; return }
        if (vm.chosenMethod == PayMethod.WALLET) PinSheetFragment().show(requireActivity().supportFragmentManager, "pin") else startPay()
    }

    private fun startPay() {
        val digits = cardNumber().digitsOnly()
        val label = when (vm.chosenMethod) {
            PayMethod.CARD -> getString(R.string.pay_method_card_label_fmt, digits.takeLast(LAST_DIGITS))
            PayMethod.VA -> getString(R.string.pay_method_va_label_fmt, vm.chosenBank)
            PayMethod.WALLET -> getString(R.string.pay_method_wallet_label)
            PayMethod.COD -> getString(R.string.pay_method_cod_label)
        }
        vm.pay(vm.chosenMethod, label, digits, vm.chosenBank)
    }

    private companion object {
        const val LAST_DIGITS = 4

        val METHODS: Map<PayMethod, (FragmentPayMethodBinding) -> Pair<View, android.widget.RadioButton>> = mapOf(
            PayMethod.CARD to { b -> b.shopRowPayCard to b.shopRdoPayCard },
            PayMethod.VA to { b -> b.shopRowPayVa to b.shopRdoPayVa },
            PayMethod.WALLET to { b -> b.shopRowPayWallet to b.shopRdoPayWallet },
            PayMethod.COD to { b -> b.shopRowPayCod to b.shopRdoPayCod },
        )
        val BOXES: Map<PayMethod, (FragmentPayMethodBinding) -> View> = mapOf(
            PayMethod.CARD to { b -> b.shopBoxPayCard },
            PayMethod.VA to { b -> b.shopBoxPayVa },
            PayMethod.WALLET to { b -> b.shopBoxPayWallet },
            PayMethod.COD to { b -> b.shopBoxPayCod },
        )
    }
}
