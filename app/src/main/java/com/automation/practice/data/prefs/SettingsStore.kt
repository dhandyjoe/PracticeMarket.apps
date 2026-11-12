package com.automation.practice.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Pengaturan yang bertahan setelah app di-kill (ARCHITECTURE.md D2). */
class SettingsStore(private val context: Context) {
    data class Snapshot(
        val onboardingSeen: Boolean,
        val rememberedUsername: String?,
        val biometricUser: String?,
    )

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
    }
}
