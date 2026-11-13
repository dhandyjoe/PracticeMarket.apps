package com.automation.practice.feature.shop

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentAccountBinding
import com.automation.practice.databinding.ItemMenuRowBinding
import com.automation.practice.domain.OrderStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** ACC-01 Akun: profil, menu, dan Keluar dengan dialog konfirmasi (FR-ACC-01). */
class AccountFragment : ShopFragment(R.layout.fragment_account) {
    private var ordersTag: ItemMenuRowBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentAccountBinding.bind(view)
        b.shopHeaderAccount.applySystemInsets(top = true)
        val container = requireContext().app.container

        row(b.accListMenuMain, R.id.shop_item_orders, R.drawable.ic_bag, R.string.acc_orders) { nav.openOrders() }.also { ordersTag = it }
        row(b.accListMenuMain, R.id.shop_item_wishlist, R.drawable.ic_favorite, R.string.account_wishlist) { nav.openWishlist() }
        row(b.accListMenuMain, R.id.acc_item_address, R.drawable.ic_location, R.string.acc_address) { nav.openAddresses() }
        row(b.accListMenuMain, R.id.acc_item_pay, R.drawable.ic_wallet, R.string.nav_pay) { nav.selectTab(Tab.PAY) }
        row(b.accListMenuMore, R.id.acc_item_settings, R.drawable.ic_settings, R.string.acc_settings, R.string.acc_settings_tag) { nav.openSettings(dev = false) }
        row(b.accListMenuMore, R.id.acc_item_help, R.drawable.ic_help, R.string.acc_help) { nav.openHelp() }
        row(b.accListMenuMore, R.id.acc_item_dev, R.drawable.ic_code, R.string.acc_dev, R.string.acc_dev_tag) { nav.openSettings(dev = true) }

        b.accBtnEditProfile.setOnClickListener { nav.openProfile() }
        b.shopBtnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_body)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.home_logout) { _, _ -> nav.logout() }
                .show()
        }
        val pm = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        @Suppress("DEPRECATION")
        b.accTxtVersion.text = getString(R.string.acc_version_fmt, pm.versionName, pm.versionCode)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(vm.session.profile, vm.session.orders) { p, o -> p to o }.collect { (p, orders) ->
                    b.accTxtName.text = p.name
                    b.accTxtUsername.text = container.auth.currentUser?.username.orEmpty()
                    b.accTxtInitials.text = initials(p.name)
                    val photo = p.photoUri
                    b.accImgAvatar.isVisible = photo != null
                    b.accTxtInitials.isVisible = photo == null
                    if (photo != null) { b.accImgAvatar.clipToOutline = true; b.accImgAvatar.setImageURI(Uri.parse(photo)) }
                    val active = orders.count { it.status != OrderStatus.DONE && it.status != OrderStatus.CANCELLED }
                    ordersTag?.accTxtMenuTag?.apply { isVisible = active > 0; text = getString(R.string.acc_orders_active_fmt, active) }
                }
            }
        }
    }

    private fun row(parent: LinearLayout, @IdRes id: Int, @DrawableRes icon: Int, @StringRes label: Int, @StringRes tag: Int = 0, onClick: () -> Unit): ItemMenuRowBinding {
        val r = ItemMenuRowBinding.inflate(layoutInflater, parent, false)
        r.root.id = id
        r.accImgMenu.setImageResource(icon)
        r.accTxtMenu.setText(label)
        if (tag != 0) { r.accTxtMenuTag.setText(tag); r.accTxtMenuTag.isVisible = true }
        r.root.setOnClickListener { onClick() }
        parent.addView(r.root)
        return r
    }
}

/** Dua huruf pertama dari kata awal nama: "Standard User" → "SU". */
fun initials(name: String) = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
