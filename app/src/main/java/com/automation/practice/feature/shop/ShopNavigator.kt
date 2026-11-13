package com.automation.practice.feature.shop

/** Tab bottom navigation (PRD §5). */
enum class Tab { HOME, CATEGORY, PAY, CART, ACCOUNT }

/** Navigasi layar Belanja; diimplementasi MainActivity (pola sama dengan AuthNavigator). */
interface ShopNavigator {
    fun selectTab(tab: Tab)
    fun openSearch(prefill: String = "")
    fun openResults()
    fun openFilter()
    fun openProduct(id: String)
    fun openImageViewer(id: String)
    fun openWishlist()
    fun openVoucher()
    fun openCheckout()
    fun openPayMethod()
    fun openVa()
    fun openResult()
    fun openThreeDs(total: Long, cardLast4: String)
    fun openOrders()
    fun openOrder(id: String)
    fun openCancel(id: String)
    fun openProfile()
    fun openAddresses()
    fun openAddressForm(id: Int)
    fun openSettings(dev: Boolean)
    fun openHelp()
    fun openContacts()
    fun openAmount(name: String, phone: String)
    fun openScanQr()
    fun openQrConfirm(merchant: String, amount: Long)
    fun openHistory()
    fun back()
    fun logout()
}
