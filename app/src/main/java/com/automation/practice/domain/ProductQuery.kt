package com.automation.practice.domain

enum class Sort { RELEVANT, PRICE_ASC, PRICE_DESC, RATING, NEWEST }

const val PRICE_MAX = 2_000_000L

data class ProductFilter(
    val query: String = "",
    val categories: Set<String> = emptySet(),
    val minPrice: Long = 0,
    val maxPrice: Long = PRICE_MAX,
    val minRating: Double = 0.0,
    val inStockOnly: Boolean = false,
    val sort: Sort = Sort.RELEVANT,
) {
    /** Jumlah filter aktif untuk badge; urutan tidak dihitung. */
    val activeCount
        get() = listOf(
            categories.isNotEmpty(),
            minPrice > 0 || (maxPrice < PRICE_MAX),
            minRating > 0,
            inStockOnly,
        ).count { it }
}

/**
 * Saring + urutkan katalog. [glitch] meniru bug `glitch_user` (PRD §8.8):
 * urutan "Harga Terendah" salah untuk dua item terakhir.
 */
fun List<Product>.applyFilter(f: ProductFilter, glitch: Boolean = false): List<Product> {
    val q = f.query.trim().lowercase()
    val matched = filter {
        (q.isEmpty() || q in it.name.lowercase()) &&
            (f.categories.isEmpty() || it.categorySlug in f.categories) &&
            it.price in f.minPrice..f.maxPrice &&
            it.rating >= f.minRating &&
            (!f.inStockOnly || it.inStock)
    }
    val sorted = when (f.sort) {
        // sortedBy stabil: awalan nama yang cocok naik, sisanya tetap urutan katalog.
        Sort.RELEVANT -> matched.sortedBy { if (q.isNotEmpty() && it.name.lowercase().startsWith(q)) 0 else 1 }
        Sort.PRICE_ASC -> matched.sortedBy { it.price }
        Sort.PRICE_DESC -> matched.sortedByDescending { it.price }
        Sort.RATING -> matched.sortedByDescending { it.rating }
        Sort.NEWEST -> matched.sortedByDescending { it.id } // id lebih besar = lebih baru
    }
    return if (glitch && f.sort == Sort.PRICE_ASC && sorted.size >= 2) {
        sorted.dropLast(2) + sorted.takeLast(2).reversed()
    } else sorted
}
