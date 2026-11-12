package com.automation.practice.feature.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val vm: AuthViewModel by activityViewModels()
    private val nav get() = requireActivity() as AuthNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentLoginBinding.bind(view)
        val prefs = requireContext().app.container.settings.snapshot()

        if (savedInstanceState == null) {
            b.shopInputUsername.setText(arguments?.getString(ARG_PREFILL) ?: prefs.rememberedUsername.orEmpty())
            arguments?.getInt(ARG_FLASH)?.takeIf { it != 0 }?.let { Snackbar.make(view, it, Snackbar.LENGTH_LONG).show() }
        }

        b.shopInputUsername.doAfterTextChanged { vm.editedUsername() }
        b.shopInputPassword.doAfterTextChanged { vm.editedPassword() }
        b.shopInputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit(b).let { true } else false
        }

        b.shopBtnTogglePassword.setOnClickListener {
            val show = !it.isSelected
            it.isSelected = show
            b.shopInputPassword.transformationMethod = if (show) null else PasswordTransformationMethod.getInstance()
            b.shopInputPassword.setSelection(b.shopInputPassword.text.length)
            b.shopBtnTogglePassword.contentDescription = getString(if (show) R.string.password_hide else R.string.password_show)
            b.shopBtnTogglePassword.setImageResource(if (show) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
        }

        b.shopBtnLogin.setOnClickListener { submit(b) }
        b.shopBtnForgotPassword.setOnClickListener { nav.showForgotPassword() }
        b.shopBtnRegisterLink.setOnClickListener { nav.showRegister() }

        // FR-AUTH-11: tersedia setelah login pertama dan bila perangkat punya sidik jari terdaftar.
        val canUseBiometric = prefs.biometricUser != null &&
            BiometricManager.from(requireContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
        b.shopBtnBiometric.isEnabled = canUseBiometric
        b.shopBtnBiometric.setOnClickListener { promptBiometric() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { combine(vm.loginState, vm.lockLeft, ::Pair).collect { (ui, lock) -> render(b, ui, lock) } }
                launch { vm.events.collect { nav.onLoggedIn(it.showOnboarding) } }
            }
        }
    }

    private fun submit(b: FragmentLoginBinding) =
        vm.login(b.shopInputUsername.text.toString(), b.shopInputPassword.text.toString(), b.shopChkRemember.isChecked)

    private fun render(b: FragmentLoginBinding, ui: LoginUiState, lockLeft: Int) {
        b.shopBannerLoginError.isVisible = ui.bannerRes != null
        ui.bannerRes?.let { b.shopTxtLoginError.setText(it) }

        b.shopInputUsername.isSelected = ui.usernameError
        b.shopTxtUsernameError.isVisible = ui.usernameError
        b.shopInputPassword.isSelected = ui.passwordError
        b.shopTxtPasswordError.isVisible = ui.passwordError

        val locked = lockLeft > 0
        b.shopBtnLogin.isEnabled = !locked && !ui.loading
        b.shopBtnLogin.text = when {
            ui.loading -> ""
            locked -> getString(R.string.login_locked_fmt, lockLeft)
            else -> getString(R.string.login_submit)
        }
        b.shopProgressLogin.isVisible = ui.loading
    }

    private fun promptBiometric() {
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setNegativeButtonText(getString(R.string.biometric_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = vm.loginWithBiometric()
                override fun onAuthenticationFailed() = vm.biometricFailed()
            },
        ).authenticate(info)
    }

    companion object {
        private const val ARG_PREFILL = "prefill"
        private const val ARG_FLASH = "flash"

        fun newInstance(prefill: String?, flashRes: Int) =
            LoginFragment().apply { arguments = bundleOf(ARG_PREFILL to prefill, ARG_FLASH to flashRes) }
    }
}
