package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentAddressListBinding
import com.automation.practice.databinding.ItemAddressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** ACC-03 Daftar Alamat: tambah, ubah, hapus; satu alamat utama (FR-ACC-03). */
class AddressListFragment : ShopFragment(R.layout.fragment_address_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentAddressListBinding.bind(view)
        b.shopTopbar.setup(R.string.acc_address)
        b.addrBarAdd.applySystemInsets(bottom = true)
        b.addrBtnAdd.setOnClickListener { nav.openAddressForm(0) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.addresses.collect { list ->
                    b.addrList.removeAllViews()
                    b.addrTxtEmpty.isVisible = list.isEmpty()
                    list.forEach { a ->
                        val r = ItemAddressBinding.inflate(layoutInflater, b.addrList, false)
                        r.addrTxtLabel.text = a.label
                        r.addrBadgePrimary.isVisible = a.primary
                        r.addrTxtRecipient.text = getString(R.string.addr_recipient_fmt, a.recipient, a.phone)
                        r.addrTxtFull.text = getString(R.string.addr_full_fmt, a.street, a.city, a.province, a.postal)
                        r.addrBtnEdit.setOnClickListener { nav.openAddressForm(a.id) }
                        r.addrBtnDelete.setOnClickListener {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.addr_delete_title)
                                .setMessage(getString(R.string.addr_delete_body_fmt, a.label))
                                .setNegativeButton(R.string.dialog_cancel, null)
                                .setPositiveButton(R.string.addr_delete) { _, _ -> vm.session.deleteAddress(a.id) }
                                .show()
                        }
                        b.addrList.addView(r.root)
                    }
                }
            }
        }
    }
}
