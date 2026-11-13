package com.automation.practice.data.db

import com.automation.practice.data.repo.ShopStore
import com.automation.practice.data.repo.UserState
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.automation.practice.domain.Tx

/** [ShopStore] di atas Room: memetakan [UserState] ke tabel per akun (ARCHITECTURE.md §6.4). */
class RoomShopStore(private val db: AppDatabase) : ShopStore {
    private val dao = db.dao()

    override fun load(username: String): UserState? {
        val a = dao.account(username) ?: return null
        val items = dao.orderItems(username).groupBy { it.orderId }
        return UserState(
            profile = Profile(a.name, a.email, a.birth, a.gender, a.photoUri),
            addresses = dao.addresses(username).map { Address(it.id, it.label, it.recipient, it.phone, it.province, it.city, it.street, it.postal, it.isPrimary) },
            balance = a.balance,
            homeBalance = a.homeBalance,
            transactions = dao.transactions(username).map { Tx(it.id, it.title, it.amount, it.at) },
            cart = dao.cart(username).map { CartLine(it.productId, it.color, it.size, it.qty, it.selected, it.saved) },
            wishlist = dao.wishlist(username).map { it.productId },
            history = dao.history(username).map { it.query },
            voucher = a.voucher,
            orders = dao.orders(username).map { o ->
                Order(
                    id = o.id,
                    lines = items[o.id].orEmpty().map { CartLine(it.productId, it.color, it.size, it.qty) },
                    total = o.total,
                    method = PayMethod.valueOf(o.method),
                    methodLabel = o.methodLabel,
                    status = OrderStatus.valueOf(o.status),
                    statusAt = o.statusAt,
                    stamps = o.stamps.split(';').filter { it.isNotEmpty() }
                        .associate { it.substringBefore('=').let(OrderStatus::valueOf) to it.substringAfter('=').toLong() },
                    cancelReason = o.cancelReason,
                    vaDeadline = o.vaDeadline,
                )
            },
            pinHash = a.pinHash,
            pinFailures = a.pinFailures,
            pinLockedUntil = a.pinLockedUntil,
        )
    }

    override fun save(username: String, state: UserState) {
        val p = state.profile
        dao.replaceAccount(
            account = AccountRow(
                username, p.name, p.email, p.birth, p.gender, p.photoUri, state.balance, state.homeBalance,
                state.pinHash, state.pinFailures, state.pinLockedUntil, state.voucher,
            ),
            cart = state.cart.mapIndexed { i, l -> CartRow(username, i, l.productId, l.color, l.size, l.qty, l.selected, l.saved) },
            wishlist = state.wishlist.mapIndexed { i, id -> WishlistRow(username, i, id) },
            history = state.history.mapIndexed { i, q -> HistoryRow(username, i, q) },
            addresses = state.addresses.map { AddressRow(username, it.id, it.label, it.recipient, it.phone, it.province, it.city, it.street, it.postal, it.primary) },
            orders = state.orders.map { o ->
                OrderRow(
                    o.id, username, o.total, o.method.name, o.methodLabel, o.status.name, o.statusAt,
                    o.stamps.entries.joinToString(";") { "${it.key.name}=${it.value}" }, o.cancelReason, o.vaDeadline,
                )
            },
            orderItems = state.orders.flatMap { o -> o.lines.mapIndexed { i, l -> OrderItemRow(o.id, i, l.productId, l.color, l.size, l.qty) } },
            transactions = state.transactions.map { TxRow(username, it.id, it.title, it.amount, it.at) },
        )
    }

    override fun nextOrderSequence(): Int {
        val next = (dao.meta(ORDER_SEQ)?.toIntOrNull() ?: 0) + 1
        dao.setMeta(MetaRow(ORDER_SEQ, next.toString()))
        return next
    }

    /** Mengosongkan seluruh database, termasuk akun & sesi; pemanggil memuat ulang repository lain (AppContainer.resetAll). */
    override fun clear() = db.clearAllTables()

    private companion object {
        const val ORDER_SEQ = "order_seq"
    }
}
