package com.automation.practice.feature.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.automation.practice.MainActivity
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.core.ui.applySystemBarsPadding
import com.automation.practice.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch

interface AuthNavigator {
    fun showLogin(prefill: String? = null, flashRes: Int = 0)
    fun showRegister()
    fun showForgotPassword()
    fun showNewPassword()
    fun showTerms()
    fun onboardingDone(toHome: Boolean)
    fun onLoggedIn(showOnboarding: Boolean)
}

class AuthActivity : AppCompatActivity(), AuthNavigator {
    private val settings get() = app.container.settings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()
        if (savedInstanceState == null) route(intent)
    }

    // shortcut: deep link ditangani di sini; pindah ke DeepLinkRouter tunggal saat milestone deep link (ARCHITECTURE.md §11).
    private fun route(intent: Intent) {
        val uri = intent.data
        when (uri?.host) {
            "register" -> replaceRoot(RegisterFragment())
            "forgot-password" -> replaceRoot(OtpFragment())
            "login" -> showLogin(prefill = uri.getQueryParameter("username"))
            else -> if (settings.snapshot().onboardingSeen) showLogin() else replaceRoot(OnboardingFragment.newInstance(toHome = false))
        }
    }

    override fun showLogin(prefill: String?, flashRes: Int) = replaceRoot(LoginFragment.newInstance(prefill, flashRes))
    override fun showRegister() = push(RegisterFragment())
    override fun showForgotPassword() = push(OtpFragment())
    override fun showNewPassword() = push(NewPasswordFragment())
    override fun showTerms() = startActivity(Intent(this, TermsActivity::class.java))

    override fun onboardingDone(toHome: Boolean) {
        lifecycleScope.launch {
            settings.setOnboardingSeen(true)
            if (toHome) goHome() else showLogin()
        }
    }

    override fun onLoggedIn(showOnboarding: Boolean) {
        if (showOnboarding) replaceRoot(OnboardingFragment.newInstance(toHome = true)) else goHome()
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun replaceRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.commit { replace(R.id.auth_container, fragment) }
    }

    private fun push(fragment: Fragment) = supportFragmentManager.commit {
        replace(R.id.auth_container, fragment)
        addToBackStack(null)
    }
}
