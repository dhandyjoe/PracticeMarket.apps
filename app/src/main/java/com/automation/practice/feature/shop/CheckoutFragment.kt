package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
import com.automation.practice.domain.courierAvailable
import com.automation.practice.domain.isDeliveryTimeValid
import com.automation.practice.domain.toRupiah
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
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
        b.shopBtnChangeAddress.setOnClickListener { pickAddress() }
        b.shopBtnDeliveryTime.setOnClickListener { pickDeliveryTime() }

        COURIERS.forEach { (courier, views) ->
            val (row, radio) = views(b)
            val select = { if (row.isEnabled) vm.updateCheckout { it.copy(courier = courier) } }
            row.setOnClickListener { select() }
            radio.setOnClickListener { select() }
        }

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
    }

    /** Pilih satu dari alamat akun; Same Day jatuh ke Express bila alamat baru bukan DKI Jakarta. */
    private fun pickAddress() {
        val list = vm.session.addresses.value
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.checkout_address_pick_title)
            .setSingleChoiceItems(
                list.map { "${it.label}\n" + getString(R.string.addr_full_fmt, it.street, it.city, it.province, it.postal) }.toTypedArray(),
                list.indexOfFirst { it.id == vm.checkoutAddress()?.id },
            ) { dialog, which ->
                val a = list[which]
                vm.updateCheckout { it.copy(addressId = a.id, courier = if (courierAvailable(it.courier, a.province)) it.courier else Courier.EXPRESS) }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /** Jam kirim Same Day; rentang 09:00-17:00 dicek di [isDeliveryTimeValid], bukan oleh picker. */
    private fun pickDeliveryTime() {
        val current = vm.checkout.value.deliveryTime.takeIf { isDeliveryTimeValid(it) }
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setTitleText(R.string.checkout_delivery_time)
            .setHour(current?.take(2)?.toInt() ?: DEFAULT_HOUR)
            .setMinute(current?.takeLast(2)?.toInt() ?: 0)
            .build()
        picker.addOnPositiveButtonClickListener { vm.updateCheckout { it.copy(deliveryTime = "%02d:%02d".format(picker.hour, picker.minute)) } }
        picker.show(childFragmentManager, "delivery_time")
    }

    private fun render() {
        val d = vm.checkout.value
        val address = vm.checkoutAddress()
        val courier = vm.checkoutCourier()
        val province = address?.province.orEmpty()
        b.shopTxtAddressLabel.isVisible = address != null
        b.shopTagAddressMain.isVisible = address?.primary == true
        b.shopTxtAddressLabel.text = address?.label.orEmpty()
        b.shopTxtAddress.text = address?.let { getString(R.string.addr_full_fmt, it.street, it.city, it.province, it.postal) } ?: getString(R.string.checkout_address_none)
        b.shopBtnChangeAddress.isVisible = vm.session.addresses.value.size > 1

        COURIERS.forEach { (c, views) ->
            val (row, radio) = views(b)
            val available = courierAvailable(c, province)
            row.isEnabled = available
            radio.isEnabled = available
            row.isSelected = courier == c
            radio.isChecked = courier == c
        }
        b.shopTxtCourierSamedayNote.setText(if (courierAvailable(Courier.SAME_DAY, province)) R.string.courier_sameday_available else R.string.courier_sameday_note)
        val sameDay = courier == Courier.SAME_DAY
        val timeOk = !sameDay || isDeliveryTimeValid(d.deliveryTime)
        b.shopGroupDeliveryTime.isVisible = sameDay
        b.shopTxtDeliveryTimeError.isVisible = !timeOk
        b.shopBtnDeliveryTime.text = d.deliveryTime.ifEmpty { getString(R.string.checkout_delivery_time_pick) }
        b.shopBtnPickPayment.isEnabled = vm.checkoutLines().isNotEmpty() && address != null && timeOk
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
        const val DEFAULT_HOUR = 12
        val COURIERS: Map<Courier, (FragmentCheckoutBinding) -> Pair<View, android.widget.RadioButton>> = mapOf(
            Courier.REGULER to { b -> b.shopRowCourierReguler to b.shopRdoCourierReguler },
            Courier.EXPRESS to { b -> b.shopRowCourierExpress to b.shopRdoCourierExpress },
            Courier.SAME_DAY to { b -> b.shopRowCourierSameday to b.shopRdoCourierSameday },
        )
    }
}
