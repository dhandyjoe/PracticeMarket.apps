package com.automation.practice.feature.auth

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.automation.practice.R
import com.automation.practice.databinding.FragmentNewPasswordBinding
import com.automation.practice.domain.AuthRules
import com.google.android.material.snackbar.Snackbar

class NewPasswordFragment : Fragment(R.layout.fragment_new_password) {
    private val vm: AuthViewModel by activityViewModels()
    private val nav get() = requireActivity() as AuthNavigator
    private var done = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentNewPasswordBinding.bind(view)
        done = savedInstanceState?.getBoolean(STATE_DONE) ?: false

        b.shopBtnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.shopInputNewPassword.doAfterTextChanged { render(b) }
        b.shopInputNewPasswordConfirm.doAfterTextChanged { render(b) }
        b.shopBtnSavePassword.setOnClickListener {
            if (vm.changePassword(b.shopInputNewPassword.text.toString())) {
                done = true
                render(b)
            } else {
                Snackbar.make(view, R.string.forgot_email_unknown, Snackbar.LENGTH_LONG).show()
            }
        }
        b.shopBtnGotoLogin.setOnClickListener { nav.showLogin() }
        render(b)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_DONE, done)
    }

    private fun render(b: FragmentNewPasswordBinding) {
        b.newpwForm.isVisible = !done
        b.newpwDone.isVisible = done

        val checks = AuthRules.passwordChecks(b.shopInputNewPassword.text.toString(), b.shopInputNewPasswordConfirm.text.toString())
        listOf(
            Triple(b.shopTxtRuleMin, b.shopTxtRuleMinIcon, checks.minLength),
            Triple(b.shopTxtRuleLetter, b.shopTxtRuleLetterIcon, checks.hasLetter),
            Triple(b.shopTxtRuleDigit, b.shopTxtRuleDigitIcon, checks.hasDigit),
            Triple(b.shopTxtRuleMatch, b.shopTxtRuleMatchIcon, checks.matches),
        ).forEach { (text, icon, met) ->
            val color = ContextCompat.getColor(requireContext(), if (met) R.color.pm_primary_pressed else R.color.pm_ink_muted)
            text.setTextColor(color)
            icon.setImageResource(if (met) R.drawable.ic_check else R.drawable.ic_remove)
            icon.setColorFilter(color)
        }
        b.shopBtnSavePassword.isEnabled = checks.all
    }

    private companion object {
        const val STATE_DONE = "done"
    }
}
