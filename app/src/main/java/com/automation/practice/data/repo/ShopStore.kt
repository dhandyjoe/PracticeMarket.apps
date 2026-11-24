package com.automation.practice.data.repo

import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.automation.practice.domain.Tx
import com.automation.practice.domain.cashback
import com.automation.practice.domain.orderId

/** Stok global per produk → `"<warna>|<ukuran>"` → jumlah (contract §8.2 `catalog/stock`). */
typealias Stock = Map<String, Map<String, Int>>

/** Key varian di [Stock]; varian tanpa warna/ukuran = `"|"`. */
val CartLine.variantKey get() = "$color|$size"

/** Semua data belanja & PracticePay milik satu akun: `states/{uid}` + subkoleksi `orders/` & `transactions/` (contract §8.2). */
data class UserState(
    val profile: Profile,
    val addresses: List<Address>,
    val balance: Long,
    val transactions: List<Tx>,
    val cart: List<CartLine>,
    val wishlist: List<String>,
    val history: List<String>,
    val voucher: String?,
    val orders: List<Order>,
    val pinHash: String,
    val pinFailures: Int,
    val pinLockedUntil: Long,
    /** true bila dokumen dibaca dari cache lokal, bukan server (Firestore tidak terjangkau). */
    val fromCache: Boolean = false,
)

/** Hasil transaksi Firestore; selain [Done] dipetakan UI ke teks contract §6. */
sealed interface Outcome<out T> {
    data class Done<T>(val value: T) : Outcome<T>
    data class NoStock(val productId: String) : Outcome<Nothing>
    data object NoBalance : Outcome<Nothing>
    data object Offline : Outcome<Nothing>
    data object Failed : Outcome<Nothing>
}

/** Ubah nilai [Outcome.Done]; kegagalan diteruskan apa adanya. */
fun <T, R> Outcome<T>.map(f: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Done -> Outcome.Done(f(value))
    is Outcome.NoStock -> this
    Outcome.NoBalance -> Outcome.NoBalance
    Outcome.Offline -> Outcome.Offline
    Outcome.Failed -> Outcome.Failed
}

/**
 * Isi dokumen sesudah transaksi, untuk memperbarui cache [ShopSession]. Null = tidak berubah.
 * [order] selalu isi dokumen pesanan terkini; [changed] false bila transaksi tidak menulis apa pun (sudah diubah client lain).
 */
data class Change(val order: Order? = null, val balance: Long? = null, val tx: Tx? = null, val stock: Stock? = null, val changed: Boolean = true)

/**
 * Penyimpanan data per akun untuk [ShopSession] (ARCHITECTURE.md §6.5): Firestore di app
 * ([com.automation.practice.data.firebase.FirestoreShopStore]), memori di unit test. Aturan tulis = contract §8.3.
 */
interface ShopStore {
    /** Data akun; dibuat dari [seed] dalam transaksi bila `states/{uid}` belum ada (login pertama). */
    suspend fun loadState(uid: String, seed: () -> UserState): UserState

    /** Stok global; dokumen dibuat dari stok awal bila belum ada. */
    suspend fun loadStock(): Stock

    // Field list: baca-ubah-tulis. [f] dijalankan pada nilai terbaru di dokumen, bukan salinan memori yang bisa basi,
    // supaya perubahan device lain tidak tertimpa. Tidak menunggu server; urutan antar-panggilan terjaga.
    fun editCart(uid: String, f: (List<CartLine>) -> List<CartLine>)
    fun editWishlist(uid: String, f: (List<String>) -> List<String>)
    fun editHistory(uid: String, f: (List<String>) -> List<String>)
    fun editAddresses(uid: String, f: (List<Address>) -> List<Address>)

    // update() per field skalar: tidak menunggu server, diterapkan ke cache lokal seketika.
    fun setProfile(uid: String, profile: Profile)
    fun setVoucher(uid: String, voucher: String?)
    fun setPin(uid: String, hash: String, failures: Int, lockedUntil: Long)

    /** Pesanan baru: cek stok (& saldo bila PracticePay), `orderSeq + 1`, kurangi stok. ID [draft] diabaikan. */
    suspend fun placeOrder(uid: String, draft: Order, at: Long): Outcome<Change>

    /** Maju satu status hanya bila status sekarang = [expected]; cashback PracticePay saat `DONE`. */
    suspend fun advanceStatus(uid: String, id: String, expected: OrderStatus, at: Long): Outcome<Change>

    /** Batal hanya bila belum `CANCELLED` dan status ≤ `PACKED`; stok kembali. */
    suspend fun cancelOrder(uid: String, id: String, reason: String, at: Long): Outcome<Change>

    /** Transfer & Bayar QR: kurangi saldo dan catat transaksi keluar. */
    suspend fun spend(uid: String, amount: Long, title: String, at: Long): Outcome<Change>

    /** Top Up: tambah saldo dan catat transaksi masuk. */
    suspend fun topUp(uid: String, amount: Long, title: String, at: Long): Outcome<Change>
}

/** Tanpa disk & tanpa jaringan, aturan sama dengan Firestore. Dipakai unit test; objek yang sama = data di cloud. */
class MemoryShopStore(initialStock: Stock = emptyMap()) : ShopStore {
    private val states = mutableMapOf<String, UserState>()
    private val seqs = mutableMapOf<String, Int>()
    private val stock = initialStock.mapValues { it.value.toMutableMap() }.toMutableMap()
    private var txIds = 0

    override suspend fun loadState(uid: String, seed: () -> UserState) = states.getOrPut(uid, seed)
    override suspend fun loadStock(): Stock = stock.mapValues { it.value.toMap() }

    private fun edit(uid: String, f: (UserState) -> UserState) { states[uid]?.let { states[uid] = f(it) } }
    override fun editCart(uid: String, f: (List<CartLine>) -> List<CartLine>) = edit(uid) { it.copy(cart = f(it.cart)) }
    override fun editWishlist(uid: String, f: (List<String>) -> List<String>) = edit(uid) { it.copy(wishlist = f(it.wishlist)) }
    override fun editHistory(uid: String, f: (List<String>) -> List<String>) = edit(uid) { it.copy(history = f(it.history)) }
    override fun editAddresses(uid: String, f: (List<Address>) -> List<Address>) = edit(uid) { it.copy(addresses = f(it.addresses)) }
    override fun setProfile(uid: String, profile: Profile) = edit(uid) { it.copy(profile = profile) }
    override fun setVoucher(uid: String, voucher: String?) = edit(uid) { it.copy(voucher = voucher) }
    override fun setPin(uid: String, hash: String, failures: Int, lockedUntil: Long) =
        edit(uid) { it.copy(pinHash = hash, pinFailures = failures, pinLockedUntil = lockedUntil) }

    private fun stockOf(l: CartLine) = stock[l.productId]?.get(l.variantKey) ?: 0
    private fun adjust(lines: List<CartLine>, sign: Int) = lines.forEach { l ->
        stock[l.productId]?.let { it[l.variantKey] = stockOf(l) + sign * l.qty }
    }
    private fun newTx(title: String, amount: Long, at: Long) = Tx("m${++txIds}", title, amount, at)

    override suspend fun placeOrder(uid: String, draft: Order, at: Long): Outcome<Change> {
        val s = states.getValue(uid)
        draft.lines.firstOrNull { stockOf(it) < it.qty }?.let { return Outcome.NoStock(it.productId) }
        val wallet = draft.method == PayMethod.WALLET
        if (wallet && s.balance < draft.total) return Outcome.NoBalance
        val seq = seqs.getOrDefault(uid, 0) + 1
        seqs[uid] = seq
        val order = draft.copy(id = orderId(seq), statusAt = at, stamps = mapOf(draft.status to at))
        adjust(draft.lines, -1)
        val tx = if (wallet) newTx("Belanja PracticeMart", -draft.total, at) else null
        val balance = s.balance - (if (wallet) draft.total else 0L)
        states[uid] = s.copy(orders = listOf(order) + s.orders, balance = balance, transactions = listOfNotNull(tx) + s.transactions)
        return Outcome.Done(Change(order, balance.takeIf { wallet }, tx, loadStock()))
    }

    override suspend fun advanceStatus(uid: String, id: String, expected: OrderStatus, at: Long): Outcome<Change> {
        val s = states.getValue(uid)
        val o = s.orders.first { it.id == id }
        val next = expected.next()
        if (o.status != expected || next == null) return Outcome.Done(Change(o, changed = false))
        val moved = o.copy(status = next, statusAt = at, stamps = o.stamps + (next to at))
        val cash = if (next == OrderStatus.DONE && o.method == PayMethod.WALLET) cashback(o.total) else 0L
        val tx = if (cash > 0) newTx("Cashback belanja", cash, at) else null
        states[uid] = s.copy(
            orders = s.orders.map { if (it.id == id) moved else it },
            balance = s.balance + cash, transactions = listOfNotNull(tx) + s.transactions,
        )
        return Outcome.Done(Change(moved, (s.balance + cash).takeIf { tx != null }, tx))
    }

    override suspend fun cancelOrder(uid: String, id: String, reason: String, at: Long): Outcome<Change> {
        val s = states.getValue(uid)
        val o = s.orders.first { it.id == id }
        if (!o.status.cancellable) return Outcome.Done(Change(o, changed = false))
        val done = o.copy(status = OrderStatus.CANCELLED, statusAt = at, stamps = o.stamps + (OrderStatus.CANCELLED to at), cancelReason = reason)
        adjust(o.lines, +1)
        states[uid] = s.copy(orders = s.orders.map { if (it.id == id) done else it })
        return Outcome.Done(Change(done, stock = loadStock()))
    }

    override suspend fun spend(uid: String, amount: Long, title: String, at: Long): Outcome<Change> {
        val s = states.getValue(uid)
        if (s.balance < amount) return Outcome.NoBalance
        val tx = newTx(title, -amount, at)
        states[uid] = s.copy(balance = s.balance - amount, transactions = listOf(tx) + s.transactions)
        return Outcome.Done(Change(balance = s.balance - amount, tx = tx))
    }

    override suspend fun topUp(uid: String, amount: Long, title: String, at: Long): Outcome<Change> {
        val s = states.getValue(uid)
        val tx = newTx(title, amount, at)
        states[uid] = s.copy(balance = s.balance + amount, transactions = listOf(tx) + s.transactions)
        return Outcome.Done(Change(balance = s.balance + amount, tx = tx))
    }
}
