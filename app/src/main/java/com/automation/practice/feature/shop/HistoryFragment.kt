package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.compose.Pm
import com.automation.practice.core.ui.compose.PmChip
import com.automation.practice.core.ui.compose.PmIconButton
import com.automation.practice.core.ui.compose.PmScreen
import com.automation.practice.core.ui.compose.PmText
import com.automation.practice.core.ui.compose.PmTopBar
import com.automation.practice.domain.HISTORY_PAGE
import com.automation.practice.domain.Tx
import com.automation.practice.domain.filterHistory
import com.automation.practice.domain.formatDate
import com.automation.practice.domain.formatHm
import com.automation.practice.domain.formatHms
import com.automation.practice.domain.historyBacklog
import com.automation.practice.domain.toRupiah
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PAY-06 Riwayat Transaksi, Compose **tanpa testTag** sebagai pembanding locator (FR-PAY-11):
 * chip Semua/Masuk/Keluar, rentang tanggal, sticky header tanggal, infinite scroll 20 per halaman, tarik-segarkan.
 */
class HistoryFragment : ShopFragment(0) {
    private var range by mutableStateOf<LongRange?>(null)
    private var refreshing by mutableStateOf(false)
    private var updatedAt by mutableStateOf(0L)
    private val backlog = historyBacklog()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val appContainer = requireContext().app.container
        updatedAt = appContainer.now()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PmScreen(tags = false) {
                    val session by vm.session.transactions.collectAsState()
                    val scope = rememberCoroutineScope()
                    HistoryScreen(
                        all = session + backlog, range = range, refreshing = refreshing, updatedAt = updatedAt,
                        onBack = { nav.back() }, onPickRange = ::pickRange, onClearRange = { range = null },
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                delay(600L * appContainer.settings.prefs.value.latency)
                                if (appContainer.serverFails()) Snackbar.make(requireView(), R.string.load_failed, Snackbar.LENGTH_LONG).show()
                                else updatedAt = appContainer.now()
                                refreshing = false
                            }
                        },
                    )
                }
            }
        }
    }

    private fun pickRange() {
        MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.pay_range_title).build().also { dp ->
            dp.addOnPositiveButtonClickListener { sel ->
                // Rentang dari picker dalam UTC tengah malam; ambil hari penuh [awal, akhir+1 hari).
                range = sel.first..(sel.second + DAY_MS - 1)
            }
        }.show(childFragmentManager, "range")
    }

    private companion object {
        const val DAY_MS = 24L * 3_600_000L
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    all: List<Tx>, range: LongRange?, refreshing: Boolean, updatedAt: Long,
    onBack: () -> Unit, onPickRange: () -> Unit, onClearRange: () -> Unit, onRefresh: () -> Unit,
) {
    var kind by remember { mutableIntStateOf(0) }
    var limit by remember { mutableIntStateOf(HISTORY_PAGE) }
    val filtered = filterHistory(all, kind, range?.let { r -> (r.first - OFFSET_UTC_TO_WIB)..(r.last - OFFSET_UTC_TO_WIB) })
    LaunchedEffect(kind, range) { limit = HISTORY_PAGE }
    val page = filtered.take(limit)
    val listState = rememberLazyListState()
    LaunchedEffect(listState, filtered.size) {
        snapshotFlow { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to listState.layoutInfo.totalItemsCount }.collect { (last, total) ->
            if (total > 0 && last >= total - 3 && limit < filtered.size) { delay(400); limit += HISTORY_PAGE }
        }
    }

    Column(Modifier.fillMaxSize().background(Pm.surface)) {
        PmTopBar(stringResource(R.string.pay_history_title), onBack, tags = false) {
            PmIconButton(R.drawable.ic_calendar, stringResource(R.string.pay_range_cd), onPickRange)
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(R.string.pay_filter_all, R.string.pay_filter_in, R.string.pay_filter_out).forEachIndexed { i, res ->
                PmChip(stringResource(res), kind == i, { kind = i }, dark = true)
            }
            if (range != null) PmChip(stringResource(R.string.pay_range_clear), false, onClearRange)
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                PmText(stringResource(R.string.pay_updated_fmt, formatHms(updatedAt)), 12, color = Pm.muted, modifier = Modifier.fillMaxWidth().padding(6.dp))
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    page.groupBy { formatDate(it.at) }.forEach { (date, items) ->
                        stickyHeader { PmText(date, 13, FontWeight.Bold, Pm.inkSecondary, modifier = Modifier.fillMaxWidth().background(Pm.background).padding(horizontal = 20.dp, vertical = 8.dp)) }
                        items(items.size, key = { items[it].id }) { i -> TxRow(items[i]) }
                    }
                    if (page.isEmpty()) item { PmText(stringResource(R.string.pay_history_empty), 14, color = Pm.muted, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
                    if (limit < filtered.size) item {
                        PmText(stringResource(R.string.pay_loading_more), 13, color = Pm.muted, modifier = Modifier.fillMaxWidth().padding(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TxRow(t: Tx) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (t.incoming) Pm.container else Pm.subtle),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(if (t.incoming) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(if (t.incoming) Pm.pressed else Pm.inkSecondary))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            PmText(t.title, 14, FontWeight.SemiBold)
            PmText(formatHm(t.at), 12, color = Pm.muted)
        }
        PmText((if (t.incoming) "+" else "−") + kotlin.math.abs(t.amount).toRupiah(), 14, FontWeight.Bold, if (t.incoming) Pm.pressed else Pm.ink)
    }
}

/** DatePicker memberi UTC tengah malam; riwayat memakai WIB (UTC+7), jadi geser agar hari penuh WIB tercakup. */
private const val OFFSET_UTC_TO_WIB = 7L * 3_600_000L
