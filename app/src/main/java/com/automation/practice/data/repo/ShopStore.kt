package com.automation.practice.data.repo

import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.Profile
import com.automation.practice.domain.Tx

/** Semua data belanja & PracticePay milik satu akun (ARCHITECTURE.md §6.4). */
data class UserState(
    val profile: Profile,
    val addresses: List<Address>,
    val balance: Long,
    /** Saldo di Beranda Belanja; beda dari [balance] hanya untuk bug `glitch_user` #6. */
    val homeBalance: Long,
    val transactions: List<Tx>,
    val cart: List<CartLine>,
    val wishlist: List<String>,
    val history: List<String>,
    val voucher: String?,
    val orders: List<Order>,
    val pinHash: String,
    val pinFailures: Int,
    val pinLockedUntil: Long,
)

/** Penyimpanan data per akun untuk [ShopSession]: Room di app ([com.automation.practice.data.db.RoomShopStore]), memori di unit test. */
interface ShopStore {
    /** Null bila akun belum pernah login (atau setelah Reset). */
    fun load(username: String): UserState?
    fun save(username: String, state: UserState)

    /** Nomor urut Order ID berikutnya, global untuk semua akun (PRD FR-PAYM-13). */
    fun nextOrderSequence(): Int

    /** "Reset semua data" (PRD §10). */
    fun clear()
}

/** Tanpa disk: data hilang bersama objeknya. Dipakai unit test untuk meniru database yang bertahan setelah kill. */
class MemoryShopStore : ShopStore {
    private val states = mutableMapOf<String, UserState>()
    private var sequence = 0

    override fun load(username: String) = states[username]
    override fun save(username: String, state: UserState) { states[username] = state }
    override fun nextOrderSequence() = ++sequence
    override fun clear() { states.clear(); sequence = 0 }
}
