package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentCheckoutBinding
import com.automation.practice.databinding.ItemCheckoutProductBinding
import com.automation.practice.databinding.ItemSummaryLineBinding
import com.automation.practice.domain.Courier
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** SHOP-11 Checkout: alamat, kurir, asuransi, catatan, ringkasan harga real-time. */
class CheckoutFragment : ShopFragment(R.layout.fragment_checkout) {
    private var _b: FragmentCheckoutBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentCheckoutBinding.bind(view)
        b.shopHeaderCheckout.applySystemInsets(top = true)
        b.shopBarCheckout.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }
        b.shopBtnChangeAddress.setOnClickListener {
            // shortcut: satu alamat tetap; ubah alamat menunggu ACC-03 Form Alamat.
            Snackbar.make(view, R.string.checkout_address_soon, Snackbar.LENGTH_SHORT).show()
        }

        COURIERS.forEach { (courier, views) ->
            val (row, radio) = views(b)
            val select = { if (row.isEnabled) vm.updateCheckout { it.copy(courier = courier) } }
            row.setOnClickListener { select() }
            radio.setOnClickListener { select() }
        }
        // Same Day hanya DKI Jakarta; alamat tetap di Bandung, jadi selalu mati (FR-PAYM-03).
        b.shopRowCourierSameday.isEnabled = false
        b.shopRdoCourierSameday.isEnabled = false

        b.shopSwInsurance.setOnCheckedChangeListener { _, on -> vm.updateCheckout { it.copy(insured = on) } }
        b.shopInputNote.setText(vm.checkout.value.note)
        b.shopInputNote.doAfterTextChanged { text -> vm.updateCheckout { it.copy(note = text.toString()) } }
        b.shopBtnPickPayment.setOnClickListener { nav.openPayMethod() }

        b.shopTxtCourierRegulerPrice.text = Courier.REGULER.fee.toRupiah()
        b.shopTxtCourierExpressPrice.text = Courier.EXPRESS.fee.toRupiah()
        b.shopTxtCourierSamedayPrice.text = Courier.SAME_DAY.fee.toRupiah()
        renderItems()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.checkout.collect { render() }
            }
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun renderItems() {
        val lines = vm.checkoutLines()
        b.shopListCheckoutItems.removeAllViews()
        lines.forEach { line ->
            val p = vm.catalog.product(line.productId) ?: return@forEach
            val row = ItemCheckoutProductBinding.inflate(layoutInflater, b.shopListCheckoutItems, false)
            vm.visual(p).applyTo(row.shopImgCheckoutBg, row.shopImgCheckout)
            row.shopTxtCheckoutName.text = p.name
            val variant = variantLabel(requireContext(), line)
            val unit = p.price.toRupiah()
            row.shopTxtCheckoutDetail.text = getString(R.string.checkout_item_fmt, variant, line.qty, unit).removePrefix(" · ")
            row.shopTxtCheckoutLineTotal.text = vm.lineTotal(line).toRupiah()
            b.shopListCheckoutItems.addView(row.root)
        }
        b.shopBtnPickPayment.isEnabled = lines.isNotEmpty()
    }

    private fun render() {
        val d = vm.checkout.value
        COURIERS.forEach { (courier, views) ->
            val (row, radio) = views(b)
            row.isSelected = d.courier == courier
            radio.isChecked = d.courier == courier
        }
        if (b.shopSwInsurance.isChecked != d.insured) b.shopSwInsurance.isChecked = d.insured

        val q = vm.quote()
        b.shopListSummary.removeAllViews()
        fun line(key: String, value: String, positive: Boolean = false) {
            val row = ItemSummaryLineBinding.inflate(layoutInflater, b.shopListSummary, false)
            row.shopTxtSummaryKey.text = key
            row.shopTxtSummaryValue.text = value
            if (positive) row.shopTxtSummaryValue.setTextColor(requireContext().getColor(R.color.pm_primary_pressed))
            b.shopListSummary.addView(row.root)
        }
        line(getString(R.string.checkout_subtotal), q.subtotal.toRupiah())
        if (q.discount > 0) line(getString(R.string.checkout_discount_fmt, q.voucherCode), "−" + q.discount.toRupiah(), positive = true)
        line(getString(R.string.checkout_shipping), q.shipping.toRupiah())
        line(getString(R.string.checkout_insurance_line), q.insurance.toRupiah())
        line(getString(R.string.checkout_service), q.service.toRupiah())
        b.shopTxtCheckoutTotal.text = q.total.toRupiah()
        b.shopBtnPickPayment.text = getString(R.string.checkout_pick_payment_fmt, q.total.toRupiah())
    }

    private companion object {
        val COURIERS: Map<Courier, (FragmentCheckoutBinding) -> Pair<View, android.widget.RadioButton>> = mapOf(
            Courier.REGULER to { b -> b.shopRowCourierReguler to b.shopRdoCourierReguler },
            Courier.EXPRESS to { b -> b.shopRowCourierExpress to b.shopRdoCourierExpress },
            Courier.SAME_DAY to { b -> b.shopRowCourierSameday to b.shopRdoCourierSameday },
        )
    }
}
