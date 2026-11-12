package com.automation.practice.feature.auth

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.automation.practice.R
import com.automation.practice.databinding.FragmentRegisterBinding
import com.automation.practice.domain.AuthRules
import com.automation.practice.domain.AuthRules.Strength

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private val vm: AuthViewModel by activityViewModels()
    private val nav get() = requireActivity() as AuthNavigator

    // Pesan error khusus dari submit ("Email sudah terdaftar"); hilang saat email diedit.
    @StringRes private var emailServerError: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentRegisterBinding.bind(view)

        b.shopBtnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.shopBtnLoginLink.setOnClickListener { nav.showLogin() }
        bindTermsLink(b)

        b.shopInputRegEmail.doAfterTextChanged { emailServerError = null }
        listOf(b.shopInputRegName, b.shopInputRegEmail, b.shopInputRegPassword, b.shopInputRegConfirm)
            .forEach { it.doAfterTextChanged { render(b) } }
        b.shopChkTerms.setOnCheckedChangeListener { _, _ -> render(b) }
        render(b)

        b.shopBtnRegister.setOnClickListener {
            val email = b.shopInputRegEmail.text.toString()
            val error = vm.register(b.shopInputRegName.text.toString(), email, b.shopInputRegPassword.text.toString())
            if (error != null) {
                emailServerError = error
                render(b)
            } else {
                nav.showLogin(prefill = email.trim(), flashRes = R.string.register_success)
            }
        }
    }

    private fun render(b: FragmentRegisterBinding) {
        val name = b.shopInputRegName.text.toString()
        val email = b.shopInputRegEmail.text.toString()
        val password = b.shopInputRegPassword.text.toString()
        val confirm = b.shopInputRegConfirm.text.toString()

        val emailFormatBad = email.isNotEmpty() && !AuthRules.isValidEmail(email)
        val emailError = if (emailFormatBad) R.string.error_email_invalid else emailServerError
        b.shopInputRegEmail.isSelected = emailError != null
        b.shopTxtRegEmailError.isVisible = emailError != null
        emailError?.let { b.shopTxtRegEmailError.setText(it) }

        val strength = AuthRules.passwordStrength(password)
        val (label, color) = when (strength) {
            Strength.NONE -> 0 to R.color.pm_surface_disabled
            Strength.WEAK -> R.string.strength_weak to R.color.pm_error
            Strength.MEDIUM -> R.string.strength_medium to R.color.pm_warning
            Strength.STRONG -> R.string.strength_strong to R.color.pm_primary
        }
        val tint = ContextCompat.getColor(requireContext(), color)
        listOf(b.strengthBar1, b.strengthBar2, b.strengthBar3).forEachIndexed { i, bar ->
            val filled = i < strength.ordinal
            bar.backgroundTintList = ColorStateList.valueOf(
                if (filled) tint else ContextCompat.getColor(requireContext(), R.color.pm_surface_disabled),
            )
        }
        b.shopTxtPasswordStrength.text = if (label == 0) "" else getString(label)
        b.shopTxtPasswordStrength.setTextColor(tint)

        val confirmBad = confirm.isNotEmpty() && confirm != password
        b.shopInputRegConfirm.isSelected = confirmBad
        b.shopTxtRegConfirmError.isVisible = confirmBad

        b.shopBtnRegister.isEnabled = name.isNotBlank() && email.isNotEmpty() && emailError == null &&
            strength >= Strength.MEDIUM && confirm == password && b.shopChkTerms.isChecked
    }

    // FR-AUTH-12: link "Syarat & Ketentuan" membuka WebView.
    private fun bindTermsLink(b: FragmentRegisterBinding) {
        val link = getString(R.string.register_terms_link)
        val text = getString(R.string.register_terms_fmt, link)
        val start = text.indexOf(link)
        val span = object : ClickableSpan() {
            override fun onClick(widget: View) = nav.showTerms()
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.pm_primary)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }
        b.shopTxtTerms.text = SpannableString(text).apply { setSpan(span, start, start + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        b.shopTxtTerms.movementMethod = LinkMovementMethod.getInstance()
    }
}
