package com.automation.practice

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.webkit.WebView
import com.automation.practice.di.AppContainer

class App : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Context WEBVIEW_com.automation.practice harus terlihat di Appium (ARCHITECTURE.md §12).
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}

val Context.app: App get() = applicationContext as App
