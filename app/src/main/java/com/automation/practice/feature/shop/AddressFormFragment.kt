package com.automation.practice.feature.shop

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentAddressFormBinding
import com.automation.practice.domain.Address
import com.automation.practice.domain.MOCK_CITY
import com.automation.practice.domain.MOCK_PROVINCE
import com.automation.practice.domain.POSTAL_LENGTH
import com.automation.practice.domain.REGIONS
import com.automation.practice.domain.isPostalValid
import com.google.android.material.snackbar.Snackbar

/** ACC-03 Form Alamat: lokasi saat ini, label, Provinsi→Kota bertingkat, kode pos 5 digit, alamat utama (FR-ACC-03/04). */
class AddressFormFragment : ShopFragment(R.layout.fragment_address_form) {
    private lateinit var b: FragmentAddressFormBinding
    private val editId get() = requireArguments().getInt(ARG_ID)
    private val provinces = REGIONS.keys.toList()

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useLocation() else b.addrTxtLocation.setText(R.string.addr_location_denied)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentAddressFormBinding.bind(view)
        b.shopTopbar.setup(if (editId == 0) R.string.addr_title_add else R.string.addr_title_edit)
        // Keyboard sengaja menutupi tombol Simpan (FR-ACC-04): bar hanya memakai inset nav bar; isi form tetap bisa digulir.
        b.addrBarSave.applySystemInsets(bottom = true, ime = false)
        b.addrScroll.applySystemInsets(bottom = true)

        b.addrSpnProvince.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, provinces)
        b.addrSpnProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = setCities(provinces[pos], keep = b.addrSpnCity.selectedItem as? String)
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        val existing = vm.session.addresses.value.firstOrNull { it.id == editId }
        val user = vm.session.profile.value
        if (existing != null) fill(existing) else {
            b.addrGroupLabel.check(R.id.addr_chip_home)
            b.addrInputName.setText(user.name)
            setProvince(provinces.first(), null)
        }

        listOf(b.addrInputName, b.addrInputPhone, b.addrInputStreet, b.addrInputPostal).forEach { it.doAfterTextChanged { validate() } }
        b.addrBtnUseLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLocation()
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        b.addrBtnSave.setOnClickListener {
            if (!validate()) return@setOnClickListener
            vm.session.saveAddress(
                Address(
                    editId, label(), b.addrInputName.text.toString().trim(), b.addrInputPhone.text.toString().trim(),
                    b.addrSpnProvince.selectedItem as String, b.addrSpnCity.selectedItem as String,
                    b.addrInputStreet.text.toString().trim(), b.addrInputPostal.text.toString(), b.addrSwPrimary.isChecked,
                ),
            )
            Snackbar.make(view, R.string.addr_saved, Snackbar.LENGTH_SHORT).show()
            nav.back()
        }
        validate()
    }

    private fun fill(a: Address) {
        b.addrGroupLabel.check(when (a.label) { getString(R.string.addr_label_office) -> R.id.addr_chip_office; getString(R.string.addr_label_other) -> R.id.addr_chip_other; else -> R.id.addr_chip_home })
        b.addrInputName.setText(a.recipient)
        b.addrInputPhone.setText(a.phone)
        b.addrInputStreet.setText(a.street)
        b.addrInputPostal.setText(a.postal)
        b.addrSwPrimary.isChecked = a.primary
        setProvince(a.province, a.city)
    }

    private fun label() = b.addrGroupLabel.checkedChipId.let {
        getString(when (it) { R.id.addr_chip_office -> R.string.addr_label_office; R.id.addr_chip_other -> R.string.addr_label_other; else -> R.string.addr_label_home })
    }

    private fun setProvince(province: String, city: String?) {
        b.addrSpnProvince.setSelection(provinces.indexOf(province).coerceAtLeast(0))
        setCities(province, city)
    }

    private fun setCities(province: String, keep: String?) {
        val cities = REGIONS.getValue(province)
        b.addrSpnCity.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, cities)
        b.addrSpnCity.setSelection(cities.indexOf(keep).coerceAtLeast(0))
    }

    /** Simpan aktif bila semua isian terisi dan kode pos tepat 5 digit; pesan galat muncul setelah mulai diketik. */
    private fun validate(): Boolean {
        val postal = b.addrInputPostal.text.toString()
        val postalBad = postal.isNotEmpty() && !isPostalValid(postal)
        b.addrErrorPostal.isVisible = postalBad
        b.addrInputPostal.isActivated = postalBad
        val ok = b.addrInputName.text.isNotBlank() && b.addrInputPhone.text.isNotBlank() &&
            b.addrInputStreet.text.isNotBlank() && postal.length == POSTAL_LENGTH && !postalBad
        b.addrBtnSave.isEnabled = ok
        return ok
    }

    /** Provinsi/kota dari titik lokasi (mock location di emulator → DKI Jakarta, Jakarta Pusat). */
    @SuppressLint("MissingPermission")
    private fun useLocation() {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc: Location? = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (loc == null) return b.addrTxtLocation.setText(R.string.addr_location_none)
        val (province, city) = nearest(loc)
        b.addrTxtLocation.text = getString(R.string.addr_location_fmt, loc.latitude, loc.longitude, city)
        setProvince(province, city)
    }

    private fun nearest(loc: Location): Pair<String, String> {
        val best = CITY_POINTS.minByOrNull { p ->
            val r = FloatArray(1); Location.distanceBetween(loc.latitude, loc.longitude, p.lat, p.lon, r); r[0]
        }
        return best?.let { it.province to it.city } ?: (MOCK_PROVINCE to MOCK_CITY)
    }

    companion object {
        private const val ARG_ID = "id"
        fun newInstance(id: Int) = AddressFormFragment().apply { arguments = bundleOf(ARG_ID to id) }

        private data class CityPoint(val province: String, val city: String, val lat: Double, val lon: Double)

        /** Titik pusat kota contoh untuk memetakan koordinat ke pilihan dropdown. */
        private val CITY_POINTS = listOf(
            CityPoint("DKI Jakarta", "Jakarta Pusat", -6.2088, 106.8228),
            CityPoint("DKI Jakarta", "Jakarta Selatan", -6.2615, 106.8106),
            CityPoint("Jawa Barat", "Bandung", -6.9175, 107.6191),
            CityPoint("Jawa Barat", "Bogor", -6.5971, 106.8060),
            CityPoint("Jawa Tengah", "Semarang", -6.9667, 110.4167),
            CityPoint("Jawa Timur", "Surabaya", -7.2575, 112.7521),
            CityPoint("Bali", "Denpasar", -8.6705, 115.2126),
        )
    }
}
