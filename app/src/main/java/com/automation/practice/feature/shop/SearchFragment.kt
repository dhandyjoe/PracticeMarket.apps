package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentSearchBinding
import com.automation.practice.databinding.ItemCategoryChipBinding
import com.automation.practice.databinding.ItemHistoryBinding
import com.automation.practice.databinding.ItemSuggestionBinding
import kotlinx.coroutines.launch

/** SHOP-03 Pencarian: saran (≥ 2 huruf), riwayat 5 terakhir, kategori populer. */
class SearchFragment : ShopFragment(R.layout.fragment_search) {
    private var _b: FragmentSearchBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentSearchBinding.bind(view)
        b.shopHeaderSearch.applySystemInsets(top = true)
        b.shopScrollSearch.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }

        b.shopInputSearch.apply {
            if (savedInstanceState == null) setText(requireArguments().getString(ARG_QUERY).orEmpty())
            setSelection(text.length)
            doAfterTextChanged { renderSuggestions() }
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEARCH) submit(text.toString()).let { true } else false
            }
            requestFocus()
        }
        b.shopBtnClearSearch.setOnClickListener { b.shopInputSearch.text.clear() }
        b.shopBtnClearHistory.setOnClickListener { vm.session.clearHistory() }

        POPULAR.forEach { slug ->
            val name = vm.catalog.categoryName(slug)
            val chip = ItemCategoryChipBinding.inflate(layoutInflater, b.shopGroupPopular, false)
            chip.root.text = name
            chip.root.setOnClickListener { vm.browse(slug); nav.openResults() }
            b.shopGroupPopular.addView(chip.root)
        }

        renderSuggestions()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.history.collect(::renderHistory)
            }
        }
        if (b.shopInputSearch.text.isEmpty()) showKeyboard()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun submit(q: String) {
        if (q.isBlank()) return
        hideKeyboard()
        vm.search(q)
        nav.openResults()
    }

    private fun renderSuggestions() {
        val q = b.shopInputSearch.text.toString()
        b.shopBtnClearSearch.isVisible = q.isNotEmpty()
        val active = q.trim().length >= 2
        val suggestions = if (active) vm.catalog.suggestions(q) else emptyList()
        b.shopListSuggestions.removeAllViews()
        suggestions.forEach { p ->
            val row = ItemSuggestionBinding.inflate(LayoutInflater.from(context), b.shopListSuggestions, false)
            row.shopTxtSuggestion.text = p.name
            row.shopTxtSuggestionCategory.text = vm.catalog.categoryName(p.categorySlug)
            row.root.setOnClickListener { submit(p.name) }
            b.shopListSuggestions.addView(row.root)
        }
        b.shopListSuggestions.isVisible = suggestions.isNotEmpty()
        b.shopTxtNoSuggestion.isVisible = active && suggestions.isEmpty()
        b.shopTxtNoSuggestion.text = getString(R.string.search_no_suggestion_fmt, q.trim())
    }

    private fun renderHistory(history: List<String>) {
        b.shopListHistory.removeAllViews()
        history.forEach { term ->
            val row = ItemHistoryBinding.inflate(LayoutInflater.from(context), b.shopListHistory, false)
            row.shopTxtHistory.text = term
            row.shopBtnDeleteHistory.contentDescription = getString(R.string.search_history_delete_cd_fmt, term)
            row.shopBtnDeleteHistory.setOnClickListener { vm.session.removeHistory(term) }
            row.root.setOnClickListener { submit(term) }
            // Tekan lama → menu "Hapus" (FR-SHOP-04).
            row.root.setOnLongClickListener {
                PopupMenu(requireContext(), row.root).apply {
                    menuInflater.inflate(R.menu.menu_history_row, menu)
                    setOnMenuItemClickListener { vm.session.removeHistory(term); true }
                    show()
                }
                true
            }
            b.shopListHistory.addView(row.root)
        }
        b.shopBtnClearHistory.isVisible = history.isNotEmpty()
        b.shopTxtHistoryEmpty.isVisible = history.isEmpty()
    }

    private fun showKeyboard() {
        b.shopInputSearch.post {
            requireContext().getSystemService(InputMethodManager::class.java)
                .showSoftInput(b.shopInputSearch, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        requireContext().getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(b.shopInputSearch.windowToken, 0)
    }

    companion object {
        private const val ARG_QUERY = "query"
        private val POPULAR = listOf("fashion", "elektronik", "olahraga", "buku")
        fun newInstance(query: String) = SearchFragment().apply { arguments = bundleOf(ARG_QUERY to query) }
    }
}
