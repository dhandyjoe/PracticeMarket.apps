package com.automation.practice.feature.shop

import android.content.res.ColorStateList
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import com.automation.practice.R
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus

@StringRes
fun OrderStatus.label() = when (this) {
    OrderStatus.WAITING -> R.string.ord_status_waiting
    OrderStatus.PAID -> R.string.ord_status_paid
    OrderStatus.PACKED -> R.string.ord_status_packed
    OrderStatus.SHIPPED -> R.string.ord_status_shipped
    OrderStatus.DONE -> R.string.ord_status_done
    OrderStatus.CANCELLED -> R.string.ord_status_cancelled
}

/** Warna pil status pesanan (DESIGN.md §4.8): latar + teks. */
private fun OrderStatus.pill(): Pair<Int, Int> = when (this) {
    OrderStatus.WAITING -> R.drawable.bg_status_warning to R.color.pm_warning_strong
    OrderStatus.PAID, OrderStatus.PACKED, OrderStatus.SHIPPED -> R.drawable.bg_status_info to R.color.pm_img_blue_ink
    OrderStatus.DONE -> R.drawable.bg_status_ok to R.color.pm_primary_pressed
    OrderStatus.CANCELLED -> R.drawable.bg_status_muted to R.color.pm_ink_muted
}

fun TextView.showStatus(status: OrderStatus) {
    val (bg, fg) = status.pill()
    setText(status.label())
    setBackgroundResource(bg)
    setTextColor(context.getColor(fg))
}

/** Tab Pesanan Saya: label + status yang masuk tab (kosong = semua), urutan PRD FR-ORD-01. */
val ORDER_TABS: List<Pair<Int, Set<OrderStatus>>> = listOf(
    R.string.ord_tab_all to emptySet(),
    R.string.ord_tab_waiting to setOf(OrderStatus.WAITING),
    R.string.ord_tab_processing to setOf(OrderStatus.PAID, OrderStatus.PACKED),
    R.string.ord_tab_shipped to setOf(OrderStatus.SHIPPED),
    R.string.ord_tab_done to setOf(OrderStatus.DONE),
    R.string.ord_tab_cancelled to setOf(OrderStatus.CANCELLED),
)

fun List<Order>.forTab(tab: Int) = ORDER_TABS[tab].second.let { s -> if (s.isEmpty()) this else filter { it.status in s } }

/** Thumbnail produk pertama pesanan. */
fun ShopViewModel.showThumb(order: Order, bg: FrameLayout, art: ImageView) {
    val product = catalog.product(order.lines.first().productId) ?: return
    val v = visual(product)
    val ctx = bg.context
    bg.setBackgroundResource(R.drawable.bg_thumb_round)
    bg.backgroundTintList = ColorStateList.valueOf(ctx.getColor(v.bg))
    art.setImageResource(v.icon)
    art.imageTintList = ColorStateList.valueOf(ctx.getColor(v.ink))
}
