package com.automation.practice.core.ui.compose

import androidx.annotation.ColorRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import com.automation.practice.R

/** Token DESIGN.md §11.2: warna dibaca dari res/values(-night) supaya satu sumber untuk Views dan Compose. */
object Pm {
    val Inter = FontFamily(Font(R.font.inter))
    val Grotesk = FontFamily(Font(R.font.space_grotesk))
    val Mono = FontFamily(Font(R.font.jetbrains_mono))

    @Composable fun c(@ColorRes id: Int): Color = colorResource(id)
    val primary @Composable get() = c(R.color.pm_primary)
    val onPrimary @Composable get() = c(R.color.pm_on_primary)
    val ink @Composable get() = c(R.color.pm_ink)
    val inkSecondary @Composable get() = c(R.color.pm_ink_secondary)
    val muted @Composable get() = c(R.color.pm_ink_muted)
    val background @Composable get() = c(R.color.pm_background)
    val surface @Composable get() = c(R.color.pm_surface)
    val subtle @Composable get() = c(R.color.pm_surface_subtle)
    val disabled @Composable get() = c(R.color.pm_surface_disabled)
    val outline @Composable get() = c(R.color.pm_outline)
    val outlineStrong @Composable get() = c(R.color.pm_outline_strong)
    val error @Composable get() = c(R.color.pm_error)
    val container @Composable get() = c(R.color.pm_primary_container)
    val pressed @Composable get() = c(R.color.pm_primary_pressed)
}

@Composable
fun PmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pm.primary, onPrimary = Pm.onPrimary, surface = Pm.surface, onSurface = Pm.ink,
            background = Pm.background, outline = Pm.outlineStrong, error = Pm.error,
        ),
        content = content,
    )
}

/** Akar layar PracticePay: resource-id dari testTag (PRD FR-PAY, ARCHITECTURE §10). Riwayat memanggil dengan [tags] false. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PmScreen(tags: Boolean = true, content: @Composable () -> Unit) {
    PmTheme {
        Box(Modifier.semantics { if (tags) testTagsAsResourceId = true }.background(Pm.background)) { content() }
    }
}

fun Modifier.tag(name: String?, enabled: Boolean = true): Modifier = if (name != null && enabled) testTag(name) else this

@Composable
fun PmText(text: String, size: Int = 15, weight: FontWeight = FontWeight.Normal, color: Color = Pm.ink, family: FontFamily = Pm.Inter, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, style = TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight))
}

@Composable
fun PmTopBar(title: String, onBack: () -> Unit, tags: Boolean = true, actions: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().background(Pm.surface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            PmIconButton(R.drawable.ic_arrow_back, stringBack(), onClick = onBack, tag = "pay_btn_back".takeIf { tags })
            PmText(title, 18, FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp).tag("pay_txt_title".takeIf { tags }))
            actions()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Pm.outline))
    }
}

@Composable private fun stringBack() = androidx.compose.ui.res.stringResource(R.string.nav_back)

@Composable
fun PmIconButton(icon: Int, description: String, onClick: () -> Unit, tint: Color = Pm.ink, tag: String? = null) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = description; role = Role.Button }.tag(tag),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(tint)) }
}

@Composable
fun PmPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null) {
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Pm.primary else Pm.disabled)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }.tag(tag),
        contentAlignment = Alignment.Center,
    ) { PmText(text, 15, FontWeight.Bold, if (enabled) Pm.onPrimary else Pm.muted) }
}

@Composable
fun PmOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tag: String? = null) {
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
            .background(Pm.surface).border(BorderStroke(1.5.dp, Pm.outlineStrong), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }.tag(tag),
        contentAlignment = Alignment.Center,
    ) { PmText(text, 15, FontWeight.Bold) }
}

/** Avatar inisial; palet 4 warna dari DESIGN.md §4.18. */
@Composable
fun PmAvatar(initials: String, index: Int, size: Dp = 44.dp) {
    val (bg, ink) = listOf(
        R.color.pm_img_green_bg to R.color.pm_img_green_ink, R.color.pm_img_blue_bg to R.color.pm_img_blue_ink,
        R.color.pm_img_orange_bg to R.color.pm_img_orange_ink, R.color.pm_img_purple_bg to R.color.pm_img_purple_ink,
    )[index % 4]
    Box(Modifier.size(size).clip(CircleShape).background(Pm.c(bg)), contentAlignment = Alignment.Center) {
        PmText(initials, 14, FontWeight.Bold, Pm.c(ink))
    }
}

@Composable
fun PmChip(text: String, selected: Boolean, onClick: () -> Unit, tag: String? = null, dark: Boolean = false) {
    val shape = RoundedCornerShape(19.dp)
    val bg = when { selected && dark -> Pm.ink; selected -> Pm.container; else -> Pm.surface }
    val fg = when { selected && dark -> Pm.surface; selected -> Pm.pressed; else -> Pm.ink }
    val stroke = when { selected && dark -> Color.Transparent; selected -> Pm.primary; else -> Pm.outlineStrong }
    Box(
        Modifier.height(38.dp).clip(shape).background(bg).border(BorderStroke(1.5.dp, stroke), shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp)
            .semantics { role = Role.RadioButton }.tag(tag),
        contentAlignment = Alignment.Center,
    ) { PmText(text, 13, FontWeight.SemiBold, fg) }
}
