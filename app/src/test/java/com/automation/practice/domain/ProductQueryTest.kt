package com.automation.practice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductQueryTest {
    private fun product(id: String, name: String, cat: String, price: Long, rating: Double, stock: Int = 5, orig: Long? = null) =
        Product(id, name, cat, price, orig, rating, emptyList(), emptyList(), listOf(Variant("", "", stock)))

    private val catalog = listOf(
        product("P001", "Kaos Polos Basic", "fashion", 75_000, 4.8),
        product("P003", "Kaos Oversize", "fashion", 99_000, 4.6),
        product("P004", "Kaos Raglan", "fashion", 119_000, 4.5),
        product("P010", "Headphone Nirkabel", "elektronik", 850_000, 4.6),
        product("P060", "Buku Catatan A5", "buku", 15_000, 4.0, stock = 0),
    )

    private fun ids(f: ProductFilter, glitch: Boolean = false) = catalog.applyFilter(f, glitch).map { it.id }

    @Test fun `query cocok dengan sebagian nama tanpa peduli huruf besar`() {
        assertEquals(listOf("P001", "P003", "P004"), ids(ProductFilter(query = "KAOS")))
    }

    @Test fun `filter kategori, harga, rating, dan stok digabung`() {
        val f = ProductFilter(categories = setOf("fashion"), minPrice = 80_000, minRating = 4.5, inStockOnly = true)
        assertEquals(listOf("P003", "P004"), ids(f))
        assertTrue(ids(ProductFilter(inStockOnly = true)).none { it == "P060" })
    }

    @Test fun `urutan harga dan rating`() {
        assertEquals(listOf("P060", "P001", "P003", "P004", "P010"), ids(ProductFilter(sort = Sort.PRICE_ASC)))
        assertEquals("P010", ids(ProductFilter(sort = Sort.PRICE_DESC)).first())
        assertEquals("P001", ids(ProductFilter(sort = Sort.RATING)).first())
        assertEquals("P060", ids(ProductFilter(sort = Sort.NEWEST)).first())
    }

    @Test fun `bug glitch menukar dua item terakhir hanya untuk harga terendah`() {
        val f = ProductFilter(sort = Sort.PRICE_ASC)
        assertEquals(listOf("P060", "P001", "P003", "P010", "P004"), ids(f, glitch = true))
        assertEquals(ids(f), ids(f, glitch = false).also { assertEquals(listOf("P060", "P001", "P003", "P004", "P010"), it) })
        assertEquals(ids(ProductFilter(sort = Sort.PRICE_DESC)), ids(ProductFilter(sort = Sort.PRICE_DESC), glitch = true))
    }

    @Test fun `jumlah filter aktif tidak menghitung urutan`() {
        assertEquals(0, ProductFilter(sort = Sort.PRICE_ASC).activeCount)
        val all = ProductFilter(categories = setOf("fashion"), minPrice = 1, minRating = 3.0, inStockOnly = true)
        assertEquals(4, all.activeCount)
        assertEquals(1, ProductFilter(maxPrice = 200_000).activeCount)
    }

    @Test fun `diskon dan stok varian`() {
        val p = product("P002", "Sepatu", "olahraga", 450_000, 4.7, orig = 600_000)
        assertEquals(25, p.discountPercent)
        assertTrue(p.inStock)
        assertFalse(product("P060", "Buku", "buku", 15_000, 4.0, stock = 0).inStock)
        assertEquals(5, p.stockOf("", ""))
    }

    @Test fun `format rupiah tanpa spasi kecuali glitch`() {
        assertEquals("Rp158.000", 158_000L.toRupiah())
        assertEquals("Rp1.250.000", 1_250_000L.toRupiah())
        assertEquals("Rp0", 0L.toRupiah())
        assertEquals("Rp 75.000", 75_000L.toRupiah(nbsp = true))
    }
}
