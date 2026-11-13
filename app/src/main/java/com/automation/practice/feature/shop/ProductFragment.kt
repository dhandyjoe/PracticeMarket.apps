package com.automation.practice.feature.shop

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentProductBinding
import com.automation.practice.databinding.ItemFilterChipBinding
import com.automation.practice.databinding.ItemGalleryBinding
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.MAX_QTY
import com.automation.practice.domain.Product
import com.automation.practice.domain.toRupiah
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** SHOP-05 Detail Produk: galeri, varian warna × ukuran, stepper jumlah, wishlist, keranjang. */
class ProductFragment : ShopFragment(R.layout.fragment_product) {
    private lateinit var product: Product
    private var color = ""
    private var size = ""
    private var qty = 1
    private var _b: FragmentProductBinding? = null
    private val b get() = _b!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = vm.catalog.product(requireArguments().getString(ARG_ID).orEmpty()) ?: error("Produk tidak ada")
        if (savedInstanceState == null) {
            product.firstAvailable()?.let { color = it.color; size = it.size }
            vm.galleryIndex.value = 0
        } else {
            color = savedInstanceState.getString(STATE_COLOR).orEmpty()
            size = savedInstanceState.getString(STATE_SIZE).orEmpty()
            qty = savedInstanceState.getInt(STATE_QTY, 1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_COLOR, color)
        outState.putString(STATE_SIZE, size)
        outState.putInt(STATE_QTY, qty)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentProductBinding.bind(view)
        b.shopBarGalleryTop.applySystemInsets(top = true)
        b.shopBarPurchase.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }

        bindInfo()
        setupGallery()
        setupWishlist()
        setupMore()
        setupPurchase()
        renderVariants()
        renderQty()
    }

    override fun onStart() {
        super.onStart()
        // Image Viewer bisa mengganti gambar; selaraskan galeri saat kembali.
        if (_b != null && b.shopPagerGallery.currentItem != vm.galleryIndex.value) {
            b.shopPagerGallery.setCurrentItem(vm.galleryIndex.value, false)
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun bindInfo() {
        b.shopTxtProductMeta.text = getString(R.string.product_id_category_fmt, product.id, vm.catalog.categoryName(product.categorySlug))
        b.shopTxtProductName.text = product.name
        // Bug glitch_user: harga detail memakai spasi non-breaking (PRD §8.8).
        b.shopTxtProductPrice.text = product.price.toRupiah(nbsp = vm.glitch)
        product.originalPrice?.let {
            b.shopRowDiscount.isVisible = true
            b.shopTxtProductOldPrice.text = it.toRupiah()
            b.shopTxtProductOldPrice.paintFlags = b.shopTxtProductOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            b.shopTxtProductDiscount.text = "−${product.discountPercent}%"
        }
    }

    private fun setupGallery() {
        val visual = vm.visual(product)
        b.shopPagerGallery.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = GALLERY_SIZE
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val page = ItemGalleryBinding.bind(holder.itemView)
                visual.applyTo(page.shopImgGalleryBg, page.shopImgGallery)
            }
        }
        fun counter(index: Int) {
            b.shopTxtGalleryCounter.text = getString(R.string.gallery_counter_fmt, index + 1, GALLERY_SIZE)
        }
        b.shopPagerGallery.setCurrentItem(vm.galleryIndex.value, false)
        counter(vm.galleryIndex.value)
        b.shopPagerGallery.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                vm.galleryIndex.value = position
                counter(position)
            }
        })
        b.shopBtnPrevImage.setOnClickListener {
            b.shopPagerGallery.currentItem = (b.shopPagerGallery.currentItem + GALLERY_SIZE - 1) % GALLERY_SIZE
        }
        b.shopBtnNextImage.setOnClickListener {
            b.shopPagerGallery.currentItem = (b.shopPagerGallery.currentItem + 1) % GALLERY_SIZE
        }
        b.shopBtnZoom.setOnClickListener { nav.openImageViewer(product.id) }
    }

    private fun setupWishlist() {
        b.shopBtnWishlist.setOnClickListener { vm.session.toggleWishlist(product.id) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.wishlist.collect { list ->
                    val wished = product.id in list
                    b.shopBtnWishlist.isSelected = wished
                    b.shopBtnWishlist.setImageResource(if (wished) R.drawable.ic_favorite_filled else R.drawable.ic_favorite)
                    b.shopBtnWishlist.imageTintList = requireContext().getColorStateList(if (wished) R.color.pm_error else R.color.pm_ink)
                    b.shopBtnWishlist.contentDescription = getString(if (wished) R.string.product_wish_remove else R.string.product_wish_add)
                }
            }
        }
    }

    private fun setupMore() {
        b.shopBtnMore.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                menuInflater.inflate(R.menu.menu_product_more, menu)
                setOnMenuItemClickListener {
                    val message = if (it.itemId == R.id.shop_menu_share) R.string.product_shared else R.string.product_reported
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).setAnchorView(b.shopBarPurchase).show()
                    true
                }
                show()
            }
        }
    }

    private fun setupPurchase() {
        b.shopBtnQtyMinus.setOnClickListener { qty--; renderQty() }
        b.shopBtnQtyPlus.setOnClickListener {
            if (qty < maxQty()) { qty++; renderQty() } else showQtyLimit()
        }
        b.shopBtnAddToCart.apply {
            isEnabled = product.inStock
            setText(if (product.inStock) R.string.cart_add else R.string.stock_out)
            setOnClickListener {
                vm.session.addToCart(CartLine(product.id, color, size, qty))
                showAdded(anchor = b.shopBarPurchase)
            }
        }
        b.shopBtnBuyNow.apply {
            isEnabled = product.inStock
            setOnClickListener {
                vm.startCheckout(buyNow = CartLine(product.id, color, size, qty))
                nav.openCheckout()
            }
        }
    }

    /** Batas jumlah = min(stok varian, 10) (FR-SHOP-09). */
    private fun maxQty() = minOf(product.stockOf(color, size), MAX_QTY).coerceAtLeast(1)

    private fun showQtyLimit() {
        val stock = product.stockOf(color, size)
        b.shopTxtQtyMax.text = if (stock in 1 until MAX_QTY) getString(R.string.product_qty_stock_fmt, stock) else getString(R.string.product_qty_max)
        b.shopTxtQtyMax.isVisible = true
    }

    private fun renderQty() {
        qty = qty.coerceIn(1, maxQty())
        b.shopTxtQty.text = qty.toString()
        b.shopBtnQtyMinus.isEnabled = qty > 1
        b.shopTxtQtyMax.isVisible = false
    }

    private fun renderVariants() {
        val hasColor = product.colors.isNotEmpty()
        val hasSize = product.sizes.isNotEmpty()
        b.shopSectionColor.isVisible = hasColor
        b.shopSectionSize.isVisible = hasSize

        if (hasColor) {
            b.shopTxtColorLabel.text = boldValue(R.string.product_color_fmt, color)
            fillChips(b.shopGroupColor, product.colors, selected = color, enabled = { c -> product.variants.any { it.color == c && it.stock > 0 } }) { c ->
                color = c
                if (hasSize && product.stockOf(color, size) == 0) size = product.variants.firstOrNull { it.color == color && it.stock > 0 }?.size.orEmpty()
                afterVariantChange()
            }
        }
        if (hasSize) {
            b.shopTxtSizeLabel.text = boldValue(R.string.product_size_fmt, size)
            fillChips(b.shopGroupSize, product.sizes, selected = size, enabled = { s -> product.stockOf(color, s) > 0 }) { s ->
                size = s
                afterVariantChange()
            }
        }
        val empty = product.variants.filter { it.color == color && it.stock == 0 && it.size.isNotEmpty() }
        b.shopTxtStockNote.isVisible = empty.isNotEmpty()
        b.shopTxtStockNote.text = empty.joinToString { getString(R.string.product_stock_note_fmt, listOf(it.color, it.size).filter(String::isNotEmpty).joinToString(" · ")) }
    }

    private fun afterVariantChange() {
        renderVariants()
        renderQty()
    }

    private fun fillChips(group: ChipGroup, options: List<String>, selected: String, enabled: (String) -> Boolean, onPick: (String) -> Unit) {
        group.removeAllViews()
        options.forEach { option ->
            val chip: Chip = ItemFilterChipBinding.inflate(layoutInflater, group, false).root
            chip.id = View.generateViewId()
            chip.text = option
            chip.isEnabled = enabled(option)
            chip.isChecked = option == selected
            if (!chip.isEnabled) chip.paintFlags = chip.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            chip.setOnClickListener { onPick(option) }
            group.addView(chip)
        }
    }

    private fun boldValue(format: Int, value: String): CharSequence {
        val text = getString(format, value)
        return android.text.SpannableString(text).apply {
            val start = text.length - value.length
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    companion object {
        private const val ARG_ID = "id"
        private const val STATE_COLOR = "color"
        private const val STATE_SIZE = "size"
        private const val STATE_QTY = "qty"
        const val GALLERY_SIZE = 5
        fun newInstance(id: String) = ProductFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
