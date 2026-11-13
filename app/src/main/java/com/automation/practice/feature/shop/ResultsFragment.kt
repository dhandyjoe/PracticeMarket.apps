package com.automation.practice.feature.shop

import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentResultsBinding
import com.automation.practice.domain.ProductFilter
import com.automation.practice.domain.Sort
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** SHOP-04 Hasil Pencarian: filter, urutan, grid/list, infinite scroll 20 per halaman. */
class ResultsFragment : ShopFragment(R.layout.fragment_results) {
    private var mode = ViewMode.GRID
    private var shown = PAGE_SIZE
    private var loading = false
    private var listState: Parcelable? = null
    private var lastFilter: ProductFilter? = null

    private var _b: FragmentResultsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: ProductAdapter
    private lateinit var layoutManager: GridLayoutManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentResultsBinding.bind(view)
        b.shopHeaderResults.applySystemInsets(top = true)
        b.shopListResults.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }
        b.shopBtnSearchBox.setOnClickListener { nav.openSearch(vm.filter.value.query) }
        b.shopBtnFilter.isSelected = true // tampil gelap (bg_icon_toggle)
        b.shopBtnFilter.setOnClickListener { nav.openFilter() }
        b.shopBtnResetFilter.setOnClickListener { vm.resetFilter() }

        adapter = ProductAdapter(
            visual = vm::visual,
            onOpen = { nav.openProduct(it.id) },
            onAdd = ::quickAdd,
            gridNameLines = 1,
        )
        layoutManager = GridLayoutManager(requireContext(), ProductAdapter.GRID_SPAN).apply { spanSizeLookup = adapter.spanLookup }
        b.shopListResults.layoutManager = layoutManager
        b.shopListResults.adapter = adapter
        b.shopListResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) = loadMoreIfNeeded()
        })

        b.shopBtnViewGrid.setOnClickListener { mode = ViewMode.GRID; render() }
        b.shopBtnViewList.setOnClickListener { mode = ViewMode.LIST; render() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.filter.collect {
                    // Filter baru mulai dari halaman pertama; kembali dari Detail mempertahankan posisi scroll.
                    if (lastFilter != null && it != lastFilter) { shown = PAGE_SIZE; listState = null }
                    lastFilter = it
                    render()
                }
            }
        }
    }

    override fun onDestroyView() {
        listState = layoutManager.onSaveInstanceState()
        loading = false // coroutine muat-lagi ikut batal bersama view
        _b = null
        super.onDestroyView()
    }

    private fun render() {
        val f = vm.filter.value
        val all = vm.results()
        val visible = all.take(shown)

        layoutManager.spanCount = if (mode == ViewMode.GRID) ProductAdapter.GRID_SPAN else 1
        adapter.mode = mode
        adapter.items = visible
        adapter.tail = when {
            all.isEmpty() -> Tail.NONE
            visible.size < all.size -> Tail.LOADING
            else -> Tail.END
        }
        listState?.let { layoutManager.onRestoreInstanceState(it); listState = null }

        b.shopBtnViewGrid.isSelected = mode == ViewMode.GRID
        b.shopBtnViewList.isSelected = mode == ViewMode.LIST
        b.shopEmptyResults.isVisible = all.isEmpty()
        b.shopListResults.isVisible = all.isNotEmpty()

        val query = f.query.ifBlank { f.categories.firstOrNull()?.let(vm.catalog::categoryName).orEmpty() }
        b.shopTxtSearchQuery.text = query.ifBlank { getString(R.string.search_hint) }
        b.shopTxtSearchQuery.setTextColor(requireContext().getColor(if (query.isBlank()) R.color.pm_ink_muted else R.color.pm_ink))
        val count = f.activeCount
        b.shopBadgeFilter.isVisible = count > 0
        b.shopBadgeFilter.text = count.toString()
        b.shopBtnFilter.contentDescription =
            if (count > 0) getString(R.string.results_filter_cd_fmt, count) else getString(R.string.results_filter_cd_none)

        val head = "${all.size} produk"
        val summary = getString(R.string.results_count_fmt, all.size, getString(f.sort.labelRes()))
        b.shopTxtResultCount.text = SpannableStringBuilder(summary).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, head.length.coerceAtMost(length), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        b.shopListResults.post { loadMoreIfNeeded() }
    }

    /** Muat 20 produk berikutnya (jeda singkat meniru jaringan) saat penutup daftar terlihat. */
    private fun loadMoreIfNeeded() {
        if (_b == null || loading || adapter.tail != Tail.LOADING) return
        if (layoutManager.findLastVisibleItemPosition() < adapter.itemCount - 1) return
        loading = true
        viewLifecycleOwner.lifecycleScope.launch {
            delay(LOAD_DELAY_MS)
            shown += PAGE_SIZE
            loading = false
            if (_b != null) render()
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val LOAD_DELAY_MS = 600L
    }
}

fun Sort.labelRes() = when (this) {
    Sort.RELEVANT -> R.string.sort_relevant
    Sort.PRICE_ASC -> R.string.sort_price_asc
    Sort.PRICE_DESC -> R.string.sort_price_desc
    Sort.RATING -> R.string.sort_rating
    Sort.NEWEST -> R.string.sort_newest
}
