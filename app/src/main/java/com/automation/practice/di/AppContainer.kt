package com.automation.practice.di

import android.app.Application
import com.automation.practice.data.prefs.SettingsStore
import com.automation.practice.data.repo.AuthRepository

class AppContainer(app: Application) {
    val settings = SettingsStore(app)
    val auth = AuthRepository(app.assets)
}
