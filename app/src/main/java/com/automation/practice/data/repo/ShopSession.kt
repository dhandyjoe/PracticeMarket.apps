package com.automation.practice.data.repo

import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.MAX_QTY
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.automation.practice.domain.SEED_ADDRESSES
import com.automation.practice.domain.Tx
import com.automation.practice.domain.cashback
import com.automation.practice.domain.jakartaTime
import com.automation.practice.domain.orderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

const val INITIAL_BALANCE = 500_000L

/**
 * Data sesi belanja di memori: bertahan lintas logout–login, kembali ke default saat app di-kill (PRD §6.3 no. 6).
 * [stockOf] memberi stok varian untuk batas jumlah per baris keranjang.
 * shortcut: satu sesi untuk semua akun, belum per-user; pindah ke WishlistEntity/CartItemEntity (username) bersama Room.
 */
class ShopSession(
    private val stockOf: (CartLine) -> Int,
    /** Jam aplikasi; mode deterministik memberi tanggal tetap 2026-01-15 (PRD §10). */
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _wishlist = MutableStateFlow(listOf("P001", "P010", "P002", "P020"))
    val wishlist: StateFlow<List<String>> = _wishlist.asStateFlow()

    private val _history = MutableStateFlow(listOf("sepatu lari", "headphone", "buku catatan", "smartwatch"))
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _cart = MutableStateFlow(emptyList<CartLine>())
    val cart: StateFlow<List<CartLine>> = _cart.asStateFlow()

    private val _badge = MutableStateFlow(0)

    /** Angka di badge tab Keranjang; terpisah dari [cart] supaya bug `glitch_user` (tidak turun setelah hapus) bisa ditiru. */
    val badge: StateFlow<Int> = _badge.asStateFlow()

    private val _voucher = MutableStateFlow<String?>(null)
    val voucher: StateFlow<String?> = _voucher.asStateFlow()

    private val _balance = MutableStateFlow(INITIAL_BALANCE)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    private val _orders = MutableStateFlow(emptyList<Order>())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _homeBalance = MutableStateFlow(INITIAL_BALANCE)

    /** Saldo yang tampil di Beranda Belanja; sama dengan [balance] kecuali bug `glitch_user` #6 (PRD §8.8). */
    val homeBalance: StateFlow<Long> = _homeBalance.asStateFlow()

    private val _transactions = MutableStateFlow(seedTransactions())
    val transactions: StateFlow<List<Tx>> = _transactions.asStateFlow()
    private var txSequence = _transactions.value.size

    private val _profile = MutableStateFlow(Profile("", ""))
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _addresses = MutableStateFlow(SEED_ADDRESSES)
    val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    private var userKey: String? = null
    private var isNewUser = false

    /** Kunci PIN PracticePay setelah 3× salah (PRD FR-PAY-06). */
    var pinLockedUntil = 0L
    var pinFailures = 0

    /** Bug `glitch_user`: tap pertama "Bayar" tidak bereaksi; nilai ini menandai sudah dipakai. */
    var glitchPayTapUsed = false

    private var orderSequence = 0

    /** Flash sale berakhir 2 jam 14 menit 37 detik sejak sesi dimulai. */
    val flashEndsAt = System.currentTimeMillis() + FLASH_DURATION_MS

    // --- wishlist & riwayat ---

    fun isWished(id: String) = id in _wishlist.value

    /** @return true bila produk sekarang ada di wishlist. */
    fun toggleWishlist(id: String): Boolean {
        val wished = isWished(id)
        _wishlist.update { if (wished) it - id else it + id }
        return !wished
    }

    fun setWishlistOrder(order: List<String>) { _wishlist.value = order }

    fun addHistory(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        _history.update { (listOf(t) + it.filterNot { h -> h.equals(t, ignoreCase = true) }).take(MAX_HISTORY) }
    }

    fun removeHistory(q: String) = _history.update { it - q }
    fun clearHistory() { _history.value = emptyList() }

    // --- keranjang ---

    /** Batas jumlah baris: min(stok varian, 10), minimal 1 (FR-SHOP-09). */
    fun maxQty(line: CartLine) = minOf(stockOf(line), MAX_QTY).coerceAtLeast(1)

    /** Gabungkan baris dengan varian sama (FR-SHOP-16); jumlah dibatasi [maxQty]. */
    fun addToCart(line: CartLine) {
        _cart.update { lines ->
            val same = lines.firstOrNull { it.sameItem(line) }
            if (same == null) lines + line
            else lines.map { if (it === same) it.copy(qty = (it.qty + line.qty).coerceAtMost(maxQty(it)), saved = false) else it }
        }
        syncBadge()
    }

    fun setQty(line: CartLine, qty: Int) {
        update(line) { it.copy(qty = qty.coerceIn(1, maxQty(it))) }
        syncBadge()
    }

    fun setSelected(line: CartLine, selected: Boolean) = update(line) { it.copy(selected = selected) }

    fun setAllSelected(selected: Boolean) =
        _cart.update { lines -> lines.map { if (it.saved) it else it.copy(selected = selected) } }

    /** "Simpan untuk nanti": baris tetap di daftar, tapi tidak dipilih dan tidak dihitung. */
    fun toggleSaved(line: CartLine) {
        update(line) { it.copy(saved = !it.saved, selected = false) }
        syncBadge()
    }

    /** @param keepBadge true meniru bug `glitch_user`: badge tidak ikut turun. */
    fun remove(line: CartLine, keepBadge: Boolean = false) {
        _cart.update { lines -> lines.filterNot { it.sameItem(line) } }
        if (!keepBadge) syncBadge()
    }

    /** Batalkan hapus: kembalikan baris di posisi semula. */
    fun restore(line: CartLine, index: Int) {
        _cart.update { lines -> lines.toMutableList().also { it.add(index.coerceIn(0, it.size), line) } }
        syncBadge()
    }

    private fun update(line: CartLine, f: (CartLine) -> CartLine) =
        _cart.update { lines -> lines.map { if (it.sameItem(line)) f(it) else it } }

    private fun syncBadge() { _badge.value = _cart.value.filterNot { it.saved }.sumOf { it.qty } }

    // --- voucher ---

    fun setVoucher(code: String?) { _voucher.value = code }

    // --- pesanan & saldo ---

    /** Buat pesanan baru dengan ID berikutnya. Pemanggil bertanggung jawab atas stok, saldo, dan keranjang. */
    fun newOrder(build: (id: String) -> Order): Order = build(orderId(++orderSequence)).let { o -> o.copy(statusAt = now(), stamps = mapOf(o.status to now())) }.also { o -> _orders.update { listOf(o) + it } }

    fun updateOrder(id: String, status: OrderStatus, reason: String = "") =
        _orders.update { list -> list.map { if (it.id == id) it.copy(status = status, statusAt = now(), stamps = it.stamps + (status to now()), cancelReason = reason) else it } }

    fun setVaDeadline(id: String, deadline: Long) =
        _orders.update { list -> list.map { if (it.id == id) it.copy(vaDeadline = deadline) else it } }

    fun order(id: String) = _orders.value.firstOrNull { it.id == id }

    /**
     * Majukan status satu langkah (simulator atau tombol manual). Pesanan PracticePay yang Selesai
     * memberi cashback ke saldo tanpa mengubah total pesanan (PRD §8.7).
     * @return status baru, atau null bila tidak bisa maju.
     */
    fun advance(id: String): OrderStatus? {
        val order = order(id) ?: return null
        val next = order.status.next() ?: return null
        updateOrder(id, next)
        if (next == OrderStatus.DONE && order.method == PayMethod.WALLET) credit(cashback(order.total), "Cashback belanja")
        return next
    }

    /** Kurangi saldo PracticePay dan catat transaksi keluar; false bila saldo kurang. */
    fun debit(amount: Long, title: String = "Belanja PracticeMart", syncHome: Boolean = true): Boolean {
        if (amount > _balance.value) return false
        _balance.update { it - amount }
        if (syncHome) _homeBalance.value = _balance.value
        record(title, -amount)
        return true
    }

    fun credit(amount: Long, title: String) {
        if (amount <= 0) return
        _balance.update { it + amount }
        _homeBalance.value = _balance.value
        record(title, amount)
    }

    private fun record(title: String, amount: Long) {
        _transactions.update { listOf(Tx(++txSequence, title, amount, now())) + it }
    }

    // --- akun ---

    fun updateProfile(p: Profile) { _profile.value = p }

    /** Simpan alamat baru atau perbarui yang ada; hanya satu alamat utama. */
    fun saveAddress(a: Address) {
        _addresses.update { list ->
            val withId = if (a.id == 0) a.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1) else a
            val others = list.filterNot { it.id == withId.id }.map { if (withId.primary) it.copy(primary = false) else it }
            (others + withId).let { all -> if (all.none { it.primary }) all.mapIndexed { i, x -> if (i == 0) x.copy(primary = true) else x } else all }
                .sortedBy { it.id }
        }
    }

    fun deleteAddress(id: Int) = _addresses.update { list -> list.filterNot { it.id == id } }

    /**
     * Siapkan data pribadi saat akun berganti. `new_user`: saldo Rp0, tanpa alamat dan riwayat (PRD §11.1, §11.4).
     * shortcut: data belanja lain (keranjang, wishlist) tetap dibagi semua akun; pecah per user bersama Room.
     */
    fun applyUser(username: String, displayName: String, email: String, isNew: Boolean) {
        if (userKey == username) return
        userKey = username
        isNewUser = isNew
        loadUserData(displayName, email)
    }

    private fun loadUserData(displayName: String, email: String) {
        _profile.value = Profile(displayName, email)
        _addresses.value = if (isNewUser) emptyList() else SEED_ADDRESSES.map { it.copy(recipient = displayName) }
        _balance.value = if (isNewUser) 0 else INITIAL_BALANCE
        _homeBalance.value = _balance.value
        _transactions.value = if (isNewUser) emptyList() else seedTransactions()
        txSequence = _transactions.value.size
    }

    /** "Reset semua data": data sesi kembali ke default tanpa kill (PRD §10). [onReset] memulihkan stok katalog. */
    fun reset(onReset: () -> Unit) {
        _wishlist.value = listOf("P001", "P010", "P002", "P020")
        _history.value = listOf("sepatu lari", "headphone", "buku catatan", "smartwatch")
        _cart.value = emptyList()
        _badge.value = 0
        _voucher.value = null
        _orders.value = emptyList()
        orderSequence = 0
        pinFailures = 0
        pinLockedUntil = 0
        glitchPayTapUsed = false
        onReset()
        val p = _profile.value
        loadUserData(p.name, p.email)
    }

    /** Buang baris keranjang yang sudah dibeli. */
    fun removePurchased(purchased: List<CartLine>) {
        _cart.update { lines -> lines.filterNot { l -> purchased.any { it.sameItem(l) } } }
        syncBadge()
    }

    private companion object {
        const val MAX_HISTORY = 5

        /** 8 transaksi awal deterministik, 12–15 Januari 2026 (PRD §11.4). Terbaru di atas. */
        fun seedTransactions(): List<Tx> {
            fun t(d: Int, h: Int, m: Int) = jakartaTime(2026, 1, d, h, m)
            return listOf(
                Tx(8, "Top Up dari BCA", 300_000, t(15, 9, 12)),
                Tx(7, "Belanja PracticeMart", -86_000, t(15, 8, 47)),
                Tx(6, "Transfer ke Budi Santoso", -150_000, t(14, 19, 30)),
                Tx(5, "Cashback belanja", 4_300, t(14, 12, 5)),
                Tx(4, "Bayar QR · Kopi Pagi", -28_000, t(14, 7, 58)),
                Tx(3, "Terima dari Citra Dewi", 75_000, t(13, 20, 14)),
                Tx(2, "Belanja PracticeMart", -158_000, t(13, 10, 2)),
                Tx(1, "Top Up dari Mandiri", 500_000, t(12, 18, 40)),
            )
        }
        const val FLASH_DURATION_MS = ((2 * 60 + 14) * 60 + 37) * 1000L
    }
}
