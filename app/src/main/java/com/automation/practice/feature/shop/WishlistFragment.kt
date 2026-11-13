package com.automation.practice.feature.shop

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentWishlistBinding
import com.automation.practice.databinding.ItemWishlistBinding
import com.automation.practice.domain.Product
import com.automation.practice.domain.toRupiah
import kotlinx.coroutines.launch

/** SHOP-08 Wishlist: hapus lewat hati, atur urutan lewat drag handle atau tombol naik. */
class WishlistFragment : ShopFragment(R.layout.fragment_wishlist) {
    private var rows = mutableListOf<Product>()
    private var editing = false
    private lateinit var adapter: RowAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentWishlistBinding.bind(view)
        b.shopHeaderWishlist.applySystemInsets(top = true)
        b.shopListWishlist.applySystemInsets(bottom = true)
        b.shopBtnBack.setOnClickListener { nav.back() }
        b.shopBtnStartShopping.setOnClickListener { nav.selectTab(Tab.HOME) }
        b.shopBtnEditOrder.setOnClickListener { editing = !editing; render(b) }

        adapter = RowAdapter()
        b.shopListWishlist.layoutManager = LinearLayoutManager(requireContext())
        b.shopListWishlist.adapter = adapter
        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = false
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val a = from.bindingAdapterPosition
                val z = to.bindingAdapterPosition
                rows.add(z, rows.removeAt(a))
                adapter.notifyItemMoved(a, z)
                return true
            }
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(rv, holder)
                vm.session.setWishlistOrder(rows.map { it.id })
            }
        }).also { it.attachToRecyclerView(b.shopListWishlist) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.wishlist.collect { ids ->
                    if (ids != rows.map { it.id }) rows = ids.mapNotNull(vm.catalog::product).toMutableList()
                    render(b)
                }
            }
        }
    }

    private fun render(b: FragmentWishlistBinding) {
        if (rows.isEmpty()) editing = false
        b.shopBtnEditOrder.isVisible = rows.isNotEmpty()
        b.shopBtnEditOrder.setText(if (editing) R.string.wishlist_done else R.string.wishlist_reorder)
        b.shopListWishlist.isVisible = rows.isNotEmpty()
        b.shopEmptyWishlist.isVisible = rows.isEmpty()
        adapter.notifyDataSetChanged()
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.Holder>() {
        inner class Holder(val b: ItemWishlistBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = rows.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val p = rows[position]
            val b = holder.b
            val ctx = b.root.context
            vm.visual(p).applyTo(b.shopImgWishBg, b.shopImgWish)
            b.shopTxtWishName.text = p.name
            b.shopTxtWishPrice.text = p.price.toRupiah()
            b.shopItemWishlistOpen.setOnClickListener { nav.openProduct(p.id) }

            b.shopImgDragHandle.isVisible = editing
            b.shopImgDragHandle.contentDescription = ctx.getString(R.string.wishlist_drag_cd_fmt, p.name)
            b.shopImgDragHandle.setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
                false
            }
            b.shopBtnWishRemove.isVisible = !editing
            b.shopBtnWishRemove.contentDescription = ctx.getString(R.string.wishlist_remove_cd_fmt, p.name)
            b.shopBtnWishRemove.setOnClickListener { vm.session.toggleWishlist(p.id) }
            b.shopBtnWishUp.isVisible = editing
            b.shopBtnWishUp.isEnabled = position > 0
            b.shopBtnWishUp.contentDescription = ctx.getString(R.string.wishlist_up_cd_fmt, p.name)
            b.shopBtnWishUp.setOnClickListener {
                val i = holder.bindingAdapterPosition
                if (i > 0) vm.session.setWishlistOrder(rows.map { it.id }.toMutableList().also { it.add(i - 1, it.removeAt(i)) })
            }
        }
    }
}
