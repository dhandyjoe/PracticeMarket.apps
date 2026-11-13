package com.automation.practice.feature.shop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentCartBinding
import com.automation.practice.databinding.ItemCartBinding
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.VOUCHERS
import com.automation.practice.domain.itemDiscount
import com.automation.practice.domain.toRupiah
import com.automation.practice.domain.voucherError
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** SHOP-09 Keranjang: pilih item, ubah jumlah, simpan untuk nanti, hapus dengan geser + Urungkan. */
class CartFragment : ShopFragment(R.layout.fragment_cart) {
    private var lines = emptyList<CartLine>()
    private lateinit var adapter: RowAdapter
    private var _b: FragmentCartBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentCartBinding.bind(view)
        b.shopHeaderCart.applySystemInsets(top = true)

        adapter = RowAdapter()
        b.shopListCart.layoutManager = LinearLayoutManager(requireContext())
        b.shopListCart.adapter = adapter
        ItemTouchHelper(SwipeToDelete()).attachToRecyclerView(b.shopListCart)

        b.shopChkSelectAll.setOnClickListener {
            val selectable = lines.filterNot { it.saved }
            vm.session.setAllSelected(selectable.isNotEmpty() && !selectable.all { it.selected })
            render()
        }
        b.shopBtnVoucher.setOnClickListener { nav.openVoucher() }
        b.shopBtnStartShopping.setOnClickListener { nav.selectTab(Tab.HOME) }
        b.shopBtnCheckout.setOnClickListener {
            vm.startCheckout()
            nav.openCheckout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.session.cart.combine(vm.session.voucher) { cart, _ -> cart }.collect {
                    lines = it
                    render()
                }
            }
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private fun render() {
        val selected = lines.filter { it.selected && !it.saved }
        val selectable = lines.filterNot { it.saved }

        b.shopContentCart.isVisible = lines.isNotEmpty()
        b.shopEmptyCart.isVisible = lines.isEmpty()
        b.shopBarCartFooter.isVisible = lines.isNotEmpty()
        adapter.notifyDataSetChanged()

        b.shopChkSelectAll.checkedState = when {
            selected.isEmpty() -> MaterialCheckBox.STATE_UNCHECKED
            selected.size == selectable.size -> MaterialCheckBox.STATE_CHECKED
            else -> MaterialCheckBox.STATE_INDETERMINATE
        }
        b.shopTxtSelectedCount.text = getString(R.string.cart_selected_fmt, selected.size, lines.size)

        val subtotal = vm.subtotal(selected)
        val code = vm.session.voucher.value?.takeIf { voucherError(it, subtotal) == null }
        val discount = itemDiscount(subtotal, code)
        b.shopTxtCartSubtotalLabel.text = getString(R.string.cart_subtotal_fmt, selected.sumOf { it.qty })
        b.shopTxtCartSubtotal.text = subtotal.toRupiah()
        b.shopRowCartDiscount.isVisible = discount > 0
        b.shopTxtCartDiscount.text = getString(R.string.cart_voucher_applied_off_fmt, discount.toRupiah())
        b.shopTxtCartTotal.text = (subtotal - discount).toRupiah()
        b.shopBtnCheckout.text = getString(R.string.cart_checkout_fmt, selected.size)
        b.shopBtnCheckout.isEnabled = selected.isNotEmpty()

        if (code == null) {
            b.shopTxtVoucherTitle.setText(R.string.cart_voucher_pick)
            b.shopTxtVoucherNote.setText(R.string.cart_voucher_pick_note)
            b.shopTxtVoucherOff.text = ""
        } else {
            b.shopTxtVoucherTitle.text = getString(R.string.cart_voucher_applied_fmt, code)
            b.shopTxtVoucherNote.text = VOUCHERS.first { it.code == code }.let { "${it.title} · ${it.note}" }
            b.shopTxtVoucherOff.text =
                if (discount > 0) getString(R.string.cart_voucher_applied_off_fmt, discount.toRupiah()) else getString(R.string.cart_voucher_shipping)
        }
    }

    private fun onSwipe(position: Int) {
        val line = lines[position]
        // Bug glitch_user: badge tidak ikut turun setelah hapus (PRD §8.8 no. 5).
        vm.session.remove(line, keepBadge = vm.glitch)
        val name = vm.catalog.product(line.productId)?.name.orEmpty()
        Snackbar.make(requireView(), getString(R.string.cart_deleted_fmt, name), UNDO_MS)
            .setAction(R.string.cart_undo) { vm.session.restore(line, position) }
            .show()
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowAdapter.Holder>() {
        inner class Holder(val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = lines.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val line = lines[position]
            val p = vm.catalog.product(line.productId) ?: return
            val ctx = holder.itemView.context
            val v = holder.b
            vm.visual(p).applyTo(v.shopImgCartBg, v.shopImgCart)
            v.shopTxtCartName.text = p.name
            v.shopTxtCartVariant.text = variantLabel(ctx, line)
            v.shopTxtCartPrice.text = vm.lineTotal(line).toRupiah()
            v.shopTxtCartQty.text = line.qty.toString()

            v.shopChkCartItem.contentDescription = ctx.getString(R.string.cart_select_item_cd_fmt, p.name)
            v.shopChkCartItem.isChecked = line.selected && !line.saved
            v.shopChkCartItem.isEnabled = !line.saved
            v.shopChkCartItem.setOnClickListener { vm.session.setSelected(line, v.shopChkCartItem.isChecked) }

            v.shopBtnCartSave.setText(if (line.saved) R.string.cart_move_back else R.string.cart_save_later)
            v.shopBtnCartSave.setOnClickListener { vm.session.toggleSaved(line) }

            v.shopItemCartRow.alpha = if (line.saved) SAVED_ALPHA else 1f
            v.shopBtnCartMinus.isEnabled = line.qty > 1
            v.shopBtnCartMinus.setOnClickListener { vm.session.setQty(line, line.qty - 1) }
            v.shopBtnCartPlus.isEnabled = true
            v.shopBtnCartPlus.setOnClickListener {
                if (line.qty < vm.session.maxQty(line)) vm.session.setQty(line, line.qty + 1)
                else Snackbar.make(requireView(), R.string.product_qty_max, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /** Geser ke kiri menampilkan latar merah "Hapus" (DESIGN SHOP-09). */
    private inner class SwipeToDelete : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(requireContext(), R.color.pm_error)
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(requireContext(), R.color.pm_on_primary)
            textSize = resources.displayMetrics.scaledDensity * 13
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        private val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!.mutate().apply {
            setTint(ContextCompat.getColor(requireContext(), R.color.pm_on_primary))
        }

        override fun onMove(rv: RecyclerView, h: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = onSwipe(holder.bindingAdapterPosition)

        override fun onChildDraw(c: Canvas, rv: RecyclerView, h: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
            val item = h.itemView
            if (dX < 0) {
                val radius = resources.getDimension(R.dimen.pm_radius_card)
                c.drawRoundRect(RectF(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat()), radius, radius, paint)
                val cx = item.right - resources.getDimension(R.dimen.pm_space_12)
                val cy = (item.top + item.bottom) / 2f
                val half = resources.getDimensionPixelSize(R.dimen.pm_space_6) / 2
                icon.setBounds((cx - half).toInt(), (cy - half * 1.6f).toInt(), (cx + half).toInt(), (cy + half * 0.4f).toInt())
                if (-dX > (item.right - cx) * ICON_REVEAL) {
                    icon.draw(c)
                    c.drawText(getString(R.string.cart_delete), cx, cy + half * 1.4f, labelPaint)
                }
            }
            super.onChildDraw(c, rv, h, dX, dY, state, active)
        }
    }

    private companion object {
        const val UNDO_MS = 4_000
        const val SAVED_ALPHA = 0.6f
        const val ICON_REVEAL = 0.9f
    }
}

/** `Hitam · L`, `Ukuran 42`, atau kosong untuk produk tanpa varian. */
fun variantLabel(ctx: android.content.Context, line: CartLine) = when {
    line.color.isNotEmpty() && line.size.isNotEmpty() -> ctx.getString(R.string.cart_variant_color_size_fmt, line.color, line.size)
    line.size.isNotEmpty() -> ctx.getString(R.string.cart_variant_size_fmt, line.size)
    else -> line.color
}
