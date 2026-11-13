package com.automation.practice.feature.shop

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automation.practice.R
import com.automation.practice.databinding.ItemProductGridBinding
import com.automation.practice.databinding.ItemProductListBinding
import com.automation.practice.databinding.ItemProductTailBinding
import com.automation.practice.databinding.ItemSeeAllBinding
import com.automation.practice.domain.Product
import com.automation.practice.domain.toRupiah
import java.util.Locale

enum class ViewMode { GRID, LIST }

/** Baris penutup daftar: spinner, teks selesai, atau sel "Lihat semua" (Beranda). */
enum class Tail { NONE, LOADING, END, SEE_ALL }

/** Satu adapter untuk kartu grid, baris list, dan penutup. Dipakai Beranda dan Hasil Pencarian. */
class ProductAdapter(
    private val visual: (Product) -> Visual,
    private val onOpen: (Product) -> Unit,
    private val onAdd: (Product) -> Unit,
    private val onSeeAll: () -> Unit = {},
    private val gridNameLines: Int = 2,
    private val nbspPrice: () -> Boolean = { false },
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<Product> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }

    var mode = ViewMode.GRID
        set(value) { if (field != value) { field = value; notifyDataSetChanged() } }

    var tail = Tail.NONE
        set(value) { if (field != value) { field = value; notifyDataSetChanged() } }

    var seeAllMeta = ""

    /** Grid 2 kolom, list 1 kolom (spanCount diatur pemilik). Penutup merentang penuh, kecuali sel "Lihat semua". */
    val spanLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = when {
            mode == ViewMode.LIST || position < items.size || tail == Tail.SEE_ALL -> 1
            else -> GRID_SPAN
        }
    }

    override fun getItemCount() = items.size + if (tail == Tail.NONE) 0 else 1

    override fun getItemViewType(position: Int) = when {
        position >= items.size -> if (tail == Tail.SEE_ALL) TYPE_SEE_ALL else TYPE_TAIL
        mode == ViewMode.GRID -> TYPE_GRID
        else -> TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GRID -> GridHolder(ItemProductGridBinding.inflate(inflater, parent, false))
            TYPE_LIST -> ListHolder(ItemProductListBinding.inflate(inflater, parent, false))
            TYPE_SEE_ALL -> SeeAllHolder(ItemSeeAllBinding.inflate(inflater, parent, false))
            else -> TailHolder(ItemProductTailBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GridHolder -> holder.bind(items[position])
            is ListHolder -> holder.bind(items[position])
            is SeeAllHolder -> holder.b.apply {
                shopTxtSeeAllMeta.text = seeAllMeta
                shopBtnSeeAll.setOnClickListener { onSeeAll() }
            }
            is TailHolder -> holder.b.apply {
                val loading = tail == Tail.LOADING
                shopProgressMore.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
                shopTxtListFooter.setText(if (loading) R.string.results_loading else R.string.results_end)
            }
        }
    }

    private fun price(p: Product) = p.price.toRupiah(nbspPrice())

    private fun rating(p: Product, ctx: android.content.Context) =
        ctx.getString(R.string.rating_fmt, String.format(Locale.US, "%.1f", p.rating))

    inner class GridHolder(val b: ItemProductGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Product) {
            visual(p).applyTo(b.shopImgProductBg, b.shopImgProduct)
            b.shopTxtProductName.text = p.name
            b.shopTxtProductName.setLines(gridNameLines)
            b.shopTxtProductPrice.text = price(p)
            b.shopTxtProductRating.text = rating(p, b.root.context)
            val old = p.originalPrice
            b.shopRowDiscount.visibility = if (old == null) android.view.View.GONE else android.view.View.VISIBLE
            if (old != null) {
                b.shopTxtProductOldPrice.text = old.toRupiah()
                b.shopTxtProductOldPrice.paintFlags = b.shopTxtProductOldPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                b.shopTxtProductDiscount.text = "−${p.discountPercent}%"
            }
            b.shopItemProductOpen.setOnClickListener { onOpen(p) }
            b.shopBtnAddToCart.apply {
                isEnabled = p.inStock
                setText(if (p.inStock) R.string.cart_add else R.string.stock_out)
                setOnClickListener { onAdd(p) }
            }
        }
    }

    inner class ListHolder(val b: ItemProductListBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Product) {
            val ctx = b.root.context
            visual(p).applyTo(b.shopImgProductBg, b.shopImgProduct)
            b.shopTxtProductName.text = p.name
            b.shopTxtProductPrice.text = price(p)
            b.shopTxtProductRating.text = rating(p, ctx)
            b.shopItemProductOpen.setOnClickListener { onOpen(p) }
            b.shopBtnAddToCart.apply {
                isEnabled = p.inStock
                contentDescription = ctx.getString(if (p.inStock) R.string.cart_add_cd_fmt else R.string.stock_out, p.name)
                iconTint = ColorStateList.valueOf(ctx.getColor(if (p.inStock) R.color.pm_primary else R.color.pm_ink_muted))
                setOnClickListener { onAdd(p) }
            }
        }
    }

    class SeeAllHolder(val b: ItemSeeAllBinding) : RecyclerView.ViewHolder(b.root)
    class TailHolder(val b: ItemProductTailBinding) : RecyclerView.ViewHolder(b.root)

    companion object {
        const val GRID_SPAN = 2
        private const val TYPE_GRID = 0
        private const val TYPE_LIST = 1
        private const val TYPE_TAIL = 2
        private const val TYPE_SEE_ALL = 3
    }
}
