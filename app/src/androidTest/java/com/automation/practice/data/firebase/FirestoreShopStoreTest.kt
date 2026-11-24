package com.automation.practice.data.firebase

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.automation.practice.data.repo.CatalogRepository
import com.automation.practice.data.repo.Outcome
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.data.repo.Stock
import com.automation.practice.data.repo.variantKey
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Aturan tulis contract §8.3 & Security Rules §8.4 terhadap Firebase Emulator Suite di laptop (ARCHITECTURE.md §15).
 * Jalankan `firebase emulators:start --only auth,firestore` dari `firebase/` dulu; emulator Android melihat laptop di 10.0.2.2.
 * Stok dipakai bersama antar test, jadi assertion stok selalu relatif.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreShopStoreTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalog = CatalogRepository(ctx.assets)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var store: FirestoreShopStore
    private lateinit var uid: String
    // qty 1: stok awal varian ini 10 dan dipakai bersama semua test.
    private val line = CartLine("P001", "Hitam", "L", 1)
    // Jam asli: lebih baru dari 8 transaksi awal (Januari 2026), jadi transaksi baru ada di urutan teratas.
    private val at = System.currentTimeMillis()

    @Before fun setUp() = runBlocking {
        // App terpisah per test: instance default dipakai app & tidak boleh diarahkan ke emulator setelah dipakai.
        val app = FirebaseApp.initializeApp(ctx, FirebaseApp.getInstance().options, "emu-${UUID.randomUUID()}")
        auth = FirebaseAuth.getInstance(app).apply { useEmulator(HOST, 9099) }
        db = FirebaseFirestore.getInstance(app).apply { useEmulator(HOST, 8080) }
        store = FirestoreShopStore(db, catalog::seedStock)
        uid = signUp()
    }

    private suspend fun signUp() = auth.createUserWithEmailAndPassword("t${UUID.randomUUID()}@contoh.id", "rahasia123").await().user!!.uid

    private suspend fun session(id: String = uid) = ShopSession({ 10 }, { at }, store).also { it.load(id, "Tes", "tes@contoh.id") }
    private suspend fun stockOf(l: CartLine): Int = store.loadStock()[l.productId]!![l.variantKey]!!
    private fun draft(method: PayMethod, status: OrderStatus, total: Long = 158_000, lines: List<CartLine> = listOf(line)) =
        Order("", lines, total, method, "m", status)

    @Test fun dataAwalAkunDibuatSekaliWalauDuaLoginBersamaan() = runBlocking {
        val loads = (1..2).map { async { session() } }.awaitAll()
        assertTrue(loads.all { it.balance.value == 500_000L })
        val txs = db.collection("states").document(uid).collection("transactions").get().await()
        assertEquals((1..8).map(Int::toString).toSet(), txs.documents.map { it.id }.toSet())
        assertEquals(listOf("Rumah", "Kantor"), session().addresses.value.map { it.label })
    }

    // G-12: web menulis id alamat string; dokumen lama app menyimpan angka. Keduanya harus terbaca unik.
    @Test fun idAlamatStringDariWebDanAngkaLamaTerbaca() = runBlocking {
        session()
        val addr = { id: Any, label: String -> mapOf("id" to id, "label" to label, "isPrimary" to false) }
        db.collection("states").document(uid).update("addresses", listOf(addr("home", "Rumah"), addr("office", "Kantor"), addr(3, "Kos"))).await()
        assertEquals(listOf("home", "office", "3"), session().addresses.value.map { it.id })
    }

    @Test fun stokGlobalDibuatDariStokAwalDanPunyaSemuaProduk() = runBlocking {
        val stock: Stock = store.loadStock()
        assertEquals(catalog.seedStock().keys, stock.keys)
        assertEquals(catalog.seedStock().mapValues { it.value.keys }, stock.mapValues { it.value.keys })
    }

    @Test fun fieldPerAkunTersimpanDenganUpdate() = runBlocking {
        session().apply {
            addToCart(line)
            toggleWishlist("P060")
            addHistory("kaos")
            setVoucher("HEMAT10")
            updateProfile(Profile("Baru", "tes@contoh.id", "2000-01-02", "L", null))
            saveAddress(Address("", "Kos", "Baru", "081", "Bali", "Denpasar", "Jl C", "80113", true))
            checkPin("000000")
        }
        // edit() per field membaca dulu baru menulis di coroutine terpisah: waitForPendingWrites saja bisa lolos sebelum update dikirim.
        for (i in 0 until 50) {
            val d = db.collection("states").document(uid).get().await()
            if ((d.get("addresses") as? List<*>)?.size == 3 && (d.get("history") as? List<*>)?.firstOrNull() == "kaos" &&
                (d.get("cart") as? List<*>)?.size == 1 && (d.get("wishlist") as? List<*>)?.contains("P060") == true) break
            delay(200)
        }
        db.waitForPendingWrites().await()
        val s = session()
        assertEquals(listOf(line), s.cart.value)
        assertTrue("P060" in s.wishlist.value)
        assertEquals("kaos", s.history.value.first())
        assertEquals("HEMAT10", s.voucher.value)
        assertEquals("Baru", s.profile.value.name)
        assertEquals(listOf("Kos"), s.addresses.value.filter { it.primary }.map { it.label })
        assertEquals(1L, db.collection("states").document(uid).get().await().getLong("pinFailures"))
    }

    @Test fun pesananMengurangiStokDanOrderSeqPerAkun() = runBlocking {
        val s = session()
        val before = stockOf(line)
        val first = (s.placeOrder(draft(PayMethod.CARD, OrderStatus.PAID)) as Outcome.Done).value
        val second = (s.placeOrder(draft(PayMethod.CARD, OrderStatus.PAID)) as Outcome.Done).value
        assertEquals("PM-20260115-0001", first.id)
        assertEquals("PM-20260115-0002", second.id)
        assertEquals(before - 2, stockOf(line))
        assertEquals(mapOf(OrderStatus.PAID to at), session().order(first.id)?.stamps)
        assertEquals(listOf(second.id, first.id), session().orders.value.map { it.id })
    }

    @Test fun pesananGagalBilaStokKurangTanpaMengubahApaPun() = runBlocking {
        val s = session()
        val empty = CartLine("P060", "", "", 1)
        assertEquals(Outcome.NoStock("P060"), s.placeOrder(draft(PayMethod.WALLET, OrderStatus.PAID, lines = listOf(empty))))
        assertEquals(0, stockOf(empty))
        val fresh = session()
        assertEquals(500_000L, fresh.balance.value)
        assertTrue(fresh.orders.value.isEmpty())
    }

    @Test fun pesananPracticePayMengurangiSaldoAtauGagalBilaKurang() = runBlocking {
        val s = session()
        assertEquals(Outcome.NoBalance, s.placeOrder(draft(PayMethod.WALLET, OrderStatus.PAID, total = 500_001)))
        assertTrue(s.placeOrder(draft(PayMethod.WALLET, OrderStatus.PAID)) is Outcome.Done)
        val fresh = session()
        assertEquals(342_000L, fresh.balance.value)
        assertEquals("Belanja PracticeMart", fresh.transactions.value.first().title)
        assertEquals(9, fresh.transactions.value.size)
    }

    @Test fun statusMajuSekaliDanCashbackSekaliWalauDuaClient() = runBlocking {
        val a = session()
        val o = (a.placeOrder(draft(PayMethod.WALLET, OrderStatus.SHIPPED)) as Outcome.Done).value
        val b = session()
        assertEquals(OrderStatus.DONE, a.advance(o.id))
        assertNull(b.advance(o.id))
        assertEquals(OrderStatus.DONE, b.order(o.id)?.status)
        val fresh = session()
        assertEquals(500_000L - 158_000 + 7_900, fresh.balance.value)
        assertEquals(1, fresh.transactions.value.count { it.title == "Cashback belanja" && it.at == at })
    }

    @Test fun batalDuaKaliMengembalikanStokSekali() = runBlocking {
        val s = session()
        val before = stockOf(line)
        val o = (s.placeOrder(draft(PayMethod.CARD, OrderStatus.PAID)) as Outcome.Done).value
        s.cancelOrder(o.id, "Berubah pikiran")
        session().cancelOrder(o.id, "lagi")
        assertEquals(before, stockOf(line))
        val saved = session().order(o.id)!!
        assertEquals(OrderStatus.CANCELLED, saved.status)
        assertEquals("Berubah pikiran", saved.cancelReason)
    }

    @Test fun pesananDikirimTidakBisaDibatalkan() = runBlocking {
        val s = session()
        val o = (s.placeOrder(draft(PayMethod.CARD, OrderStatus.SHIPPED)) as Outcome.Done).value
        val after = stockOf(line)
        assertEquals(OrderStatus.SHIPPED, (s.cancelOrder(o.id) as Outcome.Done).value.status)
        assertEquals(after, stockOf(line))
    }

    @Test fun transferMengurangiSaldoAtauDitolak() = runBlocking {
        val s = session()
        assertEquals(Outcome.NoBalance, s.spend(500_001, "Transfer ke Budi"))
        assertTrue(s.spend(150_000, "Transfer ke Budi") is Outcome.Done)
        val fresh = session()
        assertEquals(350_000L, fresh.balance.value)
        assertEquals(-150_000L, fresh.transactions.value.first().amount)
        assertEquals(at, fresh.transactions.value.first().at)
    }

    @Test fun rulesMenolakMembacaDataAkunLain() = runBlocking {
        session()
        auth.signOut()
        signUp()
        try {
            db.collection("states").document(uid).get(com.google.firebase.firestore.Source.SERVER).await()
            fail("data akun lain terbaca")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test fun rulesMenolakKeyStokBaru() = runBlocking {
        store.loadStock()
        try {
            db.collection("catalog").document("stock").update("stock.P999", mapOf("|" to 1)).await()
            fail("key produk baru diterima")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    // firestore.rules validState (contract §8.4): batas list yang juga dijaga client.
    @Test fun rulesMenolakCartLebihDari100Baris() = runBlocking {
        session()
        val over = List(101) { mapOf("productId" to "P001", "color" to "", "size" to "S$it", "qty" to 1, "selected" to true, "saved" to false) }
        try {
            db.collection("states").document(uid).update("cart", over).await()
            fail("cart 101 baris diterima")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test fun rulesMenolakAlamatLebihDari20() = runBlocking {
        session()
        val over = List(21) { mapOf("id" to it + 1, "label" to "Rumah", "isPrimary" to (it == 0)) }
        try {
            db.collection("states").document(uid).update("addresses", over).await()
            fail("21 alamat diterima")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val HOST = "10.0.2.2"
    }
}
