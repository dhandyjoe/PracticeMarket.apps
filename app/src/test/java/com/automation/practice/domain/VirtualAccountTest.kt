package com.automation.practice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vektor sama dengan web (`payment.test.ts`): nomor VA, bank dari label, dan aturan checkout Same Day. */
class VirtualAccountTest {
    @Test fun `nomor VA bank selain BCA = awalan bank + digit Order ID`() {
        assertEquals("8808 2026 0115 0003", vaNumber("PM-20260115-0003", "BNI"))
        assertEquals("8900 2026 1120 0012", vaNumber("PM-20261120-0012", "Mandiri"))
    }

    @Test fun `BCA pada Order ID deterministik tetap 8077 0000 1234 5678`() {
        assertEquals("8077 0000 1234 5678", vaNumber("PM-20260115-0007", "BCA"))
        assertEquals("8077 2026 1120 0012", vaNumber("PM-20261120-0012", "BCA"))
    }

    @Test fun `bank dibaca dari label baru maupun lama`() {
        assertEquals("BNI", vaBankOf("Virtual Account BNI"))
        assertEquals("Mandiri", vaBankOf("Mandiri Virtual Account"))
        assertEquals("BCA", vaBankOf("Virtual Account BCA"))
        assertEquals("BCA", vaBankOf("Virtual Account"))
    }

    @Test fun `Same Day hanya untuk DKI Jakarta`() {
        assertTrue(courierAvailable(Courier.SAME_DAY, "DKI Jakarta"))
        assertFalse(courierAvailable(Courier.SAME_DAY, "Jawa Barat"))
        assertTrue(courierAvailable(Courier.EXPRESS, "Jawa Barat"))
        assertTrue(courierAvailable(Courier.REGULER, ""))
    }

    @Test fun `jam kirim Same Day 09_00 sampai 17_00`() {
        assertTrue(isDeliveryTimeValid("09:00") && isDeliveryTimeValid("12:30") && isDeliveryTimeValid("17:00"))
        assertFalse(isDeliveryTimeValid("08:59") || isDeliveryTimeValid("17:01") || isDeliveryTimeValid("") || isDeliveryTimeValid("9:00"))
    }

    @Test fun `judul transaksi tetap Indonesia`() {
        assertEquals("Top Up Saldo", TX_TOPUP_TITLE)
        assertEquals("Transfer ke Budi", transferTitle("Budi"))
        assertEquals("Bayar QR · Kopi Pagi", qrTitle("Kopi Pagi"))
    }
}
