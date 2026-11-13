package com.automation.practice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderAndWalletTest {
    @Test fun `status maju berurutan sampai Selesai lalu berhenti`() {
        assertEquals(OrderStatus.PAID, OrderStatus.WAITING.next())
        assertEquals(OrderStatus.PACKED, OrderStatus.PAID.next())
        assertEquals(OrderStatus.SHIPPED, OrderStatus.PACKED.next())
        assertEquals(OrderStatus.DONE, OrderStatus.SHIPPED.next())
        assertNull(OrderStatus.DONE.next())
        assertNull(OrderStatus.CANCELLED.next())
    }

    @Test fun `batal hanya boleh bila status paling lambat Dikemas`() {
        assertTrue(OrderStatus.WAITING.cancellable && OrderStatus.PAID.cancellable && OrderStatus.PACKED.cancellable)
        assertFalse(OrderStatus.SHIPPED.cancellable || OrderStatus.DONE.cancellable || OrderStatus.CANCELLED.cancellable)
    }

    @Test fun `cashback 5 persen maksimal 10 ribu`() {
        assertEquals(7_900, cashback(158_000))
        assertEquals(10_000, cashback(1_271_000))
    }

    @Test fun `galat nominal sesuai PRD`() {
        assertEquals("Minimal transfer Rp10.000", amountError(9_999, 500_000))
        assertEquals("Saldo tidak cukup", amountError(500_001, 500_000))
        assertNull(amountError(10_000, 500_000))
        assertNull(amountError(500_000, 500_000))
        assertNull(amountError(0, 500_000))
    }

    @Test fun `parseAmount hanya mengambil digit dan membatasi panjang`() {
        assertEquals(150_000, parseAmount("Rp150.000"))
        assertEquals(0, parseAmount(""))
        assertEquals(123_456_789, parseAmount("1234567890123"))
    }

    @Test fun `cari kontak lewat nama atau nomor tanpa peduli huruf besar`() {
        assertEquals(listOf("Budi Santoso"), SEED_CONTACTS.search("budi").map { it.name })
        assertEquals(listOf("Budi Santoso"), SEED_CONTACTS.search("08123456789").map { it.name })
        assertTrue(SEED_CONTACTS.search("zzz").isEmpty())
        assertEquals(SEED_CONTACTS.size, SEED_CONTACTS.search("  ").size)
    }

    @Test fun `merchant QR dikenali dari id tanpa peduli huruf besar`() {
        assertEquals(45_000L, merchantOf("tokobuku")?.fixedAmount)
        assertNull(merchantOf("KOPIPAGI")?.fixedAmount)
        assertNull(merchantOf("XYZ"))
        assertNull(merchantOf(null))
    }

    @Test fun `riwayat lama berjumlah 52 dan menjadi 3 halaman bersama seed`() {
        val all = historyBacklog()
        assertEquals(52, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.at < jakartaTime(2026, 1, 12, 9, 0) })
        assertEquals(HISTORY_PAGE, filterHistory(all, 0).take(HISTORY_PAGE).size)
    }

    @Test fun `filter riwayat masuk keluar dan rentang tanggal`() {
        val a = Tx(1, "in", 100, 1_000)
        val b = Tx(2, "out", -50, 2_000)
        val c = Tx(3, "in2", 10, 3_000)
        assertEquals(listOf(3, 2, 1), filterHistory(listOf(a, b, c), 0).map { it.id })
        assertEquals(listOf(3, 1), filterHistory(listOf(a, b, c), 1).map { it.id })
        assertEquals(listOf(2), filterHistory(listOf(a, b, c), 2).map { it.id })
        assertEquals(listOf(2), filterHistory(listOf(a, b, c), 0, 1_500..2_500L).map { it.id })
    }

    @Test fun `kode pos harus tepat 5 digit`() {
        assertTrue(isPostalValid("10220"))
        assertFalse(isPostalValid("1022"))
        assertFalse(isPostalValid("102201"))
        assertFalse(isPostalValid("1022a"))
    }

    @Test fun `tanggal lahir bolak-balik ISO dan format Indonesia`() {
        assertEquals("1998-08-17", utcMillisToIso(isoToUtcMillis("1998-08-17")!!))
        assertNull(isoToUtcMillis("17-08-1998"))
        val t = jakartaTime(2026, 1, 15, 9, 12, 7)
        assertEquals("15 Januari 2026", formatDate(t))
        assertEquals("09:12", formatHm(t))
        assertEquals("09:12:07", formatHms(t))
    }

    @Test fun `semua provinsi punya kota dan seed alamat memuat Jakarta`() {
        assertTrue(REGIONS.values.all { it.isNotEmpty() })
        assertTrue(REGIONS.getValue(MOCK_PROVINCE).contains(MOCK_CITY))
        assertEquals(1, SEED_ADDRESSES.count { it.primary })
    }
}
