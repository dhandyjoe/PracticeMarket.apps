package com.automation.practice.data

import com.automation.practice.data.repo.INITIAL_BALANCE
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopSessionWalletTest {
    private val line = CartLine("P001", "Hitam", "L", 2)
    private fun session() = ShopSession({ 10 }, { 1_000L })
    private fun ShopSession.order(method: PayMethod, status: OrderStatus, total: Long = 158_000) =
        newOrder { id -> Order(id, listOf(line), total, method, "m", status) }

    @Test fun `debit mengurangi saldo dan mencatat transaksi keluar`() {
        val s = session()
        assertTrue(s.debit(150_000, "Transfer ke Budi", syncHome = true))
        assertEquals(INITIAL_BALANCE - 150_000, s.balance.value)
        assertEquals(s.balance.value, s.homeBalance.value)
        assertEquals(-150_000L, s.transactions.value.first().amount)
        assertEquals(9, s.transactions.value.size)
    }

    @Test fun `debit ditolak bila saldo kurang dan saldo tidak berubah`() {
        val s = session()
        assertFalse(s.debit(INITIAL_BALANCE + 1))
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertEquals(8, s.transactions.value.size)
    }

    @Test fun `bug glitch_user saldo Beranda tertinggal tapi saldo dompet benar`() {
        val s = session()
        s.debit(100_000, syncHome = false)
        assertEquals(INITIAL_BALANCE - 100_000, s.balance.value)
        assertEquals(INITIAL_BALANCE, s.homeBalance.value)
    }

    @Test fun `pesanan PracticePay yang Selesai memberi cashback tanpa mengubah total`() {
        val s = session()
        val o = s.order(PayMethod.WALLET, OrderStatus.SHIPPED)
        assertEquals(OrderStatus.DONE, s.advance(o.id))
        assertEquals(INITIAL_BALANCE + 7_900, s.balance.value)
        assertEquals(158_000L, s.order(o.id)?.total)
        assertEquals(null, s.advance(o.id))
    }

    @Test fun `pesanan non PracticePay yang Selesai tidak memberi cashback`() {
        val s = session()
        val o = s.order(PayMethod.CARD, OrderStatus.SHIPPED)
        s.advance(o.id)
        assertEquals(INITIAL_BALANCE, s.balance.value)
    }

    @Test fun `timeline mencatat waktu tiap status`() {
        val s = session()
        val o = s.order(PayMethod.CARD, OrderStatus.WAITING)
        s.advance(o.id)
        assertEquals(setOf(OrderStatus.WAITING, OrderStatus.PAID), s.order(o.id)?.stamps?.keys)
        assertEquals(1_000L, s.order(o.id)?.statusAt)
    }

    @Test fun `new_user mulai dengan saldo nol tanpa alamat dan riwayat`() {
        val s = session()
        s.applyUser("new_user", "New User", "new@contoh.id", isNew = true)
        assertEquals(0L, s.balance.value)
        assertTrue(s.addresses.value.isEmpty())
        assertTrue(s.transactions.value.isEmpty())
    }

    @Test fun `ganti ke akun biasa memulihkan saldo seed dan alamat atas nama akun`() {
        val s = session()
        s.applyUser("new_user", "New User", "n@x.id", isNew = true)
        s.applyUser("standard_user", "Standard User", "s@x.id", isNew = false)
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertEquals(2, s.addresses.value.size)
        assertTrue(s.addresses.value.all { it.recipient == "Standard User" })
    }

    @Test fun `login ulang akun yang sama tidak menghapus transaksi sesi`() {
        val s = session()
        s.applyUser("standard_user", "Standard User", "s@x.id", isNew = false)
        s.debit(10_000)
        s.applyUser("standard_user", "Standard User", "s@x.id", isNew = false)
        assertEquals(INITIAL_BALANCE - 10_000, s.balance.value)
    }

    @Test fun `hanya satu alamat utama dan alamat pertama jadi utama bila tidak ada`() {
        val s = session()
        s.applyUser("u", "U", "u@x.id", isNew = true)
        s.saveAddress(Address(0, "Rumah", "U", "081", "Bali", "Denpasar", "Jl A", "80111", false))
        assertTrue(s.addresses.value.single().primary)
        s.saveAddress(Address(0, "Kantor", "U", "081", "Bali", "Denpasar", "Jl B", "80112", true))
        assertEquals(listOf("Kantor"), s.addresses.value.filter { it.primary }.map { it.label })
        val first = s.addresses.value.first()
        s.deleteAddress(s.addresses.value.first { it.primary }.id)
        assertEquals(1, s.addresses.value.size)
        assertEquals(first.id, s.addresses.value.single().id)
    }

    @Test fun `reset mengosongkan pesanan keranjang dan memulihkan saldo`() {
        val s = session()
        s.applyUser("standard_user", "Standard User", "s@x.id", isNew = false)
        s.addToCart(line)
        s.order(PayMethod.WALLET, OrderStatus.PAID)
        s.debit(50_000)
        var restored = false
        s.reset { restored = true }
        assertTrue(restored)
        assertTrue(s.orders.value.isEmpty() && s.cart.value.isEmpty())
        assertEquals(INITIAL_BALANCE, s.balance.value)
        assertEquals("PM-20260115-0001", s.order(PayMethod.CARD, OrderStatus.WAITING).id)
    }
}
