package com.automation.practice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.automation.practice.core.ui.LocatorHints
import com.automation.practice.data.repo.Behavior
import com.automation.practice.databinding.ActivityMainBinding
import com.automation.practice.domain.merchantOf
import com.automation.practice.feature.auth.AuthActivity
import com.automation.practice.feature.shop.AccountFragment
import com.automation.practice.feature.shop.CartFragment
import com.automation.practice.feature.shop.CheckoutFragment
import com.automation.practice.feature.shop.AddressFormFragment
import com.automation.practice.feature.shop.AmountFragment
import com.automation.practice.feature.shop.ContactsFragment
import com.automation.practice.feature.shop.HistoryFragment
import com.automation.practice.feature.shop.QrConfirmFragment
import com.automation.practice.feature.shop.ScanQrFragment
import com.automation.practice.feature.shop.WalletFragment
import com.automation.practice.feature.shop.AddressListFragment
import com.automation.practice.feature.shop.CancelSheetFragment
import com.automation.practice.feature.shop.EditProfileFragment
import com.automation.practice.feature.shop.HelpFragment
import com.automation.practice.feature.shop.SettingsFragment
import com.automation.practice.feature.shop.OrderDetailFragment
import com.automation.practice.feature.shop.OrdersFragment
import com.automation.practice.feature.shop.PayMethodFragment
import com.automation.practice.feature.shop.PayStep
import com.automation.practice.feature.shop.ResultFragment
import com.automation.practice.feature.shop.ThreeDsActivity
import com.automation.practice.feature.shop.VaFragment
import com.automation.practice.feature.shop.VoucherSheetFragment
import com.automation.practice.feature.shop.FilterSheetFragment
import com.automation.practice.feature.shop.HomeFragment
import com.automation.practice.feature.shop.ImageViewerFragment
import com.automation.practice.feature.shop.PlaceholderFragment
import com.automation.practice.feature.shop.ProductFragment
import com.automation.practice.feature.shop.ResultsFragment
import com.automation.practice.feature.shop.SearchFragment
import com.automation.practice.feature.shop.ShopNavigator
import com.automation.practice.feature.shop.ShopViewModel
import com.automation.practice.feature.shop.Tab
import com.automation.practice.feature.shop.WishlistFragment
import kotlinx.coroutines.launch

/**
 * Launcher (`appActivity=.MainActivity`, PRD §14). Tanpa sesi login (belum login, Keluar, Reset), langsung ke Auth;
 * sesi tersimpan di database, jadi setelah kill app langsung kembali ke sini (PRD FR-AUTH-17).
 * Menjadi host bottom navigation dan seluruh layar Belanja.
 */
class MainActivity : AppCompatActivity(), ShopNavigator {
    private lateinit var b: ActivityMainBinding
    private val vm: ShopViewModel by viewModels()

    private val threeDs = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onThreeDsDone(it.resultCode == Activity.RESULT_OK)
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val tabIds = mapOf(
        Tab.HOME to R.id.shop_tab_home,
        Tab.CATEGORY to R.id.shop_tab_category,
        Tab.PAY to R.id.shop_tab_pay,
        Tab.CART to R.id.shop_tab_cart,
        Tab.ACCOUNT to R.id.shop_tab_account,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && handleSettingsHook(intent)) {
            finish()
            return
        }
        if (app.container.auth.currentUser == null) {
            // shortcut: deep link tanpa sesi login hilang; teruskan setelah login saat DeepLinkRouter dibangun (ARCHITECTURE.md §11).
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        app.container.auth.currentUser?.let { app.container.shop.applyUser(it.username, it.displayName, it.email, it.behavior == Behavior.NEW) }
        val hints = LocatorHints(b.root as ViewGroup)
        lifecycleScope.launch { app.container.settings.prefs.collect { hints.set(it.locatorHints) } }
        b.shopNavBottom.setOnItemSelectedListener { item ->
            tabIds.entries.firstOrNull { it.value == item.itemId }?.let { showTab(it.key) }
            true
        }
        supportFragmentManager.addOnBackStackChangedListener { updateChrome() }

        lifecycleScope.launch {
            vm.session.badge.collect { count ->
                if (count > 0) b.shopNavBottom.getOrCreateBadge(R.id.shop_tab_cart).apply {
                    number = count
                    backgroundColor = getColor(R.color.pm_error)
                    badgeTextColor = getColor(R.color.pm_on_primary)
                } else b.shopNavBottom.removeBadge(R.id.shop_tab_cart)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.steps.collect { step ->
                    when (step) {
                        is PayStep.ThreeDs -> openThreeDs(step.total, step.cardLast4)
                        PayStep.Va -> openVa()
                        PayStep.Result -> openResult()
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            showTab(Tab.HOME)
            route(intent)
            askNotificationPermission()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleSettingsHook(intent)) return
        if (::b.isInitialized) route(intent)
    }

    // shortcut: deep link ditangani di sini; pindah ke DeepLinkRouter tunggal saat milestone deep link (ARCHITECTURE.md §11).
    private fun route(intent: Intent) {
        val uri = intent.data?.takeIf { it.scheme == "practiceapp" } ?: return
        val path = uri.pathSegments
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        selectTab(Tab.HOME)
        when (uri.host) {
            "shop" -> routeShop(uri, path)
            "pay" -> routePay(uri, path)
            "account" -> when (path.firstOrNull()) {
                null -> selectTab(Tab.ACCOUNT)
                "address" -> { selectTab(Tab.ACCOUNT); openAddresses() }
                else -> notFound()
            }
            "settings" -> { selectTab(Tab.ACCOUNT); openSettings(dev = false) }
            "help" -> { selectTab(Tab.ACCOUNT); openHelp() }
            else -> notFound()
        }
    }

    private fun routeShop(uri: Uri, path: List<String>) {
        when (path.firstOrNull()) {
            null -> Unit
            "search" -> openSearch(uri.getQueryParameter("q").orEmpty())
            "product" -> path.getOrNull(1)?.takeIf { vm.catalog.product(it) != null }?.let(::openProduct) ?: notFound()
            "category" -> path.getOrNull(1)?.takeIf { slug -> vm.catalog.categories.any { it.slug == slug } }
                ?.let { vm.browse(it); openResults() } ?: notFound()
            "wishlist" -> openWishlist()
            "orders" -> openOrders()
            "order" -> path.getOrNull(1)?.takeIf { vm.session.order(it) != null }?.let(::openOrder) ?: notFound()
            "cart" -> selectTab(Tab.CART)
            else -> notFound()
        }
    }

    private fun routePay(uri: Uri, path: List<String>) {
        selectTab(Tab.PAY)
        when (path.firstOrNull()) {
            null, "topup" -> Unit
            "transfer" -> openContacts()
            "history" -> openHistory()
            "qr" -> {
                val merchant = merchantOf(uri.getQueryParameter("merchant"))
                if (merchant == null) notFound()
                else openQrConfirm(merchant.id, uri.getQueryParameter("amount")?.toLongOrNull() ?: 0L)
            }
            else -> notFound()
        }
    }

    /**
     * Test hook `practiceapp://settings/reset` dan `practiceapp://settings?deterministic=&latency=&animations=&flaky=` (PRD DEV-02):
     * ditangani tanpa UI dan tanpa perlu login. @return true bila [intent] adalah hook dan sudah diproses.
     */
    private fun handleSettingsHook(intent: Intent): Boolean {
        val uri = intent.data?.takeIf { it.scheme == "practiceapp" && it.host == "settings" } ?: return false
        if (uri.pathSegments.firstOrNull() == "reset") {
            app.container.resetAll()
            startActivity(Intent(this, AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            return true
        }
        val keys = listOf("deterministic", "latency", "animations", "flaky")
        if (keys.none { uri.getQueryParameter(it) != null }) return false
        app.container.settings.update { p ->
            p.copy(
                deterministic = uri.getQueryParameter("deterministic")?.toBooleanStrictOrNull() ?: p.deterministic,
                latency = uri.getQueryParameter("latency")?.toIntOrNull()?.coerceIn(0, 3) ?: p.latency,
                animations = uri.getQueryParameter("animations")?.toBooleanStrictOrNull() ?: p.animations,
                flaky = uri.getQueryParameter("flaky")?.toIntOrNull()?.takeIf { it in listOf(0, 30, 50) } ?: p.flaky,
            )
        }
        return true
    }

    // shortcut: toast sementara; ganti dengan SYS-01 "Halaman tidak ditemukan" saat didesain.
    private fun notFound() = Toast.makeText(this, R.string.not_found, Toast.LENGTH_SHORT).show()

    private fun showTab(tab: Tab) {
        supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.shop_container, tabFragment(tab))
        }
    }

    private fun tabFragment(tab: Tab): Fragment = when (tab) {
        Tab.HOME -> HomeFragment()
        Tab.CATEGORY -> PlaceholderFragment.newInstance(R.string.nav_category)
        Tab.PAY -> WalletFragment()
        Tab.CART -> CartFragment()
        Tab.ACCOUNT -> AccountFragment()
    }

    /** Bottom nav hanya tampil di layar tab; layar detail membukanya di atas dan kembali dengan Back. */
    private fun updateChrome() {
        b.shopNavBottom.isVisible = supportFragmentManager.backStackEntryCount == 0
    }

    private fun push(fragment: Fragment, name: String? = null) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.shop_container, fragment)
            addToBackStack(name)
        }
    }

    override fun selectTab(tab: Tab) {
        val id = tabIds.getValue(tab)
        if (b.shopNavBottom.selectedItemId == id) showTab(tab) else b.shopNavBottom.selectedItemId = id
    }

    override fun openSearch(prefill: String) = push(SearchFragment.newInstance(prefill))

    /** Dari Pencarian, tutup Pencarian dulu agar Back dari Hasil tidak kembali ke kolom ketik. */
    override fun openResults() {
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.shop_container) is SearchFragment) fm.popBackStackImmediate()
        if (fm.findFragmentById(R.id.shop_container) !is ResultsFragment) push(ResultsFragment())
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun openFilter() = FilterSheetFragment().show(supportFragmentManager, "filter")
    override fun openProduct(id: String) = push(ProductFragment.newInstance(id))
    override fun openImageViewer(id: String) = push(ImageViewerFragment.newInstance(id))
    override fun openWishlist() = push(WishlistFragment())
    override fun openVoucher() = VoucherSheetFragment().show(supportFragmentManager, "voucher")
    override fun openCheckout() = push(CheckoutFragment())
    override fun openPayMethod() {
        // "Coba Lagi" dari hasil gagal: buang layar hasil dulu agar Back tidak kembali ke sana.
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.shop_container) is ResultFragment) fm.popBackStackImmediate()
        push(PayMethodFragment())
    }
    override fun openVa() = push(VaFragment())
    override fun openThreeDs(total: Long, cardLast4: String) = threeDs.launch(ThreeDsActivity.intent(this, total, cardLast4))

    /** Pembayaran selesai: tutup Checkout/Metode/VA supaya Back dari hasil kembali ke tab, bukan ke form bayar. */
    override fun openResult() {
        val fm = supportFragmentManager
        if (fm.findFragmentById(R.id.shop_container) is ResultFragment) return
        // Tidak immediate: hasil bisa datang saat FragmentManager sedang menjalankan transaksi (VA kedaluwarsa saat layar dibuka).
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        push(ResultFragment())
    }
    /** Kembali ke Pesanan Saya bila sudah ada di tumpukan (mis. setelah batal), selain itu buka baru. */
    override fun openOrders() {
        if (!supportFragmentManager.popBackStackImmediate(ORDERS, 0)) push(OrdersFragment(), ORDERS)
    }
    override fun openOrder(id: String) = push(OrderDetailFragment.newInstance(id))
    override fun openCancel(id: String) = CancelSheetFragment.newInstance(id).show(supportFragmentManager, "cancel")
    override fun openProfile() = push(EditProfileFragment())
    override fun openAddresses() = push(AddressListFragment())
    override fun openAddressForm(id: Int) = push(AddressFormFragment.newInstance(id))
    override fun openSettings(dev: Boolean) = push(SettingsFragment.newInstance(dev))
    override fun openHelp() = push(HelpFragment())
    override fun openContacts() = push(ContactsFragment())
    override fun openAmount(name: String, phone: String) = push(AmountFragment.newInstance(name, phone))
    override fun openScanQr() = push(ScanQrFragment())
    override fun openQrConfirm(merchant: String, amount: Long) = push(QrConfirmFragment.newInstance(merchant, amount))
    override fun openHistory() = push(HistoryFragment())
    override fun back() = onBackPressedDispatcher.onBackPressed()

    private companion object {
        const val ORDERS = "orders"
    }

    override fun logout() {
        app.container.auth.logout()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
