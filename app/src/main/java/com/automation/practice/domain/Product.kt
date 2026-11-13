package com.automation.practice.domain

data class Category(val slug: String, val name: String)

/** Satu kombinasi warna × ukuran. Produk tanpa warna/ukuran memakai string kosong. */
data class Variant(val color: String, val size: String, val stock: Int)

data class Product(
    val id: String,
    val name: String,
    val categorySlug: String,
    val price: Long,
    val originalPrice: Long?,
    val rating: Double,
    val colors: List<String>,
    val sizes: List<String>,
    val variants: List<Variant>,
) {
    val inStock get() = variants.any { it.stock > 0 }
    val discountPercent get() = originalPrice?.let { ((it - price) * 100 / it).toInt() }

    fun stockOf(color: String, size: String) =
        variants.firstOrNull { it.color == color && it.size == size }?.stock ?: 0

    /** Kombinasi pertama yang masih ada stoknya; null bila produk habis. */
    fun firstAvailable() = variants.firstOrNull { it.stock > 0 }
}

/** [saved]: "Simpan untuk nanti" (tidak dihitung, tidak bisa dipilih). */
data class CartLine(
    val productId: String,
    val color: String,
    val size: String,
    val qty: Int,
    val selected: Boolean = true,
    val saved: Boolean = false,
) {
    fun sameItem(o: CartLine) = productId == o.productId && color == o.color && size == o.size
}

/** Maksimal pembelian per produk (FR-SHOP-09). */
const val MAX_QTY = 10

fun Long.toRupiah(nbsp: Boolean = false): String {
    val digits = toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp" + (if (nbsp) " " else "") + digits
}
