package com.automation.practice.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.automation.practice.data.repo.AuthRepository
import com.automation.practice.data.repo.CatalogRepository
import com.automation.practice.data.repo.LoginResult
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.data.repo.UserState
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Hashing
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.automation.practice.domain.Tx
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room sungguhan di device (in-memory, tidak menyentuh practice.db milik app).
 * Membuat ulang repository di atas database yang sama = app dibuka lagi setelah kill (ARCHITECTURE.md §15).
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun close() = db.close()

    private val line = CartLine("P001", "Hitam", "L", 2)
    private fun state(cart: List<CartLine> = listOf(line, CartLine("P002", "", "42", 1, selected = false, saved = true))) = UserState(
        profile = Profile("Budi", "budi@x.id", "2000-01-02", "male", "content://foto"),
        addresses = listOf(Address(1, "Rumah", "Budi", "081", "Bali", "Denpasar", "Jl A", "80111", true)),
        balance = 360_000,
        homeBalance = 400_000,
        transactions = listOf(Tx(2, "Belanja PracticeMart", -140_000, 2_000), Tx(1, "Top Up", 500_000, 1_000)),
        cart = cart,
        wishlist = listOf("P010", "P001"),
        history = listOf("kaos", "sepatu"),
        voucher = "HEMAT10",
        orders = listOf(
            Order("PM-20260115-0002", listOf(line), 88_000, PayMethod.VA, "BCA Virtual Account", OrderStatus.WAITING, 5_000, mapOf(OrderStatus.WAITING to 5_000), "", 9_000),
            Order("PM-20260115-0001", listOf(line, CartLine("P020", "", "", 1)), 140_000, PayMethod.WALLET, "PracticePay", OrderStatus.DONE, 4_000,
                mapOf(OrderStatus.PAID to 1_000, OrderStatus.DONE to 4_000), "", 0),
        ),
        pinHash = Hashing.hash("123456"),
        pinFailures = 2,
        pinLockedUntil = 7_000,
    )

    @Test fun storeMenyimpanDanMembacaSemuaKolom() {
        val store = RoomShopStore(db)
        val saved = state()
        store.save("budi", saved)
        assertEquals(saved, RoomShopStore(db).load("budi"))
    }

    @Test fun simpanUlangMenggantiBarisLama() {
        val store = RoomShopStore(db)
        store.save("budi", state())
        store.save("budi", state(cart = emptyList()).copy(orders = emptyList(), wishlist = emptyList()))
        val loaded = store.load("budi")!!
        assertTrue(loaded.cart.isEmpty() && loaded.orders.isEmpty() && loaded.wishlist.isEmpty())
        assertTrue(db.dao().orderItems("budi").isEmpty())
    }

    @Test fun dataTiapAkunTerpisah() {
        val store = RoomShopStore(db)
        store.save("budi", state())
        store.save("citra", state(cart = emptyList()).copy(orders = emptyList()))
        assertEquals(2, store.load("budi")!!.cart.size)
        assertTrue(store.load("citra")!!.cart.isEmpty())
        assertEquals(2, store.load("budi")!!.orders.size)
        assertNull(store.load("dewi"))
    }

    @Test fun nomorOrderBerlanjutDanClearMengosongkanSemua() {
        val store = RoomShopStore(db)
        assertEquals(1, store.nextOrderSequence())
        assertEquals(2, RoomShopStore(db).nextOrderSequence())
        store.save("budi", state())
        store.clear()
        assertNull(store.load("budi"))
        assertEquals(1, store.nextOrderSequence())
    }

    @Test fun shopSessionMembacaUlangDariDatabaseSetelahKill() {
        fun open() = ShopSession({ 10 }, { 1_000L }, RoomShopStore(db)).also { it.applyUser("standard_user", "Standard User", "s@x.id", isNew = false) }
        val before = open().apply {
            addToCart(line)
            toggleWishlist("P060")
            addHistory("kaos")
            setVoucher("ONGKIR")
            debit(50_000, "Transfer ke Budi")
            newOrder { id -> Order(id, listOf(line), 158_000, PayMethod.CARD, "Visa", OrderStatus.PAID) }
            checkPin("000000")
        }
        val after = open()
        assertEquals(before.cart.value, after.cart.value)
        assertEquals(before.wishlist.value, after.wishlist.value)
        assertEquals(before.history.value, after.history.value)
        assertEquals("ONGKIR", after.voucher.value)
        assertEquals(before.balance.value, after.balance.value)
        assertEquals(before.transactions.value, after.transactions.value)
        assertEquals(before.orders.value, after.orders.value)
        assertEquals(1, db.dao().account("standard_user")!!.pinFailures)
    }

    @Test fun authSeedHashLoginDanSesiBertahan() = runBlocking {
        val auth = AuthRepository(context.assets, db.dao())
        val rows = db.dao().users()
        assertEquals(5, rows.size)
        assertTrue(rows.none { "Practice123!" in it.passwordHash })
        assertTrue(auth.login("standard_user", "Practice123!") is LoginResult.Success)
        assertEquals(LoginResult.Invalid, auth.login("standard_user", "salah"))
        assertEquals(LoginResult.Locked, auth.login("locked_user", "Practice123!"))

        assertEquals("standard_user", AuthRepository(context.assets, db.dao()).currentUser?.username)
        auth.logout()
        assertNull(AuthRepository(context.assets, db.dao()).currentUser)
    }

    @Test fun registerDanGantiPasswordTersimpan() = runBlocking {
        val auth = AuthRepository(context.assets, db.dao())
        assertNotNull(auth.register("Tester", "tester@x.id", "Tester123"))
        assertNull(auth.register("Lagi", "tester@x.id", "Tester123"))
        assertTrue(auth.changePassword("tester@x.id", "Baru4567"))

        val reopened = AuthRepository(context.assets, db.dao())
        assertEquals(6, db.dao().users().size)
        assertEquals(LoginResult.Invalid, reopened.login("tester@x.id", "Tester123"))
        assertTrue(reopened.login("tester@x.id", "Baru4567") is LoginResult.Success)
    }

    @Test fun stokYangBerubahDibacaUlang() {
        val catalog = CatalogRepository(context.assets, db.dao())
        val p = catalog.products.first { it.variants.any { v -> v.stock > 2 } }
        val v = p.variants.first { it.stock > 2 }
        catalog.adjustStock(CartLine(p.id, v.color, v.size, 2), -2)
        val reopened = CatalogRepository(context.assets, db.dao())
        assertEquals(v.stock - 2, reopened.product(p.id)!!.stockOf(v.color, v.size))
        assertFalse(db.dao().stock().isEmpty())
    }
}
