package com.automation.practice

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.automation.practice.data.prefs.Prefs
import com.automation.practice.di.AppContainer

class App : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        applyAppearance(container.settings.prefs.value)
        // Context WEBVIEW_com.automation.practice harus terlihat di Appium (ARCHITECTURE.md §12).
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}

/** Terapkan Tema dan Bahasa per-app dari pengaturan (FR-ACC-06). */
fun applyAppearance(p: Prefs) {
    AppCompatDelegate.setDefaultNightMode(
        when (p.theme) { 1 -> AppCompatDelegate.MODE_NIGHT_NO; 2 -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM },
    )
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(p.language))
}

val Context.app: App get() = applicationContext as App
