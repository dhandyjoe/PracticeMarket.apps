package com.automation.practice.di

import android.app.Application
import com.automation.practice.data.db.AppDatabase
import com.automation.practice.data.db.RoomShopStore
import com.automation.practice.data.prefs.SettingsStore
import com.automation.practice.data.repo.AuthRepository
import com.automation.practice.data.repo.CatalogRepository
import com.automation.practice.data.repo.OrderSimulator
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.jakartaTime
import com.automation.practice.feature.shop.OrderNotifier
import java.util.Calendar
import java.util.TimeZone

class AppContainer(private val app: Application) {
    val settings = SettingsStore(app)

    /** Database lokal persisten (ARCHITECTURE.md §6). Toggle "Reset data setiap app dibuka" mengosongkannya tiap cold start. */
    private val db = AppDatabase.open(app).also { if (settings.prefs.value.resetOnLaunch) it.clearAllTables() }
    val auth = AuthRepository(app.assets, db.dao())
    val catalog = CatalogRepository(app.assets, db.dao())

    /** Jam aplikasi: mode deterministik memakai tanggal tetap 2026-01-15 dengan jam sungguhan (PRD §10). */
    val now: () -> Long = {
        val real = System.currentTimeMillis()
        if (!settings.prefs.value.deterministic) real
        else Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).let {
            it.timeInMillis = real
            jakartaTime(2026, 1, 15, it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND])
        }
    }

    val shop = ShopSession({ l -> catalog.product(l.productId)?.stockOf(l.color, l.size) ?: 0 }, now, RoomShopStore(db))

    /** Batalkan pesanan dan kembalikan stok (PRD FR-ORD-05). Aman dipanggil dua kali. */
    fun cancelOrder(id: String, reason: String = "") {
        val order = shop.order(id) ?: return
        if (order.status == OrderStatus.CANCELLED) return
        order.lines.forEach { catalog.adjustStock(it, it.qty) }
        shop.updateOrder(id, OrderStatus.CANCELLED, reason)
    }

    /** "Reset semua data" (PRD §10): database dikosongkan, akun & stok kembali ke seed, sesi login dan status onboarding dihapus. Pemanggil membuka Auth sesudahnya. */
    fun resetAll() {
        shop.reset()
        catalog.resetStock()
        auth.load()
        // shortcut: tulis blocking agar Auth langsung membaca onboarding belum dilihat; pindah ke Flow bila datastore membesar.
        kotlinx.coroutines.runBlocking { settings.setOnboardingSeen(false) }
    }

    private var requests = 0

    /**
     * Simulasi "Peluang error server" (PRD §10). Mode deterministik: permintaan pertama gagal, kedua berhasil, dst.
     * Dipakai tarik-segarkan Pesanan/Riwayat dan Cek Status VA.
     */
    fun serverFails(): Boolean {
        val p = settings.prefs.value
        if (p.flaky == 0) return false
        return if (p.deterministic) requests++ % 2 == 0 else kotlin.random.Random.nextInt(100) < p.flaky
    }

    val simulator = OrderSimulator(
        shop, settings, now,
        onAdvanced = { id, status -> shop.order(id)?.let { OrderNotifier.postStatus(app, it, status) } },
        onExpired = { cancelOrder(it) },
    ).also { it.start() }
}
