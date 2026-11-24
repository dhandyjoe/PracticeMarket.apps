package com.automation.practice.data.repo

import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Hashing
import com.automation.practice.domain.MAX_ADDRESSES
import com.automation.practice.domain.MAX_QTY
import com.automation.practice.domain.MAX_TOPUP
import com.automation.practice.domain.MIN_TOPUP
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PAY_PIN
import com.automation.practice.domain.Profile
import com.automation.practice.domain.SEED_ADDRESSES
import com.automation.practice.domain.Tx
import com.automation.practice.domain.jakartaTime
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
 * Data belanja & PracticePay akun yang sedang login. StateFlow di sini adalah cache dari [store] (ARCHITECTURE.md D15):
 * field per akun ditulis dengan `update()` per field, saldo/pesanan/stok lewat transaksi (contract §8.3).
 * [stockOf] memberi stok varian untuk batas jumlah per baris keranjang; [onStock] menerima stok global terbaru.
 */
class ShopSession(
    private val stockOf: (CartLine) -> Int,
    /** Jam asli untuk semua waktu yang ditulis ke store, juga saat mode deterministik (contract §5). */
    private val now: () -> Long = System::currentTimeMillis,
    private val store: ShopStore = MemoryShopStore(),
    private val onStock: (Stock) -> Unit = {},
) {
    private val _wishlist = MutableStateFlow(emptyList<String>())
    val wishlist: StateFlow<List<String>> = _wishlist.asStateFlow()

    private val _history = MutableStateFlow(emptyList<String>())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _cart = MutableStateFlow(emptyList<CartLine>())
    val cart: StateFlow<List<CartLine>> = _cart.asStateFlow()

    private val _badge = MutableStateFlow(0)

    /** Angka di badge tab Keranjang. */
    val badge: StateFlow<Int> = _badge.asStateFlow()

    private val _voucher = MutableStateFlow<String?>(null)
    val voucher: StateFlow<String?> = _voucher.asStateFlow()

    private val _balance = MutableStateFlow(0L)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    private val _orders = MutableStateFlow(emptyList<Order>())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _transactions = MutableStateFlow(emptyList<Tx>())
    val transactions: StateFlow<List<Tx>> = _transactions.asStateFlow()

    private val _profile = MutableStateFlow(Profile("", ""))
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _addresses = MutableStateFlow(emptyList<Address>())
    val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    private val _stale = MutableStateFlow(false)

    /** true bila [load] terakhir membaca cache lokal, jadi cart, wishlist, dan saldo bisa basi. */
    val stale: StateFlow<Boolean> = _stale.asStateFlow()

    /** `uid` akun yang datanya ada di cache; null sebelum [load] berhasil. */
    var uid: String? = null
        private set

    private var pinHash = ""
    private var pinFailures = 0
    private var pinLockedUntil = 0L

    /** Flash sale berakhir 2 jam 14 menit 37 detik sejak sesi dimulai. */
    val flashEndsAt = System.currentTimeMillis() + FLASH_DURATION_MS

    // --- wishlist & riwayat ---

    fun isWished(id: String) = id in _wishlist.value

    /** @return true bila produk sekarang ada di wishlist. */
    fun toggleWishlist(id: String): Boolean {
        val wished = isWished(id)
        editWishlist { if (wished) it - id else if (id in it) it else it + id }
        return !wished
    }

    /** Urutan baru; item yang ditambah client lain tetap ada (di belakang), yang dihapus client lain tidak hidup lagi. */
    fun setWishlistOrder(order: List<String>) {
        _wishlist.value = order
        write { editWishlist(it) { fresh -> order.filter { id -> id in fresh } + fresh.filter { id -> id !in order } } }
    }

    fun addHistory(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        editHistory { (listOf(t) + it.filterNot { h -> h.equals(t, ignoreCase = true) }).take(MAX_HISTORY) }
    }

    fun removeHistory(q: String) = editHistory { it - q }
    fun clearHistory() = editHistory { emptyList() }

    // --- keranjang ---

    /** Batas jumlah baris: min(stok varian, 10), minimal 1 (FR-SHOP-09). */
    fun maxQty(line: CartLine) = minOf(stockOf(line), MAX_QTY).coerceAtLeast(1)

    /** Gabungkan baris dengan varian sama (FR-SHOP-16); jumlah dibatasi [maxQty]. */
    fun addToCart(line: CartLine) = editCart { lines ->
        // shortcut: baris melewati MAX_CART_LINES diabaikan diam-diam; tidak terjangkau lewat UI (katalog 182 varian x qty 10), upgrade bila ada pesan.
        if (lines.none { it.sameItem(line) }) (if (lines.size >= MAX_CART_LINES) lines else lines + line)
        else lines.map { if (it.sameItem(line)) it.copy(qty = (it.qty + line.qty).coerceAtMost(maxQty(it)), saved = false) else it }
    }

    fun setQty(line: CartLine, qty: Int) = updateLine(line) { it.copy(qty = qty.coerceIn(1, maxQty(it))) }

    fun setSelected(line: CartLine, selected: Boolean) = updateLine(line) { it.copy(selected = selected) }

    fun setAllSelected(selected: Boolean) = editCart { lines -> lines.map { if (it.saved) it else it.copy(selected = selected) } }

    /** "Simpan untuk nanti": baris tetap di daftar, tapi tidak dipilih dan tidak dihitung. */
    fun toggleSaved(line: CartLine) {
        val saved = !line.saved
        updateLine(line) { it.copy(saved = saved, selected = false) }
    }

    fun remove(line: CartLine) = editCart { lines -> lines.filterNot { it.sameItem(line) } }

    /** Batalkan hapus: kembalikan baris di posisi semula. */
    fun restore(line: CartLine, index: Int) = editCart { lines ->
        if (lines.any { it.sameItem(line) }) lines else lines.toMutableList().also { it.add(index.coerceIn(0, it.size), line) }
    }

    private fun updateLine(line: CartLine, f: (CartLine) -> CartLine) =
        editCart { lines -> lines.map { if (it.sameItem(line)) f(it) else it } }

    /**
     * Semua mutasi keranjang lewat sini: [f] dijalankan di cache lalu, di store, pada keranjang terbaru dokumen
     * (bukan salinan memori yang bisa basi), jadi item yang ditambah device lain tidak ikut tertimpa.
     */
    private fun editCart(f: (List<CartLine>) -> List<CartLine>) {
        _cart.update(f)
        _badge.value = _cart.value.filterNot { it.saved }.sumOf { it.qty }
        write { editCart(it, f) }
    }

    // --- voucher ---

    fun setVoucher(code: String?) { _voucher.value = code; write { setVoucher(it, code) } }

    // --- pesanan & saldo (transaksi, butuh online) ---

    fun order(id: String) = _orders.value.firstOrNull { it.id == id }

    /** Buat pesanan dari [draft] (ID diisi store); stok & saldo dicek di transaksi (contract §8.3). */
    suspend fun placeOrder(draft: Order): Outcome<Order> = transact { placeOrder(it, draft, now()) }.map { it.order!! }

    /**
     * Majukan status satu langkah (simulator, tombol manual, VA dibayar). Pesanan PracticePay yang Selesai
     * memberi cashback ke saldo tanpa mengubah total pesanan (PRD §8.7).
     * @return status baru, atau null bila tidak bisa maju (sudah dimajukan client lain, offline).
     */
    suspend fun advance(id: String): OrderStatus? {
        val o = order(id) ?: return null
        if (o.status.next() == null) return null
        val r = transact { advanceStatus(it, id, o.status, now()) }
        return (r as? Outcome.Done)?.value?.takeIf { it.changed }?.order?.status
    }

    /** Batalkan pesanan; stok kembali di transaksi yang sama. Aman dipanggil dua kali (FR-ORD-05). */
    suspend fun cancelOrder(id: String, reason: String = ""): Outcome<Order> =
        transact { cancelOrder(it, id, reason, now()) }.map { it.order!! }

    /** Transfer & Bayar QR: kurangi saldo dan catat transaksi keluar. */
    suspend fun spend(amount: Long, title: String): Outcome<Tx> = transact { spend(it, amount, title, now()) }.map { it.tx!! }

    /** Top Up: tambah saldo dan catat transaksi masuk; nominal di luar batas ditolak. */
    suspend fun topUp(amount: Long, title: String): Outcome<Tx> =
        if (amount !in MIN_TOPUP..MAX_TOPUP) Outcome.Failed
        else transact { topUp(it, amount, title, now()) }.map { it.tx!! }

    /** Jalankan transaksi store lalu terapkan hasilnya ke cache. */
    private suspend fun transact(op: suspend ShopStore.(String) -> Outcome<Change>): Outcome<Change> {
        val key = uid ?: return Outcome.Failed
        val r = store.op(key)
        if (r !is Outcome.Done || uid != key) return r
        val c = r.value
        c.order?.let { o -> _orders.update { list -> if (list.any { it.id == o.id }) list.map { if (it.id == o.id) o else it } else listOf(o) + list } }
        c.balance?.let { _balance.value = it }
        c.tx?.let { tx -> _transactions.update { listOf(tx) + it } }
        c.stock?.let(onStock)
        return r
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
        write { setPin(it, pinHash, pinFailures, pinLockedUntil) }
        return result
    }

    // --- akun ---

    fun updateProfile(p: Profile) { _profile.value = p; write { setProfile(it, p) } }

    /** Simpan alamat baru atau perbarui yang ada; hanya satu alamat utama. */
    fun saveAddress(a: Address) = editAddresses { list ->
        if (a.id.isEmpty() && list.size >= MAX_ADDRESSES) return@editAddresses list
        val withId = if (a.id.isEmpty()) a.copy(id = newAddressId(list)) else a
        val replaced = list.any { it.id == withId.id }
        val cleared = list.map { if (withId.primary && it.id != withId.id) it.copy(primary = false) else it }
        // Urutan daftar dipertahankan seperti web: alamat yang diubah tetap di tempatnya, yang baru di belakang.
        (if (replaced) cleared.map { if (it.id == withId.id) withId else it } else cleared + withId)
            .let { all -> if (all.none { it.primary }) all.mapIndexed { i, x -> if (i == 0) x.copy(primary = true) else x } else all }
    }

    /** Id baru "addr-<ms>" seperti web; naik satu bila bentrok (dua simpan di milidetik yang sama). */
    private fun newAddressId(list: List<Address>): String {
        var n = now()
        while (list.any { it.id == "addr-$n" }) n++
        return "addr-$n"
    }

    fun deleteAddress(id: String) = editAddresses { list -> list.filterNot { it.id == id } }

    /**
     * Muat data akun dari [store] (`get()`, tanpa listener). Akun yang belum pernah login dibuat dari data awal
     * contract §4.5. Dipanggil saat app dibuka dan saat tarik-segarkan. Melempar exception bila gagal dibaca.
     */
    suspend fun load(uid: String, displayName: String, email: String) {
        val state = store.loadState(uid) { seedState(displayName, email) }
        this.uid = uid
        _profile.value = state.profile
        _addresses.value = state.addresses
        _balance.value = state.balance
        _transactions.value = state.transactions
        _cart.value = state.cart
        _badge.value = state.cart.filterNot { it.saved }.sumOf { it.qty }
        _wishlist.value = state.wishlist
        _history.value = state.history
        _voucher.value = state.voucher
        _orders.value = state.orders
        pinHash = state.pinHash
        pinFailures = state.pinFailures
        pinLockedUntil = state.pinLockedUntil
        _stale.value = state.fromCache
    }

    /** Tulis satu field akun aktif; tidak menunggu server. */
    private fun write(f: ShopStore.(String) -> Unit) { uid?.let { store.f(it) } }

    // Baca-ubah-tulis: f dijalankan di cache, lalu di store pada nilai terbaru dokumen (contract §8.3, pola web K9).
    private fun editWishlist(f: (List<String>) -> List<String>) { _wishlist.update(f); write { editWishlist(it, f) } }
    private fun editHistory(f: (List<String>) -> List<String>) { _history.update(f); write { editHistory(it, f) } }
    private fun editAddresses(f: (List<Address>) -> List<Address>) { _addresses.update(f); write { editAddresses(it, f) } }

    /** Buang baris keranjang yang sudah dibeli. */
    fun removePurchased(purchased: List<CartLine>) = editCart { lines -> lines.filterNot { l -> purchased.any { it.sameItem(l) } } }

    private companion object {
        const val MAX_HISTORY = 5
        const val MAX_CART_LINES = 100
        const val MAX_PIN_FAILURES = 3
        const val PIN_LOCK_MS = 30_000L
        val SEED_WISHLIST = listOf("P001", "P010", "P002", "P020")
        val SEED_HISTORY = listOf("sepatu lari", "headphone", "buku catatan", "smartwatch")

        /** Data awal akun (contract §4.5). */
        fun seedState(displayName: String, email: String) = UserState(
            profile = Profile(displayName, email),
            addresses = SEED_ADDRESSES.map { it.copy(recipient = displayName) },
            balance = INITIAL_BALANCE,
            transactions = seedTransactions(),
            cart = emptyList(),
            wishlist = SEED_WISHLIST,
            history = SEED_HISTORY,
            voucher = null,
            orders = emptyList(),
            pinHash = Hashing.hash(PAY_PIN),
            pinFailures = 0,
            pinLockedUntil = 0,
        )

        /** 8 transaksi awal deterministik, 12–15 Januari 2026 (contract §4.5); ID dokumen `1`–`8`. Terbaru di atas. */
        fun seedTransactions(): List<Tx> {
            fun t(d: Int, h: Int, m: Int) = jakartaTime(2026, 1, d, h, m)
            return listOf(
                Tx("8", "Top Up dari BCA", 300_000, t(15, 9, 12)),
                Tx("7", "Belanja PracticeMart", -86_000, t(15, 8, 47)),
                Tx("6", "Transfer ke Budi Santoso", -150_000, t(14, 19, 30)),
                Tx("5", "Cashback belanja", 4_300, t(14, 12, 5)),
                Tx("4", "Bayar QR · Kopi Pagi", -28_000, t(14, 7, 58)),
                Tx("3", "Terima dari Citra Dewi", 75_000, t(13, 20, 14)),
                Tx("2", "Belanja PracticeMart", -158_000, t(13, 10, 2)),
                Tx("1", "Top Up dari Mandiri", 500_000, t(12, 18, 40)),
            )
        }
        const val FLASH_DURATION_MS = ((2 * 60 + 14) * 60 + 37) * 1000L
    }
}
