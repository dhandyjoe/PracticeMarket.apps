package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentHomeBinding
import com.automation.practice.databinding.ItemBannerBinding
import com.automation.practice.databinding.ItemCategoryChipBinding
import com.automation.practice.domain.toRupiah
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** SHOP-01 Beranda. */
class HomeFragment : ShopFragment(R.layout.fragment_home, lightStatusBar = false) {
    private var mode = ViewMode.GRID

    // Urutan tetap di mode deterministik, acak selain itu (FR-SHOP-02).
    private val recommendations by lazy { vm.catalog.recommendations(shuffle = !requireContext().app.container.settings.prefs.value.deterministic) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHomeBinding.bind(view)
        b.shopHeaderHome.applySystemInsets(top = true)
        b.shopTxtGreeting.text = getString(R.string.home_greeting_fmt, requireContext().app.container.auth.currentUser?.displayName.orEmpty())
        b.shopBtnSearch.setOnClickListener { nav.openSearch() }
        b.shopCardWallet.setOnClickListener { nav.selectTab(Tab.PAY) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.homeBalance.collect { b.shopTxtWalletBalance.text = it.toRupiah() }
            }
        }
        setupBanners(b)
        setupCategories(b)
        setupFlashSale(b)
        setupProducts(b)
    }

    private fun setupBanners(b: FragmentHomeBinding) {
        val banners = listOf(
            R.string.home_banner_1_code to R.string.home_banner_1_desc,
            R.string.home_banner_2_code to R.string.home_banner_2_desc,
            R.string.home_banner_3_code to R.string.home_banner_3_desc,
        )
        b.shopPagerBanner.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = banners.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = ItemBannerBinding.bind(holder.itemView)
                item.shopTxtBannerCode.setText(banners[position].first)
                item.shopTxtBannerDesc.setText(banners[position].second)
            }
        }
        val dots = banners.map {
            View(requireContext()).also { dot ->
                dot.layoutParams = LinearLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.pm_dot), resources.getDimensionPixelSize(R.dimen.pm_dot)).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.pm_space_1_5)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.pm_space_1_5)
                }
                b.shopIndicatorBanner.addView(dot)
            }
        }
        fun select(index: Int) {
            dots.forEachIndexed { i, dot ->
                dot.setBackgroundResource(if (i == index) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
                dot.layoutParams.width = resources.getDimensionPixelSize(if (i == index) R.dimen.pm_dot_active else R.dimen.pm_dot)
                dot.requestLayout()
            }
            b.shopIndicatorBanner.contentDescription = getString(R.string.home_banner_cd_fmt, index + 1, banners.size)
        }
        select(0)
        b.shopPagerBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = select(position)
        })
        // Geser otomatis tiap 4 detik selama layar tampil (FR-SHOP-01).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(BANNER_INTERVAL_MS)
                    b.shopPagerBanner.currentItem = (b.shopPagerBanner.currentItem + 1) % banners.size
                }
            }
        }
    }

    private fun setupCategories(b: FragmentHomeBinding) {
        vm.catalog.categories.forEach { c ->
            val chip = ItemCategoryChipBinding.inflate(layoutInflater, b.shopListCategories, false)
            (chip.root.layoutParams as LinearLayout.LayoutParams).marginEnd = resources.getDimensionPixelSize(R.dimen.pm_space_2)
            chip.root.text = c.name
            chip.root.setOnClickListener {
                vm.browse(c.slug)
                nav.openResults()
            }
            b.shopListCategories.addView(chip.root)
        }
    }

    private fun setupFlashSale(b: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val left = ((vm.session.flashEndsAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                    b.shopTxtFlashCountdown.text = "%02d:%02d:%02d".format(left / 3600, left / 60 % 60, left % 60)
                    delay(1000)
                }
            }
        }
    }

    private fun setupProducts(b: FragmentHomeBinding) {
        val adapter = ProductAdapter(
            visual = vm::visual,
            onOpen = { nav.openProduct(it.id) },
            onAdd = ::quickAdd,
            onSeeAll = { vm.applyFilter(com.automation.practice.domain.ProductFilter()); nav.openResults() },
            nbspPrice = { false },
        )
        adapter.items = recommendations
        adapter.tail = Tail.SEE_ALL
        adapter.seeAllMeta = getString(R.string.home_see_all_meta_fmt, vm.catalog.products.size, vm.catalog.categories.size)
        val layoutManager = GridLayoutManager(requireContext(), ProductAdapter.GRID_SPAN).apply { spanSizeLookup = adapter.spanLookup }
        b.shopListProducts.layoutManager = layoutManager
        b.shopListProducts.adapter = adapter

        fun render() {
            adapter.mode = mode
            layoutManager.spanCount = if (mode == ViewMode.GRID) ProductAdapter.GRID_SPAN else 1
            adapter.tail = if (mode == ViewMode.GRID) Tail.SEE_ALL else Tail.NONE
            b.shopBtnViewGrid.isSelected = mode == ViewMode.GRID
            b.shopBtnViewList.isSelected = mode == ViewMode.LIST
        }
        b.shopBtnViewGrid.setOnClickListener { mode = ViewMode.GRID; render() }
        b.shopBtnViewList.setOnClickListener { mode = ViewMode.LIST; render() }
        render()
    }

    private companion object {
        const val BANNER_INTERVAL_MS = 4_000L
    }
}
