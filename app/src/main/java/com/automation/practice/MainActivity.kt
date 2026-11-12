package com.automation.practice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.automation.practice.core.ui.applySystemBarsPadding
import com.automation.practice.databinding.ActivityMainBinding
import com.automation.practice.feature.auth.AuthActivity

/**
 * Launcher (`appActivity=.MainActivity`, PRD §14). Tanpa sesi login, langsung ke Auth;
 * ini juga menangani proses baru setelah kill (ARCHITECTURE.md §6.3).
 * TODO SHOP-01: ganti isi layar ini dengan Beranda + bottom navigation.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val auth = app.container.auth
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.shopTxtGreeting.text = getString(R.string.home_greeting_fmt, user.displayName)
        b.shopBtnLogout.setOnClickListener {
            auth.logout()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}
