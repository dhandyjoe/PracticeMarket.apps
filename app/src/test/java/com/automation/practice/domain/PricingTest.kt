package com.automation.practice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingTest {
    @Test fun `contoh PRD 2 kaos HEMAT10 express asuransi = Rp158000`() {
        val q = quote(150_000, "HEMAT10", Courier.EXPRESS, insured = true)
        assertEquals(15_000, q.discount)
        assertEquals(158_000, q.total)
    }

    @Test fun `HEMAT10 dibatasi 50000 dan dibulatkan ke bawah`() {
        assertEquals(50_000, quote(1_000_000, "HEMAT10", Courier.REGULER, false).discount)
        assertEquals(7, quote(79, "hemat10", Courier.REGULER, false).discount)
    }

    @Test fun `ONGKIR memotong maks 20000 dan Same Day tetap bayar 15000`() {
        assertEquals(10_000, quote(50_000, "ONGKIR", Courier.REGULER, false).discount)
        val sameDay = quote(50_000, "ONGKIR", Courier.SAME_DAY, false)
        assertEquals(15_000, sameDay.shipping - sameDay.discount)
    }

    @Test fun `MIN100K butuh subtotal minimal 100000`() {
        assertEquals("Minimal belanja Rp100.000", voucherError("MIN100K", 99_999))
        assertNull(voucherError("MIN100K", 100_000))
        assertEquals(15_000, quote(100_000, "MIN100K", Courier.REGULER, false).discount)
        assertEquals(0, quote(99_000, "MIN100K", Courier.REGULER, false).discount)
    }

    @Test fun `voucher kedaluwarsa dan tidak dikenal`() {
        assertEquals("Voucher sudah kedaluwarsa", voucherError(" expired ", 500_000))
        assertEquals("Voucher tidak ditemukan", voucherError("ABCDE", 500_000))
        assertEquals(0, quote(500_000, "EXPIRED", Courier.EXPRESS, false).discount)
    }

    @Test fun `tanpa voucher dan tanpa asuransi`() {
        val q = quote(75_000, null, Courier.REGULER, insured = false)
        assertEquals(0, q.discount)
        assertEquals(75_000 + 10_000 + 1_000, q.total)
    }

    @Test fun `pesanan ID deterministik`() {
        assertEquals("PM-20260115-0001", orderId(1))
        assertEquals("PM-20260115-0012", orderId(12))
    }

    @Test fun `batas COD`() {
        assertTrue(1_000_000L <= COD_LIMIT)
        assertFalse(1_000_001L <= COD_LIMIT)
    }
}
