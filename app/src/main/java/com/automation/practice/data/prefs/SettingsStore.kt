package com.automation.practice.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Pengaturan umum (FR-ACC-06) dan Developer Options (PRD §10); semuanya bertahan setelah app di-kill. */
data class Prefs(
    /** 0 = ikuti sistem, 1 = terang, 2 = gelap. */
    val theme: Int = 0,
    /** Kode bahasa per-app; kosong = ikuti sistem. */
    val language: String = "",
    val orderNotifications: Boolean = true,
    val persistData: Boolean = false,
    val deterministic: Boolean = false,
    /** Pengali latency: 0, 1, 2, atau 3. */
    val latency: Int = 1,
    val animations: Boolean = true,
    /** Peluang error server dalam persen: 0, 30, 50. */
    val flaky: Int = 30,
    val vaWindowMs: Long = 15 * 60_000L,
    /** Interval status pesanan (ms); 0 = Manual. */
    val statusIntervalMs: Long = 10_000L,
    val locatorHints: Boolean = false,
)

/** Pengaturan yang bertahan setelah app di-kill (ARCHITECTURE.md D2). */
class SettingsStore(private val context: Context) {
    data class Snapshot(
        val onboardingSeen: Boolean,
        val rememberedUsername: String?,
        val biometricUser: String?,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // shortcut: dibaca blocking sekali saat start agar pengaturan sinkron tersedia di UI; pindah ke Flow bila datastore membesar.
    private val _prefs = MutableStateFlow(runBlocking { context.dataStore.data.first().toPrefs() })
    val prefs: StateFlow<Prefs> = _prefs.asStateFlow()

    /** Terapkan perubahan ke memori seketika, lalu tulis ke DataStore di latar. */
    fun update(f: (Prefs) -> Prefs) {
        val next = f(_prefs.value)
        _prefs.value = next
        scope.launch { context.dataStore.edit { it.write(next) } }
    }

    fun resetPrefs() = update { Prefs() }

    private fun Preferences.toPrefs(): Prefs {
        val d = Prefs()
        return Prefs(
            this[THEME] ?: d.theme, this[LANGUAGE] ?: d.language, this[NOTIF] ?: d.orderNotifications,
            this[PERSIST] ?: d.persistData, this[DETERMINISTIC] ?: d.deterministic, this[LATENCY] ?: d.latency,
            this[ANIMATIONS] ?: d.animations, this[FLAKY] ?: d.flaky, this[VA_WINDOW] ?: d.vaWindowMs,
            this[STATUS_INTERVAL] ?: d.statusIntervalMs, this[HINTS] ?: d.locatorHints,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(p: Prefs) {
        this[THEME] = p.theme; this[LANGUAGE] = p.language; this[NOTIF] = p.orderNotifications
        this[PERSIST] = p.persistData; this[DETERMINISTIC] = p.deterministic; this[LATENCY] = p.latency
        this[ANIMATIONS] = p.animations; this[FLAKY] = p.flaky; this[VA_WINDOW] = p.vaWindowMs
        this[STATUS_INTERVAL] = p.statusIntervalMs; this[HINTS] = p.locatorHints
    }

    // shortcut: baca blocking di main thread agar layar awal tahu tujuan tanpa flicker; pindah ke Flow bila datastore membesar.
    fun snapshot(): Snapshot = runBlocking {
        context.dataStore.data.first().let {
            Snapshot(it[ONBOARDING_SEEN] ?: false, it[REMEMBERED_USERNAME], it[BIOMETRIC_USER])
        }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[ONBOARDING_SEEN] = seen }
    }

    suspend fun setRememberedUsername(username: String?) {
        context.dataStore.edit { if (username == null) it.remove(REMEMBERED_USERNAME) else it[REMEMBERED_USERNAME] = username }
    }

    suspend fun setBiometricUser(username: String) {
        context.dataStore.edit { it[BIOMETRIC_USER] = username }
    }

    private companion object {
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val REMEMBERED_USERNAME = stringPreferencesKey("remembered_username")
        val BIOMETRIC_USER = stringPreferencesKey("biometric_user")
        val THEME = intPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIF = booleanPreferencesKey("order_notifications")
        val PERSIST = booleanPreferencesKey("persist_data")
        val DETERMINISTIC = booleanPreferencesKey("deterministic")
        val LATENCY = intPreferencesKey("latency")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val FLAKY = intPreferencesKey("flaky")
        val VA_WINDOW = longPreferencesKey("va_window_ms")
        val STATUS_INTERVAL = longPreferencesKey("status_interval_ms")
        val HINTS = booleanPreferencesKey("locator_hints")
    }
}
