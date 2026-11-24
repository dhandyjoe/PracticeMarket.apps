package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automation.practice.R
import com.automation.practice.core.ui.compose.Pm
import com.automation.practice.core.ui.compose.PmScreen
import com.automation.practice.core.ui.compose.PmText
import com.automation.practice.core.ui.compose.tag

/** SHOP-02 Kategori: grid kategori; tap membuka Hasil terfilter (sama seperti chip kategori di Beranda). */
class CategoryFragment : ShopFragment(0) {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PmScreen {
                    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
                        PmText(stringResource(R.string.nav_category), 22, FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                        LazyVerticalGrid(GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(vm.catalog.categories) { c ->
                                Box(
                                    Modifier.aspectRatio(1.6f).clip(RoundedCornerShape(14.dp)).background(Pm.surface)
                                        .border(BorderStroke(1.5.dp, Pm.outlineStrong), RoundedCornerShape(14.dp))
                                        .clickable { vm.browse(c.slug); nav.openResults() }.tag("shop_item_category"),
                                    contentAlignment = Alignment.Center,
                                ) { PmText(c.name, 16, FontWeight.SemiBold, modifier = Modifier.tag("shop_txt_category_name")) }
                            }
                        }
                    }
                }
            }
        }
}
