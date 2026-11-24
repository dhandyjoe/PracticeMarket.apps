package com.automation.practice.data

import com.automation.practice.data.repo.INITIAL_BALANCE
import com.automation.practice.data.repo.MemoryShopStore
import com.automation.practice.data.repo.Outcome
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.data.repo.Stock
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.MAX_TOPUP
import com.automation.practice.domain.MIN_TOPUP
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aturan tulis contract §8.3 lewat [ShopSession] + store memori (perilaku sama dengan transaksi Firestore). */
class ShopSessionWalletTest {
    private val line = CartLine("P001", "Hitam", "L", 2)
    private val store = MemoryShopStore(mapOf("P001" to mapOf("Hitam|L" to 3)))
    private var lastStock: Stock? = null
    private fun session(uid: String = "u") =
        ShopSession({ 10 }, { 1_000L }, store, onStock = { lastStock = it }).also { runBlocking { it.load(uid, "U", "u@x.id") } }
    private suspend fun ShopSession.order(method: PayMethod, status: OrderStatus, total: Long = 158_000, lines: List<CartLine> = listOf(line)) =
        placeOrder(Order("", lines, total, method, "m", status))

    @Test fun `spend mengurangi saldo dan mencatat transaksi keluar`() = runBlocking {
        val s = session()
        assertTrue(s.spend(150_000, "Transfer ke Budi") is Outcome.Done)
        assertEquals(INITIAL_BALANCE - 150_000, s.balance.value)
        assertEquals(-150_000L, s.transactions.value.first().amount)
        assertEquals(9, s.transactions.value.size)
    }

    @Test fun `spend ditolak bila saldo kurang dan saldo tidak berubah`() = runBlocking {
        val s = session()
        assertEquals(Outcome.NoBalance, s.spend(INITIAL_BALANCE + 1, "x"))
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertEquals(8, s.transactions.value.size)
    }

    @Test fun `topUp menambah saldo dan mencatat transaksi masuk`() = runBlocking {
        val s = session()
        assertTrue(s.topUp(100_000, "Top Up Saldo") is Outcome.Done)
        assertEquals(INITIAL_BALANCE + 100_000, s.balance.value)
        assertEquals(100_000L, s.transactions.value.first().amount)
        assertEquals(9, s.transactions.value.size)
    }

    @Test fun `topUp di luar batas ditolak dan saldo tidak berubah`() = runBlocking {
        val s = session()
        assertEquals(Outcome.Failed, s.topUp(MIN_TOPUP - 1, "x"))
        assertEquals(Outcome.Failed, s.topUp(MAX_TOPUP + 1, "x"))
        assertTrue(s.topUp(MAX_TOPUP, "x") is Outcome.Done)
        assertEquals(INITIAL_BALANCE + MAX_TOPUP, s.balance.value)
    }

    @Test fun `pesanan mengurangi stok global dan stok baru diteruskan ke katalog`() = runBlocking {
        val s = session()
        val o = (s.order(PayMethod.CARD, OrderStatus.PAID) as Outcome.Done).value
        assertEquals(mapOf("P001" to mapOf("Hitam|L" to 1)), lastStock)
        assertEquals(o, s.orders.value.single())
        assertEquals(mapOf(OrderStatus.PAID to 1_000L), o.stamps)
        assertEquals(INITIAL_BALANCE, s.balance.value)
    }

    @Test fun `pesanan gagal bila stok varian kurang dan tidak ada yang berubah`() = runBlocking {
        val s = session()
        val r = s.order(PayMethod.WALLET, OrderStatus.PAID, lines = listOf(line.copy(qty = 4)))
        assertEquals(Outcome.NoStock("P001"), r)
        assertTrue(s.orders.value.isEmpty())
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertNull(lastStock)
    }

    @Test fun `pesanan PracticePay mengurangi saldo atau gagal bila saldo kurang`() = runBlocking {
        val s = session()
        assertEquals(Outcome.NoBalance, s.order(PayMethod.WALLET, OrderStatus.PAID, total = INITIAL_BALANCE + 1))
        s.order(PayMethod.WALLET, OrderStatus.PAID)
        assertEquals(INITIAL_BALANCE - 158_000, s.balance.value)
        assertEquals("Belanja PracticeMart", s.transactions.value.first().title)
    }

    @Test fun `pesanan PracticePay yang Selesai memberi cashback sekali walau dua client memajukan`() = runBlocking {
        val a = session()
        val o = (a.order(PayMethod.WALLET, OrderStatus.SHIPPED) as Outcome.Done).value
        val b = session() // device kedua dengan cache lama: status SHIPPED
        assertEquals(OrderStatus.DONE, a.advance(o.id))
        assertNull(b.advance(o.id))
        assertEquals(OrderStatus.DONE, b.order(o.id)?.status)
        assertEquals(INITIAL_BALANCE - 158_000 + 7_900, session().balance.value)
        assertEquals(158_000L, a.order(o.id)?.total)
        assertNull(a.advance(o.id))
    }

    @Test fun `pesanan non PracticePay yang Selesai tidak memberi cashback`() = runBlocking {
        val s = session()
        val o = (s.order(PayMethod.CARD, OrderStatus.SHIPPED) as Outcome.Done).value
        s.advance(o.id)
        assertEquals(INITIAL_BALANCE, s.balance.value)
    }

    @Test fun `timeline mencatat waktu tiap status`() = runBlocking {
        val s = session()
        val o = (s.order(PayMethod.CARD, OrderStatus.WAITING) as Outcome.Done).value
        s.advance(o.id)
        assertEquals(setOf(OrderStatus.WAITING, OrderStatus.PAID), s.order(o.id)?.stamps?.keys)
        assertEquals(1_000L, s.order(o.id)?.statusAt)
    }

    @Test fun `batal dua kali mengembalikan stok sekali`() = runBlocking {
        val s = session()
        val o = (s.order(PayMethod.CARD, OrderStatus.PAID) as Outcome.Done).value
        s.cancelOrder(o.id, "Berubah pikiran")
        session().cancelOrder(o.id, "lagi")
        assertEquals(mapOf("P001" to mapOf("Hitam|L" to 3)), store.loadStock())
        assertEquals("Berubah pikiran", session().order(o.id)?.cancelReason)
    }

    @Test fun `pesanan Dikirim tidak bisa dibatalkan`() = runBlocking {
        val s = session()
        val o = (s.order(PayMethod.CARD, OrderStatus.SHIPPED) as Outcome.Done).value
        assertEquals(OrderStatus.SHIPPED, (s.cancelOrder(o.id) as Outcome.Done).value.status)
        assertEquals(mapOf("P001" to mapOf("Hitam|L" to 1)), store.loadStock())
    }

    @Test fun `hanya satu alamat utama dan alamat pertama jadi utama bila tidak ada`() {
        val s = session()
        s.saveAddress(Address("", "Kos", "U", "081", "Bali", "Denpasar", "Jl A", "80111", true))
        assertEquals(listOf("Kos"), s.addresses.value.filter { it.primary }.map { it.label })
        s.addresses.value.forEach { s.deleteAddress(it.id) }
        s.saveAddress(Address("", "Baru", "U", "081", "Bali", "Denpasar", "Jl B", "80112", false))
        assertEquals(listOf("Baru"), s.addresses.value.filter { it.primary }.map { it.label })
        assertEquals(s.addresses.value, session().addresses.value)
    }
}
