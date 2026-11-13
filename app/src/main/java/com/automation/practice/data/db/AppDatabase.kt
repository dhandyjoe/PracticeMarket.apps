package com.automation.practice.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

// Skema tabel: ARCHITECTURE.md §6.4. Ubah entity = naikkan version + migrasi (§6.6), jangan destruktif.

@Entity(tableName = "users")
data class UserRow(
    @PrimaryKey val username: String,
    val email: String,
    val displayName: String,
    val behavior: String,
    val passwordHash: String,
)

/** Nilai tunggal: `session_user`, `order_seq`. */
@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val name: String, val value: String)

@Entity(tableName = "accounts")
data class AccountRow(
    @PrimaryKey val username: String,
    val name: String,
    val email: String,
    val birth: String,
    val gender: String,
    val photoUri: String?,
    val balance: Long,
    val homeBalance: Long,
    val pinHash: String,
    val pinFailures: Int,
    val pinLockedUntil: Long,
    val voucher: String?,
)

@Entity(tableName = "cart_items", primaryKeys = ["username", "position"])
data class CartRow(
    val username: String,
    val position: Int,
    val productId: String,
    val color: String,
    val size: String,
    val qty: Int,
    val selected: Boolean,
    val saved: Boolean,
)

@Entity(tableName = "wishlist", primaryKeys = ["username", "position"])
data class WishlistRow(val username: String, val position: Int, val productId: String)

@Entity(tableName = "search_history", primaryKeys = ["username", "position"])
data class HistoryRow(val username: String, val position: Int, val query: String)

@Entity(tableName = "addresses", primaryKeys = ["username", "id"])
data class AddressRow(
    val username: String,
    val id: Int,
    val label: String,
    val recipient: String,
    val phone: String,
    val province: String,
    val city: String,
    val street: String,
    val postal: String,
    val isPrimary: Boolean,
)

@Entity(tableName = "orders")
data class OrderRow(
    @PrimaryKey val id: String,
    val username: String,
    val total: Long,
    val method: String,
    val methodLabel: String,
    val status: String,
    val statusAt: Long,
    /** `PAID=1736900000000;PACKED=…` */
    val stamps: String,
    val cancelReason: String,
    val vaDeadline: Long,
)

@Entity(tableName = "order_items", primaryKeys = ["orderId", "position"])
data class OrderItemRow(
    val orderId: String,
    val position: Int,
    val productId: String,
    val color: String,
    val size: String,
    val qty: Int,
)

@Entity(tableName = "wallet_tx", primaryKeys = ["username", "id"])
data class TxRow(val username: String, val id: Int, val title: String, val amount: Long, val at: Long)

/** Hanya varian yang stoknya pernah berubah; sisanya dari catalog.json. */
@Entity(tableName = "stock", primaryKeys = ["productId", "color", "size"])
data class StockRow(val productId: String, val color: String, val size: String, val stock: Int)

@Dao
abstract class PracticeDao {
    // --- akun login & sesi ---
    @Query("SELECT * FROM users") abstract fun users(): List<UserRow>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun upsertUsers(rows: List<UserRow>)
    @Query("SELECT value FROM meta WHERE name = :name") abstract fun meta(name: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun setMeta(row: MetaRow)
    @Query("DELETE FROM meta WHERE name = :name") abstract fun deleteMeta(name: String)

    // --- stok ---
    @Query("SELECT * FROM stock") abstract fun stock(): List<StockRow>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun upsertStock(row: StockRow)

    // --- data per akun ---
    @Query("SELECT * FROM accounts WHERE username = :u") abstract fun account(u: String): AccountRow?
    @Query("SELECT * FROM cart_items WHERE username = :u ORDER BY position") abstract fun cart(u: String): List<CartRow>
    @Query("SELECT * FROM wishlist WHERE username = :u ORDER BY position") abstract fun wishlist(u: String): List<WishlistRow>
    @Query("SELECT * FROM search_history WHERE username = :u ORDER BY position") abstract fun history(u: String): List<HistoryRow>
    @Query("SELECT * FROM addresses WHERE username = :u ORDER BY id") abstract fun addresses(u: String): List<AddressRow>
    @Query("SELECT * FROM orders WHERE username = :u ORDER BY id DESC") abstract fun orders(u: String): List<OrderRow>
    @Query("SELECT * FROM order_items WHERE orderId IN (SELECT id FROM orders WHERE username = :u) ORDER BY orderId, position")
    abstract fun orderItems(u: String): List<OrderItemRow>
    @Query("SELECT * FROM wallet_tx WHERE username = :u ORDER BY id DESC") abstract fun transactions(u: String): List<TxRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract fun insertAccount(row: AccountRow)
    @Insert protected abstract fun insertCart(rows: List<CartRow>)
    @Insert protected abstract fun insertWishlist(rows: List<WishlistRow>)
    @Insert protected abstract fun insertHistory(rows: List<HistoryRow>)
    @Insert protected abstract fun insertAddresses(rows: List<AddressRow>)
    @Insert protected abstract fun insertOrders(rows: List<OrderRow>)
    @Insert protected abstract fun insertOrderItems(rows: List<OrderItemRow>)
    @Insert protected abstract fun insertTransactions(rows: List<TxRow>)

    @Query("DELETE FROM cart_items WHERE username = :u") protected abstract fun deleteCart(u: String)
    @Query("DELETE FROM wishlist WHERE username = :u") protected abstract fun deleteWishlist(u: String)
    @Query("DELETE FROM search_history WHERE username = :u") protected abstract fun deleteHistory(u: String)
    @Query("DELETE FROM addresses WHERE username = :u") protected abstract fun deleteAddresses(u: String)
    @Query("DELETE FROM order_items WHERE orderId IN (SELECT id FROM orders WHERE username = :u)") protected abstract fun deleteOrderItems(u: String)
    @Query("DELETE FROM orders WHERE username = :u") protected abstract fun deleteOrders(u: String)
    @Query("DELETE FROM wallet_tx WHERE username = :u") protected abstract fun deleteTransactions(u: String)

    /**
     * Tulis ulang semua data satu akun dalam satu transaksi.
     * shortcut: tulis ulang penuh tiap mutasi (data per akun < 100 baris); ganti ke update per baris bila riwayat membesar.
     */
    @Transaction
    open fun replaceAccount(
        account: AccountRow,
        cart: List<CartRow>,
        wishlist: List<WishlistRow>,
        history: List<HistoryRow>,
        addresses: List<AddressRow>,
        orders: List<OrderRow>,
        orderItems: List<OrderItemRow>,
        transactions: List<TxRow>,
    ) {
        val u = account.username
        deleteCart(u); deleteWishlist(u); deleteHistory(u); deleteAddresses(u)
        deleteOrderItems(u); deleteOrders(u); deleteTransactions(u)
        insertAccount(account)
        insertCart(cart); insertWishlist(wishlist); insertHistory(history); insertAddresses(addresses)
        insertOrders(orders); insertOrderItems(orderItems); insertTransactions(transactions)
    }
}

@Database(
    entities = [
        UserRow::class, MetaRow::class, AccountRow::class, CartRow::class, WishlistRow::class, HistoryRow::class,
        AddressRow::class, OrderRow::class, OrderItemRow::class, TxRow::class, StockRow::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PracticeDao

    companion object {
        /** File `databases/practice.db` di internal storage: bertahan saat kill & update, hilang saat Clear data (ARCHITECTURE.md D1). */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "practice.db")
                // shortcut: query di main thread karena data kecil dan UI memakai API sinkron; pindah ke executor bila terasa jank.
                .allowMainThreadQueries()
                .build()
    }
}
