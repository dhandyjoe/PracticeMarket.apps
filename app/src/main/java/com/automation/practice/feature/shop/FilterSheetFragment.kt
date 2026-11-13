package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.ItemFilterChipBinding
import com.automation.practice.databinding.SheetFilterBinding
import com.automation.practice.domain.PRICE_MAX
import com.automation.practice.domain.ProductFilter
import com.automation.practice.domain.Sort
import com.automation.practice.domain.toRupiah
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlin.math.roundToLong

/** SHOP-04 Filter & Urutkan. Perubahan baru berlaku setelah "Terapkan". */
class FilterSheetFragment : BottomSheetDialogFragment() {
    private val vm: ShopViewModel by activityViewModels()
    private var draft = ProductFilter()
    private var binding: SheetFilterBinding? = null
    private var rendering = false

    private val sortIds = mutableMapOf<Int, Sort>()
    private val categoryIds = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draft = vm.filter.value
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val b = SheetFilterBinding.inflate(inflater, container, false)
        binding = b
        b.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * SHEET_HEIGHT_RATIO).toInt(),
        )
        b.shopBarFilterActions.applySystemInsets(bottom = true)

        Sort.entries.forEach { sort ->
            val chip = newChip(b.shopGroupSort, getString(sort.labelRes()))
            sortIds[chip.id] = sort
            b.shopGroupSort.addView(chip)
        }
        vm.catalog.categories.forEach { c ->
            val chip = newChip(b.shopGroupCategory, c.name)
            categoryIds[chip.id] = c.slug
            b.shopGroupCategory.addView(chip)
        }

        b.shopGroupSort.setOnCheckedStateChangeListener { _, ids ->
            if (!rendering) ids.firstOrNull()?.let { draft = draft.copy(sort = sortIds.getValue(it)) }
        }
        b.shopGroupCategory.setOnCheckedStateChangeListener { _, ids ->
            if (!rendering) { draft = draft.copy(categories = ids.mapNotNull(categoryIds::get).toSet()); renderBadge() }
        }
        b.shopSliderPrice.addOnChangeListener { slider, _, fromUser ->
            if (rendering || !fromUser) return@addOnChangeListener
            draft = draft.copy(minPrice = slider.values[0].roundToLong(), maxPrice = slider.values[1].roundToLong())
            render(skipSlider = true)
        }
        listOf(b.shopInputPriceMin, b.shopInputPriceMax).forEach { input ->
            input.setOnEditorActionListener { v, action, _ ->
                if (action == EditorInfo.IME_ACTION_DONE) { v.clearFocus(); true } else false
            }
            input.setOnFocusChangeListener { _, focused -> if (!focused) commitPriceInputs() }
        }
        b.shopGroupRating.setOnCheckedChangeListener { _, id ->
            if (!rendering) { draft = draft.copy(minRating = ratingOf(id)); renderBadge() }
        }
        b.shopSwStockOnly.setOnCheckedChangeListener { _, on ->
            if (!rendering) { draft = draft.copy(inStockOnly = on); renderBadge() }
        }

        b.shopBtnCloseFilter.setOnClickListener { dismiss() }
        b.shopBtnReset.setOnClickListener { draft = ProductFilter(query = draft.query); render() }
        b.shopBtnApply.setOnClickListener {
            commitPriceInputs()
            vm.applyFilter(draft)
            dismiss()
        }
        render()
        return b.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun newChip(parent: ViewGroup, label: String): Chip =
        ItemFilterChipBinding.inflate(layoutInflater, parent, false).root.apply {
            id = View.generateViewId()
            text = label
        }

    private fun ratingOf(checkedId: Int) = when (checkedId) {
        R.id.shop_rdo_rating_3 -> 3.0
        R.id.shop_rdo_rating_4 -> 4.0
        R.id.shop_rdo_rating_45 -> 4.5
        else -> 0.0
    }

    private fun ratingId(rating: Double) = when (rating) {
        3.0 -> R.id.shop_rdo_rating_3
        4.0 -> R.id.shop_rdo_rating_4
        4.5 -> R.id.shop_rdo_rating_45
        else -> R.id.shop_rdo_rating_all
    }

    /** Baca isian harga: ambil angka, jepit ke rentang, tukar bila min > max, bulatkan ke langkah slider. */
    private fun commitPriceInputs() {
        val b = binding ?: return
        fun parse(text: CharSequence, fallback: Long) =
            text.filter(Char::isDigit).toString().toLongOrNull() ?: fallback
        val a = parse(b.shopInputPriceMin.text, draft.minPrice).coerceIn(0, PRICE_MAX)
        val z = parse(b.shopInputPriceMax.text, draft.maxPrice).coerceIn(0, PRICE_MAX)
        draft = draft.copy(minPrice = snap(minOf(a, z)), maxPrice = snap(maxOf(a, z)))
        render()
    }

    private fun snap(v: Long) = (v / PRICE_STEP) * PRICE_STEP

    private fun renderBadge() {
        val b = binding ?: return
        val n = draft.activeCount
        b.shopBadgeApply.text = n.toString()
        b.shopBtnApply.contentDescription = getString(R.string.filter_apply_cd_fmt, n)
    }

    private fun render(skipSlider: Boolean = false) {
        val b = binding ?: return
        rendering = true
        sortIds.entries.firstOrNull { it.value == draft.sort }?.let { b.shopGroupSort.check(it.key) }
        categoryIds.forEach { (id, slug) -> (b.shopGroupCategory.findViewById<Chip>(id)).isChecked = slug in draft.categories }
        b.shopTxtPriceRange.text = "${draft.minPrice.toRupiah()} – ${draft.maxPrice.toRupiah()}"
        if (!skipSlider) b.shopSliderPrice.values = listOf(draft.minPrice.toFloat(), draft.maxPrice.toFloat())
        if (!b.shopInputPriceMin.hasFocus()) b.shopInputPriceMin.setText(draft.minPrice.toRupiah())
        if (!b.shopInputPriceMax.hasFocus()) b.shopInputPriceMax.setText(draft.maxPrice.toRupiah())
        b.shopGroupRating.check(ratingId(draft.minRating))
        b.shopSwStockOnly.isChecked = draft.inStockOnly
        renderBadge()
        rendering = false
    }

    private companion object {
        const val SHEET_HEIGHT_RATIO = 0.9
        const val PRICE_STEP = 5_000L
    }
}
