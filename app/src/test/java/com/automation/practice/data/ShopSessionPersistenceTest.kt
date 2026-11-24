package com.automation.practice.data

import com.automation.practice.data.repo.INITIAL_BALANCE
import com.automation.practice.data.repo.MemoryShopStore
import com.automation.practice.data.repo.Outcome
import com.automation.practice.data.repo.PinCheck
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.data.repo.ShopStore
import com.automation.practice.data.repo.UserState
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ShopSession baru di atas store yang sama = app dibuka lagi setelah kill, atau device lain (PRD EDGE-01, contract §7). */
class ShopSessionPersistenceTest {
    private val store = MemoryShopStore(mapOf("P001" to mapOf("Hitam|L" to 10), "P002" to mapOf("|42" to 3)))
    private val line = CartLine("P001", "Hitam", "L", 2)
    private fun open(uid: String = "uid-a") =
        ShopSession({ 10 }, { 1_000L }, store).also { runBlocking { it.load(uid, "Andi", "andi@x.id") } }

    @Test fun `login pertama menulis data awal contract 4_5 sekali`() {
        val s = open()
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertEquals((8 downTo 1).map(Int::toString), s.transactions.value.map { it.id })
        assertEquals(listOf("Rumah", "Kantor"), s.addresses.value.map { it.label })
        assertTrue(s.addresses.value.all { it.recipient == "Andi" })
        assertEquals(listOf("P001", "P010", "P002", "P020"), s.wishlist.value)
        assertEquals(listOf("sepatu lari", "headphone", "buku catatan", "smartwatch"), s.history.value)
        assertTrue(s.cart.value.isEmpty() && s.orders.value.isEmpty() && s.voucher.value == null)
        assertEquals(8, open().transactions.value.size)
    }

    @Test fun `semua data akun pulih setelah kill`() = runBlocking {
        val before = open().apply {
            addToCart(line)
            toggleSaved(CartLine("P002", "", "42", 1).also(::addToCart))
            toggleWishlist("P060")
            addHistory("kaos")
            setVoucher("HEMAT10")
            spend(20_000, "Transfer ke Budi")
            updateProfile(Profile("Baru", "baru@x.id", "2000-01-02", "L", null))
            saveAddress(Address("", "Kos", "Baru", "081", "Bali", "Denpasar", "Jl C", "80113", false))
            val o = (placeOrder(Order("", listOf(line), 158_000, PayMethod.VA, "VA BCA", OrderStatus.WAITING, vaDeadline = 9_000)) as Outcome.Done).value
            advance(o.id)
        }
        val after = open()
        assertEquals(before.cart.value, after.cart.value)
        assertEquals(2, after.badge.value)
        assertEquals(before.wishlist.value, after.wishlist.value)
        assertEquals(before.history.value, after.history.value)
        assertEquals("HEMAT10", after.voucher.value)
        assertEquals(before.balance.value, after.balance.value)
        assertEquals(before.transactions.value, after.transactions.value)
        assertEquals(before.profile.value, after.profile.value)
        assertEquals(before.addresses.value, after.addresses.value)
        assertEquals(before.orders.value, after.orders.value)
        assertEquals(OrderStatus.PAID, after.orders.value.single().status)
    }

    @Test fun `data tiap akun terpisah`() {
        open("uid-a").addToCart(line)
        val other = open("uid-b")
        assertTrue(other.cart.value.isEmpty())
        assertEquals(1, open("uid-a").cart.value.size)
    }

    @Test fun `Order ID berlanjut per akun setelah kill`() = runBlocking {
        fun draft() = Order("", listOf(CartLine("P001", "Hitam", "L", 1)), 1, PayMethod.CARD, "m", OrderStatus.PAID)
        open().placeOrder(draft())
        assertEquals("PM-20260115-0002", (open().placeOrder(draft()) as Outcome.Done).value.id)
        assertEquals("PM-20260115-0001", (open("uid-b").placeOrder(draft()) as Outcome.Done).value.id)
    }

    @Test fun `PIN benar diterima dan salah 3x mengunci walau app dibuka ulang`() {
        assertEquals(PinCheck.Ok, open().checkPin("123456"))
        assertEquals(PinCheck.Wrong(2), open().checkPin("000000"))
        assertEquals(PinCheck.Wrong(1), open().checkPin("000000"))
        assertTrue(open().checkPin("000000") is PinCheck.Locked)
        val reopened = open()
        assertTrue(reopened.pinLockSeconds() > 0)
        assertTrue(reopened.checkPin("123456") is PinCheck.Locked)
    }

    // G-01: dua device membuka akun yang sama dan tidak saling memuat ulang; tiap edit harus jatuh di data terbaru store.
    @Test fun `edit dari dua sesi basi saling menambah, bukan saling menimpa`() {
        val a = open()
        val b = open()
        a.addToCart(line)
        b.addToCart(CartLine("P002", "", "42", 1))
        a.toggleWishlist("P060")
        b.toggleWishlist("P061")
        a.addHistory("kaos")
        b.addHistory("celana")
        val c = open()
        assertEquals(listOf("P001", "P002"), c.cart.value.map { it.productId })
        assertTrue("P060" in c.wishlist.value && "P061" in c.wishlist.value)
        assertEquals(listOf("celana", "kaos"), c.history.value.take(2))
    }

    @Test fun `alamat baru dari dua sesi basi tidak kembar id`() {
        val a = open()
        val b = open()
        a.saveAddress(Address("", "Kos", "A", "081", "Bali", "Denpasar", "Jl A", "80113", false))
        b.saveAddress(Address("", "Villa", "B", "082", "Bali", "Ubud", "Jl B", "80571", false))
        val ids = open().addresses.value.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(4, ids.size)
    }

    // G-12: id alamat string seperti web; alamat yang diubah tidak pindah urutan dan tidak ganti id.
    @Test fun `id alamat string tetap dan alamat baru berformat addr-ms`() {
        val s = open()
        assertEquals(listOf("home", "office"), s.addresses.value.map { it.id })
        s.saveAddress(Address("", "Kos", "A", "081", "Bali", "Denpasar", "Jl A", "80113", false))
        assertEquals(listOf("home", "office", "addr-1000"), s.addresses.value.map { it.id })
        s.saveAddress(s.addresses.value.first { it.id == "office" }.copy(street = "Jl. Baru"))
        assertEquals(listOf("home", "office", "addr-1000"), open().addresses.value.map { it.id })
        assertEquals("Jl. Baru", open().addresses.value[1].street)
    }

    @Test fun `urutan wishlist tidak menghidupkan lagi item yang dihapus device lain`() {
        val a = open()
        val b = open()
        b.toggleWishlist("P010")
        a.setWishlistOrder(a.wishlist.value.reversed())
        assertEquals(listOf("P020", "P002", "P001"), open().wishlist.value)
    }

    // G-02: data dari cache lokal harus bisa dikenali UI.
    @Test fun `stale menyala hanya bila store membaca dari cache`() {
        val inner = MemoryShopStore()
        val cached = object : ShopStore by inner {
            override suspend fun loadState(uid: String, seed: () -> UserState) = inner.loadState(uid, seed).copy(fromCache = true)
        }
        val fromServer = ShopSession({ 10 }, { 1_000L }, inner).also { runBlocking { it.load("uid-a", "Andi", "a@x.id") } }
        val fromCache = ShopSession({ 10 }, { 1_000L }, cached).also { runBlocking { it.load("uid-a", "Andi", "a@x.id") } }
        assertEquals(false, fromServer.stale.value)
        assertEquals(true, fromCache.stale.value)
    }

    // G-13: batas yang dijaga firestore.rules (validState) dijaga juga di client supaya tulisan tidak ditolak.
    @Test fun `alamat ke-21 dan baris cart ke-101 diabaikan`() {
        val s = open()
        repeat(25) { i -> s.saveAddress(Address("", "A$i", "Andi", "081", "Bali", "Denpasar", "Jl $i", "80113", false)) }
        assertEquals(20, s.addresses.value.size)
        repeat(105) { i -> s.addToCart(CartLine("P%03d".format(i), "", "", 1)) }
        assertEquals(100, s.cart.value.size)
        assertEquals(100, open().cart.value.size)
    }
}
