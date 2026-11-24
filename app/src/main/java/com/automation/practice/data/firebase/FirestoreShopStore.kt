package com.automation.practice.data.firebase

import android.util.Log
import com.automation.practice.data.repo.Change
import com.automation.practice.data.repo.Outcome
import com.automation.practice.data.repo.ShopStore
import com.automation.practice.data.repo.Stock
import com.automation.practice.data.repo.UserState
import com.automation.practice.data.repo.variantKey
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.automation.practice.domain.Tx
import com.automation.practice.domain.cashback
import com.automation.practice.domain.orderId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * [ShopStore] di Cloud Firestore: skema contract §8.2, aturan tulis §8.3. Tanpa listener (contract §7);
 * baca dengan `get()`, field per akun dengan `update()`, saldo/stok/status lewat transaksi.
 * [seedStock] = stok awal dari `catalog.json`, dipakai saat `catalog/stock` belum ada.
 */
class FirestoreShopStore(private val db: FirebaseFirestore, private val seedStock: () -> Stock) : ShopStore {
    private fun state(uid: String) = db.collection(STATES).document(uid)
    private fun order(uid: String, id: String) = state(uid).collection(ORDERS).document(id)
    private fun txs(uid: String) = state(uid).collection(TRANSACTIONS)
    private val stockRef get() = db.collection("catalog").document("stock")

    override suspend fun loadState(uid: String, seed: () -> UserState): UserState {
        val ref = state(uid)
        // get() biasa dulu: akun lama tetap terbaca dari cache saat offline; transaksi hanya untuk login pertama.
        var doc = ref.get().await()
        if (!doc.exists()) {
            db.runTransaction { t ->
                if (!t.get(ref).exists()) {
                    val s = seed()
                    t.set(ref, stateDoc(s))
                    s.transactions.forEach { t.set(txs(uid).document(it.id), txDoc(it)) }
                }
            }.await()
            doc = ref.get().await()
        }
        val orders = ref.collection(ORDERS).orderBy("createdAt", Query.Direction.DESCENDING).get().await()
        val txs = txs(uid).orderBy("at", Query.Direction.DESCENDING).get().await()
        return toState(doc, orders.map(::toOrder), txs.map(::toTx))
            .copy(fromCache = doc.metadata.isFromCache || orders.metadata.isFromCache || txs.metadata.isFromCache)
    }

    override suspend fun loadStock(): Stock {
        val doc = stockRef.get().await()
        if (doc.exists()) return toStock(doc)
        // Create hanya bila belum ada; client lain mungkin membuatnya lebih dulu (contract §8.3).
        return db.runTransaction { t ->
            val now = t.get(stockRef)
            if (now.exists()) toStock(now)
            else seedStock().also { t.set(stockRef, mapOf(STOCK to it, SCHEMA to SCHEMA_VERSION)) }
        }.await()
    }

    private fun write(uid: String, field: String, value: Any?) {
        state(uid).update(field, value).addOnFailureListener { Log.w(TAG, "update $field gagal", it) }
    }

    private val editScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val editLock = Mutex()

    /**
     * Baca-ubah-tulis satu field list (pola web K9): [f] dijalankan pada nilai terbaru dokumen, bukan salinan memori.
     * `update()` tidak ditunggu supaya tidak macet saat offline; ia sudah terlihat oleh `get()` berikutnya, dan [editLock]
     * menjaga urutan antar-edit. Bukan transaksi: transaksi Firestore gagal saat offline, jadi jendela tabrakan menyempit, bukan nol.
     */
    private fun <T> edit(uid: String, field: String, read: (DocumentSnapshot) -> List<T>, doc: (T) -> Any, f: (List<T>) -> List<T>) {
        editScope.launch {
            editLock.withLock {
                try {
                    val fresh = read(state(uid).get().await())
                    state(uid).update(field, f(fresh).map(doc)).addOnFailureListener { Log.w(TAG, "update $field gagal", it) }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "edit $field gagal", e)
                }
            }
        }
    }

    override fun editCart(uid: String, f: (List<CartLine>) -> List<CartLine>) =
        edit(uid, "cart", { it.get("cart").maps().map(::toLine) }, ::cartDoc, f)
    override fun editWishlist(uid: String, f: (List<String>) -> List<String>) = edit(uid, "wishlist", { it.strings("wishlist") }, { it }, f)
    override fun editHistory(uid: String, f: (List<String>) -> List<String>) = edit(uid, "history", { it.strings("history") }, { it }, f)
    override fun editAddresses(uid: String, f: (List<Address>) -> List<Address>) =
        edit(uid, "addresses", { it.get("addresses").maps().map(::toAddress) }, ::addressDoc, f)
    override fun setProfile(uid: String, profile: Profile) = write(uid, "profile", profileDoc(profile))
    override fun setVoucher(uid: String, voucher: String?) = write(uid, "voucher", voucher)
    override fun setPin(uid: String, hash: String, failures: Int, lockedUntil: Long) {
        state(uid).update("pinHash", hash, "pinFailures", failures, "pinLockedUntil", lockedUntil)
            .addOnFailureListener { Log.w(TAG, "update PIN gagal", it) }
    }

    override suspend fun placeOrder(uid: String, draft: Order, at: Long) = transact { t ->
        val s = t.get(state(uid))
        val stockDoc = t.get(stockRef)
        val stock = if (stockDoc.exists()) toStock(stockDoc) else seedStock()
        val left = draft.lines.groupBy { it.productId to it.variantKey }.mapValues { (k, ls) ->
            (stock[k.first]?.get(k.second) ?: 0) - ls.sumOf { it.qty }
        }
        left.entries.firstOrNull { it.value < 0 }?.let { return@transact Outcome.NoStock(it.key.first) }
        val wallet = draft.method == PayMethod.WALLET
        val balance = s.long("balance")
        if (wallet && balance < draft.total) return@transact Outcome.NoBalance

        val seq = s.long("orderSeq") + 1
        val o = draft.copy(id = orderId(seq.toInt()), statusAt = at, stamps = mapOf(draft.status to at))
        t.set(order(uid, o.id), orderDoc(o) + ("createdAt" to at))
        val newStock = stock.mapValues { (pid, v) -> v.mapValues { (key, n) -> left[pid to key] ?: n } }
        if (stockDoc.exists()) left.forEach { (k, n) -> t.update(stockRef, FieldPath.of(STOCK, k.first, k.second), n) }
        else t.set(stockRef, mapOf(STOCK to newStock, SCHEMA to SCHEMA_VERSION))
        val tx = if (wallet) newTx(t, uid, "Belanja PracticeMart", -o.total, at) else null
        if (wallet) t.update(state(uid), "orderSeq", seq, "balance", balance - o.total)
        else t.update(state(uid), "orderSeq", seq)
        Outcome.Done(Change(o, if (wallet) balance - o.total else null, tx, newStock))
    }

    override suspend fun advanceStatus(uid: String, id: String, expected: OrderStatus, at: Long) = transact { t ->
        val o = toOrder(t.get(order(uid, id)))
        val next = expected.next()
        if (o.status != expected || next == null) return@transact Outcome.Done(Change(o, changed = false))
        val cash = if (next == OrderStatus.DONE && o.method == PayMethod.WALLET) cashback(o.total) else 0L
        // Semua baca sebelum tulis (aturan transaksi Firestore).
        val balance = if (cash > 0) t.get(state(uid)).long("balance") else 0L
        t.update(order(uid, id), "status", next.name, "statusAt", at, FieldPath.of("stamps", next.name), at)
        val tx = if (cash > 0) newTx(t, uid, "Cashback belanja", cash, at).also { t.update(state(uid), "balance", balance + cash) } else null
        Outcome.Done(Change(o.copy(status = next, statusAt = at, stamps = o.stamps + (next to at)), tx?.let { balance + cash }, tx))
    }

    override suspend fun cancelOrder(uid: String, id: String, reason: String, at: Long) = transact { t ->
        val o = toOrder(t.get(order(uid, id)))
        if (!o.status.cancellable) return@transact Outcome.Done(Change(o, changed = false))
        val stock = toStock(t.get(stockRef))
        val back = o.lines.groupBy { it.productId to it.variantKey }.mapValues { (k, ls) ->
            (stock[k.first]?.get(k.second) ?: 0) + ls.sumOf { it.qty }
        }
        t.update(
            order(uid, id), "status", OrderStatus.CANCELLED.name, "statusAt", at,
            FieldPath.of("stamps", OrderStatus.CANCELLED.name), at, "cancelReason", reason,
        )
        // Varian yang tidak ada lagi di dokumen stok dilewati: rules melarang key baru.
        back.filterKeys { stock[it.first]?.containsKey(it.second) == true }
            .forEach { (k, n) -> t.update(stockRef, FieldPath.of(STOCK, k.first, k.second), n) }
        val done = o.copy(status = OrderStatus.CANCELLED, statusAt = at, stamps = o.stamps + (OrderStatus.CANCELLED to at), cancelReason = reason)
        Outcome.Done(Change(done, stock = stock.mapValues { (pid, v) -> v.mapValues { (key, n) -> back[pid to key] ?: n } }))
    }

    override suspend fun spend(uid: String, amount: Long, title: String, at: Long) = transact { t ->
        val balance = t.get(state(uid)).long("balance")
        if (balance < amount) return@transact Outcome.NoBalance
        val tx = newTx(t, uid, title, -amount, at)
        t.update(state(uid), "balance", balance - amount)
        Outcome.Done(Change(balance = balance - amount, tx = tx))
    }

    override suspend fun topUp(uid: String, amount: Long, title: String, at: Long) = transact { t ->
        val balance = t.get(state(uid)).long("balance")
        val tx = newTx(t, uid, title, amount, at)
        t.update(state(uid), "balance", balance + amount)
        Outcome.Done(Change(balance = balance + amount, tx = tx))
    }

    private fun newTx(t: Transaction, uid: String, title: String, amount: Long, at: Long): Tx {
        val ref = txs(uid).document()
        return Tx(ref.id, title, amount, at).also { t.set(ref, txDoc(it)) }
    }

    /** Transaksi butuh online; offline → [Outcome.Offline] (contract §8.3). */
    private suspend fun transact(block: (Transaction) -> Outcome<Change>): Outcome<Change> = try {
        db.runTransaction { block(it) }.await()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "transaksi gagal", e)
        if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.UNAVAILABLE) Outcome.Offline else Outcome.Failed
    }

    private companion object {
        const val TAG = "FirestoreShopStore"
        const val STATES = "states"
        const val ORDERS = "orders"
        const val TRANSACTIONS = "transactions"
        const val STOCK = "stock"
        const val SCHEMA = "schemaVersion"
        const val SCHEMA_VERSION = 1

        fun DocumentSnapshot.long(field: String) = getLong(field) ?: 0L

        // --- domain → dokumen (contract §8.2) ---

        fun profileDoc(p: Profile) = mapOf("name" to p.name, "email" to p.email, "birth" to p.birth, "gender" to p.gender, "photoUri" to p.photoUri)
        fun cartDoc(l: CartLine) = mapOf("productId" to l.productId, "color" to l.color, "size" to l.size, "qty" to l.qty, "selected" to l.selected, "saved" to l.saved)
        fun lineDoc(l: CartLine) = mapOf("productId" to l.productId, "color" to l.color, "size" to l.size, "qty" to l.qty)
        fun addressDoc(a: Address) = mapOf(
            "id" to a.id, "label" to a.label, "recipient" to a.recipient, "phone" to a.phone, "province" to a.province,
            "city" to a.city, "street" to a.street, "postal" to a.postal, "isPrimary" to a.primary,
        )
        fun txDoc(t: Tx) = mapOf("id" to t.id, "title" to t.title, "amount" to t.amount, "at" to t.at)
        fun orderDoc(o: Order) = mapOf(
            "id" to o.id, "total" to o.total, "method" to o.method.name, "methodLabel" to o.methodLabel,
            "status" to o.status.name, "statusAt" to o.statusAt, "stamps" to o.stamps.mapKeys { it.key.name },
            "cancelReason" to o.cancelReason, "vaDeadline" to o.vaDeadline, "lines" to o.lines.map(::lineDoc),
        )
        fun stateDoc(s: UserState) = mapOf(
            "profile" to profileDoc(s.profile), "balance" to s.balance, "pinHash" to s.pinHash,
            "pinFailures" to s.pinFailures, "pinLockedUntil" to s.pinLockedUntil, "voucher" to s.voucher,
            "cart" to s.cart.map(::cartDoc), "wishlist" to s.wishlist, "history" to s.history,
            "addresses" to s.addresses.map(::addressDoc), "orderSeq" to 0, SCHEMA to SCHEMA_VERSION,
        )

        // --- dokumen → domain; field yang belum ada diberi default (contract §8.5) ---

        @Suppress("UNCHECKED_CAST")
        fun Any?.maps() = (this as? List<Map<String, Any?>>).orEmpty()
        fun Map<String, Any?>.str(k: String) = this[k] as? String ?: ""
        fun Map<String, Any?>.int(k: String) = (this[k] as? Number)?.toInt() ?: 0

        /** Dokumen lama/luar bisa menyimpan nama enum huruf kecil atau tak dikenal; satu dokumen jelek tidak boleh menggagalkan seluruh muat akun. */
        inline fun <reified E : Enum<E>> enumOf(raw: String?, default: E): E = enumValues<E>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default

        fun toLine(m: Map<String, Any?>) = CartLine(
            m.str("productId"), m.str("color"), m.str("size"), m.int("qty").coerceAtLeast(1),
            m["selected"] as? Boolean ?: true, m["saved"] as? Boolean ?: false,
        )

        // id web berupa string; dokumen lama dari app menyimpan angka.
        fun toAddress(m: Map<String, Any?>) = Address(
            when (val id = m["id"]) { is String -> id; is Number -> id.toLong().toString(); else -> "" }, m.str("label"), m.str("recipient"), m.str("phone"), m.str("province"),
            m.str("city"), m.str("street"), m.str("postal"), m["isPrimary"] as? Boolean ?: false,
        )

        fun DocumentSnapshot.strings(field: String) = (get(field) as? List<*>).orEmpty().filterIsInstance<String>()

        fun toOrder(d: DocumentSnapshot) = Order(
            id = d.getString("id") ?: d.id,
            lines = d.get("lines").maps().map(::toLine),
            total = d.long("total"),
            method = enumOf(d.getString("method"), PayMethod.CARD),
            methodLabel = d.getString("methodLabel").orEmpty(),
            status = enumOf(d.getString("status"), OrderStatus.WAITING),
            statusAt = d.long("statusAt"),
            stamps = (d.get("stamps") as? Map<*, *>).orEmpty().entries
                .associate { OrderStatus.valueOf(it.key as String) to (it.value as Number).toLong() },
            cancelReason = d.getString("cancelReason").orEmpty(),
            vaDeadline = d.long("vaDeadline"),
        )

        fun toTx(d: DocumentSnapshot) = Tx(d.getString("id") ?: d.id, d.getString("title").orEmpty(), d.long("amount"), d.long("at"))

        fun toStock(d: DocumentSnapshot): Stock = (d.get(STOCK) as? Map<*, *>).orEmpty().entries.associate { (pid, v) ->
            pid as String to (v as Map<*, *>).entries.associate { (k, n) -> k as String to (n as Number).toInt() }
        }

        fun toState(d: DocumentSnapshot, orders: List<Order>, txs: List<Tx>): UserState {
            val p = d.get("profile") as? Map<String, Any?> ?: emptyMap()
            return UserState(
                profile = Profile(p.str("name"), p.str("email"), p.str("birth"), p.str("gender"), p["photoUri"] as? String),
                addresses = d.get("addresses").maps().map(::toAddress),
                balance = d.long("balance"),
                transactions = txs,
                cart = d.get("cart").maps().map(::toLine),
                wishlist = d.strings("wishlist"),
                history = d.strings("history"),
                voucher = d.getString("voucher"),
                orders = orders,
                pinHash = d.getString("pinHash").orEmpty(),
                pinFailures = d.long("pinFailures").toInt(),
                pinLockedUntil = d.long("pinLockedUntil"),
            )
        }
    }
}
