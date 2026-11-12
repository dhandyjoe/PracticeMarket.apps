package com.automation.practice.feature.auth

import android.app.Application
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.data.repo.Behavior
import com.automation.practice.data.repo.LoginResult
import com.automation.practice.data.repo.User
import com.automation.practice.domain.AuthRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    @StringRes val bannerRes: Int? = null,
    val usernameError: Boolean = false,
    val passwordError: Boolean = false,
)

data class LoggedIn(val showOnboarding: Boolean)

/**
 * Satu ViewModel untuk seluruh alur Auth (scope Activity), supaya hitungan gagal login,
 * countdown kunci/kirim ulang, dan email reset bertahan saat rotasi.
 * shortcut: pecah per layar bila alur Auth bertambah besar.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.app.container

    private val _login = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _lockLeft = MutableStateFlow(0)
    val lockLeft: StateFlow<Int> = _lockLeft.asStateFlow()

    private val _resendLeft = MutableStateFlow(0)
    val resendLeft: StateFlow<Int> = _resendLeft.asStateFlow()

    private val _resetEmail = MutableStateFlow<String?>(null)
    val resetEmail: StateFlow<String?> = _resetEmail.asStateFlow()

    private val _events = Channel<LoggedIn>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var failedLogins = 0
    private var lockJob: Job? = null
    private var resendJob: Job? = null

    fun login(username: String, password: String, remember: Boolean) {
        val name = username.trim()
        val usernameError = name.isEmpty()
        val passwordError = password.isEmpty()
        if (usernameError || passwordError) {
            _login.value = LoginUiState(usernameError = usernameError, passwordError = passwordError)
            return
        }
        if (_login.value.loading || _lockLeft.value > 0) return

        _login.value = LoginUiState(loading = true)
        viewModelScope.launch {
            when (val result = container.auth.login(name, password)) {
                is LoginResult.Success -> {
                    failedLogins = 0
                    finishLogin(result.user, remember)
                }
                LoginResult.Locked -> _login.value = LoginUiState(bannerRes = R.string.login_error_locked)
                LoginResult.Invalid -> {
                    _login.value = LoginUiState(bannerRes = R.string.login_error_invalid)
                    if (++failedLogins >= AuthRules.MAX_FAILED_LOGINS) startLock()
                }
            }
        }
    }

    fun loginWithBiometric() {
        val username = container.settings.snapshot().biometricUser ?: return
        viewModelScope.launch {
            container.auth.loginAs(username)?.let { finishLogin(it, remember = null) }
        }
    }

    fun biometricFailed() {
        _login.value = LoginUiState(bannerRes = R.string.biometric_failed)
    }

    fun editedUsername() = _login.update { it.copy(usernameError = false) }
    fun editedPassword() = _login.update { it.copy(passwordError = false) }

    private suspend fun finishLogin(user: User, remember: Boolean?) {
        if (remember != null) container.settings.setRememberedUsername(if (remember) user.username else null)
        container.settings.setBiometricUser(user.username)
        _login.value = LoginUiState()
        _events.send(LoggedIn(showOnboarding = user.behavior == Behavior.NEW))
    }

    private fun startLock() {
        lockJob?.cancel()
        lockJob = countdown(_lockLeft, AuthRules.LOCK_SECONDS) {
            failedLogins = 0
            _login.value = LoginUiState()
        }
    }

    /** Null bila sukses, selain itu id string error. */
    @StringRes
    fun register(name: String, email: String, password: String): Int? =
        if (container.auth.register(name.trim(), email.trim(), password) == null) R.string.error_email_taken else null

    /** Null bila sukses, selain itu id string error. */
    @StringRes
    fun sendCode(email: String): Int? {
        val e = email.trim()
        if (!AuthRules.isValidEmail(e)) return R.string.error_email_invalid
        if (container.auth.findByEmail(e) == null) return R.string.forgot_email_unknown
        _resetEmail.value = e
        resend()
        return null
    }

    fun resend() {
        resendJob?.cancel()
        resendJob = countdown(_resendLeft, AuthRules.RESEND_SECONDS)
    }

    fun resetFlow() {
        resendJob?.cancel()
        _resendLeft.value = 0
        _resetEmail.value = null
    }

    fun verifyOtp(code: String) = code == AuthRules.OTP_CODE

    fun changePassword(newPassword: String): Boolean =
        _resetEmail.value?.let { container.auth.changePassword(it, newPassword) } ?: false

    // Dihitung dari timestamp, bukan dikurangi 1 per tick, supaya tetap benar setelah rotasi (ARCHITECTURE.md §9).
    private fun countdown(out: MutableStateFlow<Int>, seconds: Int, onDone: () -> Unit = {}): Job =
        viewModelScope.launch {
            val end = SystemClock.elapsedRealtime() + seconds * 1000L
            while (true) {
                val left = ((end - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceAtLeast(0)
                out.value = left
                if (left == 0) break
                delay(250)
            }
            onDone()
        }
}
