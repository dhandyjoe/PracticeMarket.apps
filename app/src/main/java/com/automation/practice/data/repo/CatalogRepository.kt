package com.automation.practice.data.repo

import android.content.res.AssetManager
import com.automation.practice.data.db.PracticeDao
import com.automation.practice.data.db.StockRow
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Category
import com.automation.practice.domain.Product
import com.automation.practice.domain.ProductFilter
import com.automation.practice.domain.Variant
import com.automation.practice.domain.applyFilter
import org.json.JSONArray
import org.json.JSONObject

/**
 * Katalog read-only dari assets/seed/catalog.json; stok yang berubah karena pesanan disimpan di tabel `stock`
 * sehingga tetap berkurang setelah kill (PRD EDGE-07).
 */
class CatalogRepository(assets: AssetManager, private val dao: PracticeDao) {
    val categories: List<Category>
    private val order: List<String>
    private val byId: MutableMap<String, Product>
    private val seed: Map<String, Product>
    val products: List<Product> get() = order.map { byId.getValue(it) }

    init {
        val root = JSONObject(assets.open(SEED).bufferedReader().use { it.readText() })
        categories = root.getJSONArray("categories").mapAs { o -> (o as JSONObject).let { Category(it.getString("slug"), it.getString("name")) } }
        val parsed = root.getJSONArray("products").mapAs { parseProduct(it as JSONObject) }
        order = parsed.map { it.id }
        byId = parsed.associateBy { it.id }.toMutableMap()
        seed = byId.toMap()
        dao.stock().forEach { setStock(it.productId, it.color, it.size, it.stock) }
    }

    /** "Reset semua data": stok kembali ke seed (tabel `stock` dikosongkan oleh pemanggil). */
    fun resetStock() { byId.clear(); byId.putAll(seed) }

    fun product(id: String) = byId[id]

    /** Stok berubah oleh pesanan (negatif = kurangi, positif = kembalikan); tidak pernah di bawah 0. */
    fun adjustStock(line: CartLine, delta: Int) {
        val current = byId[line.productId]?.stockOf(line.color, line.size) ?: return
        val next = (current + delta).coerceAtLeast(0)
        setStock(line.productId, line.color, line.size, next)
        dao.upsertStock(StockRow(line.productId, line.color, line.size, next))
    }

    private fun setStock(productId: String, color: String, size: String, stock: Int) {
        val p = byId[productId] ?: return
        byId[p.id] = p.copy(variants = p.variants.map { if (it.color == color && it.size == size) it.copy(stock = stock) else it })
    }
    fun categoryName(slug: String) = categories.firstOrNull { it.slug == slug }?.name.orEmpty()

    fun query(filter: ProductFilter, glitch: Boolean = false) = products.applyFilter(filter, glitch)

    /** Saran muncul mulai 2 karakter (FR-SHOP-04). */
    fun suggestions(q: String, limit: Int = 5): List<Product> {
        val key = q.trim().lowercase()
        return if (key.length < MIN_SUGGEST_CHARS) emptyList()
        else products.filter { key in it.name.lowercase() }.take(limit)
    }

    /** Acak tiap Beranda dibuka, atau urutan tetap untuk mode deterministik (FR-SHOP-02). */
    fun recommendations(shuffle: Boolean): List<Product> =
        if (shuffle) products.shuffled().take(RECOMMENDATION_COUNT)
        else FIXED_RECOMMENDATIONS.mapNotNull { byId[it] }

    private fun parseProduct(o: JSONObject): Product {
        val colors = o.getJSONArray("colors").mapAs { it as String }
        val sizes = o.getJSONArray("sizes").mapAs { it as String }
        val zero = o.getJSONArray("zero").mapAs { it as String }.toSet()
        val stock = o.getInt("stock")
        val variants = (colors.ifEmpty { listOf("") }).flatMap { c ->
            (sizes.ifEmpty { listOf("") }).map { s -> Variant(c, s, if ("$c|$s" in zero) 0 else stock) }
        }
        return Product(
            id = o.getString("id"),
            name = o.getString("name"),
            categorySlug = o.getString("cat"),
            price = o.getLong("price"),
            originalPrice = if (o.isNull("orig")) null else o.getLong("orig"),
            rating = o.getDouble("rating"),
            colors = colors,
            sizes = sizes,
            variants = variants,
        )
    }

    private fun <T> JSONArray.mapAs(f: (Any) -> T): List<T> = (0 until length()).map { f(get(it)) }

    private companion object {
        const val SEED = "seed/catalog.json"
        const val MIN_SUGGEST_CHARS = 2
        const val RECOMMENDATION_COUNT = 5
        val FIXED_RECOMMENDATIONS = listOf("P001", "P002", "P010", "P020", "P060")
    }
}
