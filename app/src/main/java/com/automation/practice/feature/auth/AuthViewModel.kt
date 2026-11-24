package com.automation.practice.feature.auth

import android.app.Application
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.data.repo.AuthError
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

sealed interface RegisterEvent {
    data class Done(val email: String) : RegisterEvent
    data class Failed(@StringRes val messageRes: Int) : RegisterEvent
}

/**
 * Satu ViewModel untuk seluruh alur Auth (scope Activity), supaya hitungan gagal login,
 * countdown kunci bertahan saat rotasi.
 * shortcut: pecah per layar bila alur Auth bertambah besar.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.app.container

    private val _login = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _lockLeft = MutableStateFlow(0)
    val lockLeft: StateFlow<Int> = _lockLeft.asStateFlow()

    private val _events = Channel<Unit>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _registering = MutableStateFlow(false)
    val registering: StateFlow<Boolean> = _registering.asStateFlow()

    private val _registerEvents = Channel<RegisterEvent>(Channel.BUFFERED)
    val registerEvents = _registerEvents.receiveAsFlow()

    private var failedLogins = 0
    private var lockJob: Job? = null

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
                is LoginResult.Failed -> {
                    _login.value = LoginUiState(bannerRes = messageRes(result.error))
                    // Hanya kredensial salah yang dihitung untuk kunci 30 detik (PRD FR-AUTH-07).
                    if (result.error == AuthError.INVALID_CREDENTIALS && ++failedLogins >= AuthRules.MAX_FAILED_LOGINS) startLock()
                }
            }
        }
    }

    fun editedUsername() = _login.update { it.copy(usernameError = false) }
    fun editedPassword() = _login.update { it.copy(passwordError = false) }

    private suspend fun finishLogin(user: User, remember: Boolean) {
        container.settings.setRememberedUsername(if (remember) user.email else null)
        _login.value = LoginUiState()
        _events.send(Unit)
    }

    private fun startLock() {
        lockJob?.cancel()
        lockJob = countdown(_lockLeft, AuthRules.LOCK_SECONDS) {
            failedLogins = 0
            _login.value = LoginUiState()
        }
    }

    /** Hasil dikirim lewat [registerEvents] supaya tidak hilang saat rotasi. */
    fun register(name: String, email: String, password: String) {
        if (_registering.value) return
        _registering.value = true
        viewModelScope.launch {
            val e = email.trim()
            val error = container.auth.register(name.trim(), e, password)
            _registering.value = false
            // Email ditolak Firebase saat Register = format email, bukan kredensial salah.
            val event = when (error) {
                null -> RegisterEvent.Done(e)
                AuthError.INVALID_CREDENTIALS -> RegisterEvent.Failed(R.string.error_email_invalid)
                else -> RegisterEvent.Failed(messageRes(error))
            }
            _registerEvents.send(event)
        }
    }

    @StringRes
    private fun messageRes(error: AuthError) = when (error) {
        AuthError.INVALID_CREDENTIALS -> R.string.login_error_invalid
        AuthError.EMAIL_TAKEN -> R.string.error_email_taken
        AuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
        AuthError.NETWORK -> R.string.error_network
        AuthError.FAILED -> R.string.load_failed
    }

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
