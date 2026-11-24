package com.automation.practice.di

import android.app.Application
import android.util.Log
import com.automation.practice.BuildConfig
import com.automation.practice.data.firebase.FirestoreShopStore
import com.automation.practice.data.prefs.SettingsStore
import com.automation.practice.data.repo.AuthRepository
import com.automation.practice.data.repo.CatalogRepository
import com.automation.practice.data.repo.OrderSimulator
import com.automation.practice.data.repo.ShopSession
import com.automation.practice.data.repo.User
import com.automation.practice.domain.VA_EXPIRED_REASON
import com.automation.practice.domain.jakartaTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.automation.practice.feature.shop.OrderNotifier
import kotlinx.coroutines.CancellationException
import java.util.Calendar
import java.util.TimeZone

class AppContainer(private val app: Application) {
    val settings = SettingsStore(app)

    private val emulator = BuildConfig.FIREBASE_EMULATOR
    private val firestore = FirebaseFirestore.getInstance().also { if (emulator.isNotEmpty()) it.useEmulator(emulator, 8080) }
    val auth = AuthRepository(FirebaseAuth.getInstance().also { if (emulator.isNotEmpty()) it.useEmulator(emulator, 9099) }, firestore)
    val catalog = CatalogRepository(app.assets)

    /** Jam tampilan: mode deterministik memakai tanggal tetap 2026-01-15 dengan jam sungguhan (PRD §10). */
    val now: () -> Long = {
        val real = System.currentTimeMillis()
        if (!settings.prefs.value.deterministic) real
        else Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).let {
            it.timeInMillis = real
            jakartaTime(2026, 1, 15, it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE], it[Calendar.SECOND])
        }
    }

    /** Jam asli untuk semua waktu yang ditulis ke Firestore dan dibandingkan dengannya (contract §5). */
    val realNow: () -> Long = System::currentTimeMillis

    private val store = FirestoreShopStore(firestore, catalog::seedStock)

    val shop = ShopSession({ l -> catalog.product(l.productId)?.stockOf(l.color, l.size) ?: 0 }, realNow, store, catalog::applyStock)

    /** Muat stok global & data akun dari Firestore (contract §8.1). false bila gagal (tanpa cache & tanpa jaringan). */
    suspend fun loadAccount(user: User): Boolean = try {
        catalog.applyStock(store.loadStock())
        shop.load(user.uid, user.displayName, user.email)
        true
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w("AppContainer", "data akun gagal dimuat", e)
        false
    }

    /** Tarik-segarkan: baca ulang stok & data akun dari Firestore (contract §7). */
    suspend fun refresh(): Boolean = auth.currentUser?.let { loadAccount(it) } ?: false

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
        shop, settings, realNow,
        onAdvanced = { id, status -> shop.order(id)?.let { OrderNotifier.postStatus(app, it, status) } },
        onExpired = { shop.cancelOrder(it, VA_EXPIRED_REASON) },
    ).also { it.start() }
}
