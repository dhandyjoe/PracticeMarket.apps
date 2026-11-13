package com.automation.practice.data.repo

import com.automation.practice.data.prefs.SettingsStore
import com.automation.practice.domain.OrderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Memajukan status pesanan aktif tiap interval Developer Options (PRD FR-ORD-02) dan membatalkan VA yang lewat batas.
 * Mode Manual (interval 0) mematikan kemajuan otomatis; tombol "Status berikutnya" memanggil [ShopSession.advance].
 * [onAdvanced] dipakai untuk notifikasi; [onExpired] mengembalikan stok.
 */
class OrderSimulator(
    private val session: ShopSession,
    private val settings: SettingsStore,
    private val now: () -> Long,
    private val onAdvanced: (orderId: String, status: OrderStatus) -> Unit,
    private val onExpired: (orderId: String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        scope.launch {
            while (true) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    internal fun tick() {
        val interval = settings.prefs.value.statusIntervalMs
        val t = now()
        session.orders.value.forEach { o ->
            when {
                o.status == OrderStatus.WAITING -> if (o.vaDeadline in 1 until t) onExpired(o.id)
                interval > 0 && o.status.next() != null && t - o.statusAt >= interval ->
                    session.advance(o.id)?.let { onAdvanced(o.id, it) }
            }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
