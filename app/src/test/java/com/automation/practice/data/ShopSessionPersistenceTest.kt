package com.automation.practice.data

import com.automation.practice.data.repo.MemoryShopStore
import com.automation.practice.data.repo.PinCheck
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ShopSession baru di atas store yang sama = app dibuka lagi setelah kill (PRD EDGE-01, FR-SHOP-17). */
class ShopSessionPersistenceTest {
    private val store = MemoryShopStore()
    private val line = CartLine("P001", "Hitam", "L", 2)
    private fun open(user: String = "standard_user") =
        ShopSession({ 10 }, { 1_000L }, store).also { it.applyUser(user, "Standard User", "s@x.id", isNew = false) }

    @Test fun `semua data akun pulih setelah kill`() {
        val before = open().apply {
            addToCart(line)
            toggleSaved(CartLine("P002", "", "42", 1).also(::addToCart))
            toggleWishlist("P060")
            addHistory("kaos")
            setVoucher("HEMAT10")
            debit(20_000, "Transfer ke Budi")
            updateProfile(Profile("Baru", "baru@x.id", "2000-01-02", "L", null))
            saveAddress(Address(0, "Kos", "Baru", "081", "Bali", "Denpasar", "Jl C", "80113", false))
            newOrder { id -> Order(id, listOf(line), 158_000, PayMethod.VA, "VA BCA", OrderStatus.WAITING, vaDeadline = 9_000) }
            advance(orders.value.first().id)
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
    }

    @Test fun `data tiap akun terpisah`() {
        open("standard_user").addToCart(line)
        val other = open("glitch_user")
        assertTrue(other.cart.value.isEmpty())
        assertEquals(1, open("standard_user").cart.value.size)
    }

    @Test fun `Order ID tetap berlanjut setelah kill`() {
        open().newOrder { id -> Order(id, listOf(line), 1, PayMethod.CARD, "m", OrderStatus.PAID) }
        val second = open().newOrder { id -> Order(id, listOf(line), 1, PayMethod.CARD, "m", OrderStatus.PAID) }
        assertEquals("PM-20260115-0002", second.id)
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
}
