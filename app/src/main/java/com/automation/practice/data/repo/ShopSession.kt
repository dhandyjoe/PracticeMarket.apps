package com.automation.practice.data.repo

import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Hashing
import com.automation.practice.domain.MAX_QTY
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PAY_PIN
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

/** Hasil cek PIN PracticePay (PRD FR-PAY-06). */
sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val attemptsLeft: Int) : PinCheck
    data class Locked(val secondsLeft: Int) : PinCheck
}

/**
 * Data belanja & PracticePay akun yang sedang login. StateFlow di sini adalah cache; setiap mutasi langsung
 * ditulis ke [store] sehingga data bertahan setelah kill dan update APK (PRD §6.3 no. 6, ARCHITECTURE.md D15).
 * [stockOf] memberi stok varian untuk batas jumlah per baris keranjang.
 */
class ShopSession(
    private val stockOf: (CartLine) -> Int,
    /** Jam aplikasi; mode deterministik memberi tanggal tetap 2026-01-15 (PRD §10). */
    private val now: () -> Long = System::currentTimeMillis,
    private val store: ShopStore = MemoryShopStore(),
) {
    private val _wishlist = MutableStateFlow(SEED_WISHLIST)
    val wishlist: StateFlow<List<String>> = _wishlist.asStateFlow()

    private val _history = MutableStateFlow(SEED_HISTORY)
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

    private var pinHash = ""
    private var pinFailures = 0
    private var pinLockedUntil = 0L

    /** Bug `glitch_user`: tap pertama "Bayar" tidak bereaksi; nilai ini menandai sudah dipakai (tidak disimpan). */
    var glitchPayTapUsed = false

    /** Flash sale berakhir 2 jam 14 menit 37 detik sejak sesi dimulai. */
    val flashEndsAt = System.currentTimeMillis() + FLASH_DURATION_MS

    // --- wishlist & riwayat ---

    fun isWished(id: String) = id in _wishlist.value

    /** @return true bila produk sekarang ada di wishlist. */
    fun toggleWishlist(id: String): Boolean {
        val wished = isWished(id)
        _wishlist.update { if (wished) it - id else it + id }
        save()
        return !wished
    }

    fun setWishlistOrder(order: List<String>) { _wishlist.value = order; save() }

    fun addHistory(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        _history.update { (listOf(t) + it.filterNot { h -> h.equals(t, ignoreCase = true) }).take(MAX_HISTORY) }
        save()
    }

    fun removeHistory(q: String) { _history.update { it - q }; save() }
    fun clearHistory() { _history.value = emptyList(); save() }

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

    fun setSelected(line: CartLine, selected: Boolean) { update(line) { it.copy(selected = selected) }; save() }

    fun setAllSelected(selected: Boolean) {
        _cart.update { lines -> lines.map { if (it.saved) it else it.copy(selected = selected) } }
        save()
    }

    /** "Simpan untuk nanti": baris tetap di daftar, tapi tidak dipilih dan tidak dihitung. */
    fun toggleSaved(line: CartLine) {
        update(line) { it.copy(saved = !it.saved, selected = false) }
        syncBadge()
    }

    /** @param keepBadge true meniru bug `glitch_user`: badge tidak ikut turun. */
    fun remove(line: CartLine, keepBadge: Boolean = false) {
        _cart.update { lines -> lines.filterNot { it.sameItem(line) } }
        if (!keepBadge) syncBadge() else save()
    }

    /** Batalkan hapus: kembalikan baris di posisi semula. */
    fun restore(line: CartLine, index: Int) {
        _cart.update { lines -> lines.toMutableList().also { it.add(index.coerceIn(0, it.size), line) } }
        syncBadge()
    }

    private fun update(line: CartLine, f: (CartLine) -> CartLine) =
        _cart.update { lines -> lines.map { if (it.sameItem(line)) f(it) else it } }

    /** Hitung ulang badge lalu simpan keranjang; semua mutasi keranjang lewat sini. */
    private fun syncBadge() {
        _badge.value = _cart.value.filterNot { it.saved }.sumOf { it.qty }
        save()
    }

    // --- voucher ---

    fun setVoucher(code: String?) { _voucher.value = code; save() }

    // --- pesanan & saldo ---

    /** Buat pesanan baru dengan ID berikutnya. Pemanggil bertanggung jawab atas stok, saldo, dan keranjang. */
    fun newOrder(build: (id: String) -> Order): Order = build(orderId(store.nextOrderSequence())).let { o -> o.copy(statusAt = now(), stamps = mapOf(o.status to now())) }
        .also { o -> _orders.update { listOf(o) + it }; save() }

    fun updateOrder(id: String, status: OrderStatus, reason: String = "") {
        _orders.update { list -> list.map { if (it.id == id) it.copy(status = status, statusAt = now(), stamps = it.stamps + (status to now()), cancelReason = reason) else it } }
        save()
    }

    fun setVaDeadline(id: String, deadline: Long) {
        _orders.update { list -> list.map { if (it.id == id) it.copy(vaDeadline = deadline) else it } }
        save()
    }

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
        save()
    }

    // --- PIN PracticePay ---

    /** Sisa detik kunci PIN setelah 3× salah; 0 bila tidak terkunci. Pakai jam sungguhan agar tetap jalan setelah kill. */
    fun pinLockSeconds(): Int = ((pinLockedUntil - System.currentTimeMillis()).coerceAtLeast(0) + 999).div(1000).toInt()

    /** Cocokkan dengan hash PIN akun; 3× salah → terkunci 30 dtk (PRD FR-PAY-06). Hitungan ikut tersimpan. */
    fun checkPin(pin: String): PinCheck {
        pinLockSeconds().takeIf { it > 0 }?.let { return PinCheck.Locked(it) }
        val result = if (Hashing.verify(pin, pinHash)) {
            pinFailures = 0
            PinCheck.Ok
        } else if (++pinFailures >= MAX_PIN_FAILURES) {
            pinFailures = 0
            pinLockedUntil = System.currentTimeMillis() + PIN_LOCK_MS
            PinCheck.Locked(pinLockSeconds())
        } else {
            PinCheck.Wrong(MAX_PIN_FAILURES - pinFailures)
        }
        save()
        return result
    }

    // --- akun ---

    fun updateProfile(p: Profile) { _profile.value = p; save() }

    /** Simpan alamat baru atau perbarui yang ada; hanya satu alamat utama. */
    fun saveAddress(a: Address) {
        _addresses.update { list ->
            val withId = if (a.id == 0) a.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1) else a
            val others = list.filterNot { it.id == withId.id }.map { if (withId.primary) it.copy(primary = false) else it }
            (others + withId).let { all -> if (all.none { it.primary }) all.mapIndexed { i, x -> if (i == 0) x.copy(primary = true) else x } else all }
                .sortedBy { it.id }
        }
        save()
    }

    fun deleteAddress(id: Int) { _addresses.update { list -> list.filterNot { it.id == id } }; save() }

    /**
     * Muat data akun dari [store] saat akun berganti; akun yang belum pernah login dibuat dari seed.
     * `new_user`: saldo Rp0, tanpa alamat dan riwayat (PRD §11.1, §11.4).
     */
    fun applyUser(username: String, displayName: String, email: String, isNew: Boolean) {
        if (userKey == username) return
        val state = store.load(username) ?: seedState(displayName, email, isNew).also { store.save(username, it) }
        userKey = username
        _profile.value = state.profile
        _addresses.value = state.addresses
        _balance.value = state.balance
        _homeBalance.value = state.homeBalance
        _transactions.value = state.transactions
        txSequence = state.transactions.maxOfOrNull { it.id } ?: 0
        _cart.value = state.cart
        _badge.value = state.cart.filterNot { it.saved }.sumOf { it.qty }
        _wishlist.value = state.wishlist
        _history.value = state.history
        _voucher.value = state.voucher
        _orders.value = state.orders
        pinHash = state.pinHash
        pinFailures = state.pinFailures
        pinLockedUntil = state.pinLockedUntil
    }

    /**
     * "Reset semua data" (PRD §10): kosongkan [store]. Akun aktif dilupakan, jadi [applyUser] berikutnya
     * membuat ulang data dari seed dan Order ID mulai lagi dari 0001.
     */
    fun reset() {
        store.clear()
        userKey = null
        glitchPayTapUsed = false
    }

    private fun save() {
        val key = userKey ?: return
        store.save(
            key,
            UserState(
                _profile.value, _addresses.value, _balance.value, _homeBalance.value, _transactions.value,
                _cart.value, _wishlist.value, _history.value, _voucher.value, _orders.value,
                pinHash, pinFailures, pinLockedUntil,
            ),
        )
    }

    /** Buang baris keranjang yang sudah dibeli. */
    fun removePurchased(purchased: List<CartLine>) {
        _cart.update { lines -> lines.filterNot { l -> purchased.any { it.sameItem(l) } } }
        syncBadge()
    }

    private companion object {
        const val MAX_HISTORY = 5
        const val MAX_PIN_FAILURES = 3
        const val PIN_LOCK_MS = 30_000L
        val SEED_WISHLIST = listOf("P001", "P010", "P002", "P020")
        val SEED_HISTORY = listOf("sepatu lari", "headphone", "buku catatan", "smartwatch")

        fun seedState(displayName: String, email: String, isNew: Boolean): UserState {
            val balance = if (isNew) 0 else INITIAL_BALANCE
            return UserState(
                profile = Profile(displayName, email),
                addresses = if (isNew) emptyList() else SEED_ADDRESSES.map { it.copy(recipient = displayName) },
                balance = balance,
                homeBalance = balance,
                transactions = if (isNew) emptyList() else seedTransactions(),
                cart = emptyList(),
                wishlist = SEED_WISHLIST,
                history = SEED_HISTORY,
                voucher = null,
                orders = emptyList(),
                pinHash = Hashing.hash(PAY_PIN),
                pinFailures = 0,
                pinLockedUntil = 0,
            )
        }

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
