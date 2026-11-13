package com.automation.practice.feature.shop

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.automation.practice.R
import com.automation.practice.domain.Product

/** Placeholder gambar produk: latar berwarna + ikon (DESIGN.md §4.18). Belum ada aset foto. */
class Visual(@ColorRes val bg: Int, @ColorRes val ink: Int, @DrawableRes val icon: Int)

private val PALETTES = listOf(
    R.color.pm_img_green_bg to R.color.pm_img_green_ink,
    R.color.pm_img_blue_bg to R.color.pm_img_blue_ink,
    R.color.pm_img_orange_bg to R.color.pm_img_orange_ink,
    R.color.pm_img_purple_bg to R.color.pm_img_purple_ink,
)

/** [glitch] menukar gambar P003 dan P004 (bug `glitch_user`). */
fun Product.visual(glitch: Boolean): Visual {
    val icon = when {
        id == "P020" -> R.drawable.ic_prod_watch
        "Kaos" in name -> R.drawable.ic_prod_shirt
        categorySlug == "elektronik" -> R.drawable.ic_prod_phones
        categorySlug == "rumah-tangga" -> R.drawable.ic_prod_home
        categorySlug == "olahraga" -> R.drawable.ic_prod_shoe
        categorySlug == "buku" -> R.drawable.ic_prod_book
        categorySlug == "kecantikan" -> R.drawable.ic_prod_sparkle
        else -> R.drawable.ic_prod_shirt
    }
    if (!inStock) return Visual(R.color.pm_img_gray_bg, R.color.pm_img_gray_ink, icon)
    var n = id.drop(1).toInt()
    if (glitch) n = when (n) { 3 -> 4; 4 -> 3; else -> n }
    val (bg, ink) = PALETTES[(n - 1) % PALETTES.size]
    return Visual(bg, ink, icon)
}

fun Visual.applyTo(background: View, art: ImageView) {
    val ctx = background.context
    background.setBackgroundColor(ctx.getColor(bg))
    art.setImageResource(icon)
    art.imageTintList = ColorStateList.valueOf(ctx.getColor(ink))
}
