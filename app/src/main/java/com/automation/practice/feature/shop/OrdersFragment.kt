package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentOrdersBinding
import com.automation.practice.databinding.ItemOrderBinding
import com.automation.practice.databinding.ItemOrdersPageBinding
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.formatHm
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** ORD-01 Pesanan Saya: tab status (klik/swipe), tarik-segarkan, empty state (FR-ORD-01). */
class OrdersFragment : ShopFragment(R.layout.fragment_orders) {
    private var binding: FragmentOrdersBinding? = null
    private var orders = emptyList<Order>()
    private var updatedAt = 0L
    private val pages = mutableMapOf<Int, PageHolder>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentOrdersBinding.bind(view)
        binding = b
        b.shopTopbar.setup(R.string.ord_title)
        b.shopRefreshOrders.applySystemInsets(bottom = true)
        val container = requireContext().app.container
        updatedAt = container.now()

        b.shopPagerOrders.adapter = PagerAdapter()
        TabLayoutMediator(b.shopTabOrders, b.shopPagerOrders) { tab, i -> tab.setText(ORDER_TABS[i].first) }.attach()
        // Tarik-segarkan hanya bila daftar di tab aktif sudah di paling atas.
        b.shopRefreshOrders.setOnChildScrollUpCallback { _, _ ->
            pages[b.shopPagerOrders.currentItem]?.list?.canScrollVertically(-1) == true
        }
        b.shopRefreshOrders.setColorSchemeResources(R.color.pm_primary)
        b.shopRefreshOrders.setOnRefreshListener { refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.session.orders.collect { orders = it; render() } }
                // Hitung mundur "Bayar dalam" pada pesanan VA menunggu.
                launch { while (true) { delay(1000); if (orders.any { it.status == OrderStatus.WAITING }) render() } }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        pages.clear()
        super.onDestroyView()
    }

    private fun refresh() {
        val b = binding ?: return
        val container = requireContext().app.container
        b.shopRefreshOrders.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            delay(REFRESH_MS * container.settings.prefs.value.latency)
            if (container.serverFails()) {
                Snackbar.make(b.root, R.string.load_failed, Snackbar.LENGTH_LONG).setAction(R.string.retry) { refresh() }.show()
            } else {
                updatedAt = container.now()
                render()
            }
            b.shopRefreshOrders.isRefreshing = false
        }
    }

    private fun render() {
        val b = binding ?: return
        b.shopTxtOrdersUpdated.text = getString(R.string.ord_updated_fmt, formatHm(updatedAt))
        b.shopPagerOrders.adapter?.notifyItemRangeChanged(0, ORDER_TABS.size)
    }

    private fun open(o: Order) {
        if (o.status == OrderStatus.WAITING && o.method == PayMethod.VA && vm.showVa(o.id)) nav.openVa() else nav.openOrder(o.id)
    }

    private class PageHolder(val b: ItemOrdersPageBinding, val adapter: OrderAdapter) : RecyclerView.ViewHolder(b.root) {
        val list get() = b.shopListOrders
    }

    private inner class PagerAdapter : RecyclerView.Adapter<PageHolder>() {
        override fun getItemCount() = ORDER_TABS.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val pb = ItemOrdersPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val adapter = OrderAdapter()
            pb.shopListOrders.layoutManager = LinearLayoutManager(parent.context)
            pb.shopListOrders.adapter = adapter
            return PageHolder(pb, adapter)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            pages[position] = holder
            val shown = orders.forTab(position)
            holder.adapter.submitList(shown.map { Row(it, note(it)) })
            holder.b.shopEmptyOrders.isVisible = shown.isEmpty()
        }
    }

    /** Baris daftar: [note] ikut dibandingkan supaya hitung mundur memicu bind ulang. */
    private data class Row(val order: Order, val note: String)

    /** Catatan di bawah baris: sisa waktu bayar VA (menunggu) atau alasan batal. */
    private fun note(o: Order): String = when {
        o.status == OrderStatus.WAITING && o.vaDeadline > 0 -> {
            val left = ((o.vaDeadline - requireContext().app.container.now()) / 1000).coerceAtLeast(0)
            getString(R.string.ord_pay_within_fmt, "%02d:%02d".format(left / 60, left % 60))
        }
        o.status == OrderStatus.CANCELLED -> o.cancelReason.ifEmpty { "" }
        else -> ""
    }

    private inner class OrderAdapter : ListAdapter<Row, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(a: Row, b: Row) = a.order.id == b.order.id
        override fun areContentsTheSame(a: Row, b: Row) = a == b
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val (o, note) = getItem(position)
            val row = ItemOrderBinding.bind(holder.itemView)
            row.shopTxtOrderId.text = o.id
            row.shopTxtOrderStatus.showStatus(o.status)
            vm.showThumb(o, row.shopImgOrderBg, row.shopImgOrder)
            val first = o.lines.first()
            val name = vm.catalog.product(first.productId)?.name.orEmpty()
            row.shopTxtOrderItem.text = if (o.lines.size > 1) getString(R.string.ord_more_items_fmt, name, o.lines.size - 1) else name
            row.shopTxtOrderMeta.text = if (o.lines.size > 1) getString(R.string.ord_items_fmt, o.lines.sumOf { it.qty })
            else listOf(first.color, first.size, getString(R.string.ord_qty_fmt, first.qty)).filter { it.isNotBlank() }.joinToString(" · ")
            row.shopTxtOrderTotal.text = o.total.toRupiah()
            row.shopTxtOrderNote.isVisible = note.isNotEmpty()
            row.shopTxtOrderNote.text = note
            row.shopTxtOrderNote.setTextColor(requireContext().getColor(if (o.status == OrderStatus.CANCELLED) R.color.pm_ink_muted else R.color.pm_warning_strong))
            holder.itemView.setOnClickListener { open(o) }
        }
    }

    private companion object {
        const val REFRESH_MS = 600L
    }
}
