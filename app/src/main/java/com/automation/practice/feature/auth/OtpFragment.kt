package com.automation.practice.feature.auth

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.automation.practice.R
import com.automation.practice.databinding.FragmentOtpBinding
import com.automation.practice.domain.AuthRules
import kotlinx.coroutines.launch

/** AUTH-04: fase 1 minta email, fase 2 isi OTP 6 kotak. Fase diturunkan dari `vm.resetEmail`. */
class OtpFragment : Fragment(R.layout.fragment_otp) {
    private val vm: AuthViewModel by activityViewModels()
    private val nav get() = requireActivity() as AuthNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentOtpBinding.bind(view)
        if (savedInstanceState == null) vm.resetFlow()

        b.shopBtnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        // Fase email
        b.shopBtnSendCode.setOnClickListener { sendCode(b) }
        b.shopInputForgotEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) sendCode(b).let { true } else false
        }
        b.shopInputForgotEmail.doAfterTextChanged {
            b.shopTxtForgotEmailError.isVisible = false
            b.shopInputForgotEmail.isSelected = false
        }

        // Fase OTP
        val boxes = listOf(b.shopInputOtp1, b.shopInputOtp2, b.shopInputOtp3, b.shopInputOtp4, b.shopInputOtp5, b.shopInputOtp6)
        boxes.forEachIndexed { i, box ->
            box.contentDescription = getString(R.string.otp_digit_fmt, i + 1)
            box.doAfterTextChanged { text ->
                box.isActivated = !text.isNullOrEmpty()
                clearOtpError(b, boxes)
                if (!text.isNullOrEmpty() && i < boxes.lastIndex) boxes[i + 1].requestFocus()
                b.shopBtnVerifyOtp.isEnabled = boxes.all { it.text.isNotEmpty() }
            }
            box.setOnKeyListener { _, keyCode, event ->
                val backOnEmpty = keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && box.text.isEmpty() && i > 0
                if (backOnEmpty) boxes[i - 1].apply { setText(""); requestFocus() }
                backOnEmpty
            }
        }
        b.shopBtnVerifyOtp.setOnClickListener {
            if (vm.verifyOtp(boxes.joinToString("") { it.text })) {
                nav.showNewPassword()
            } else {
                b.shopTxtOtpError.isVisible = true
                boxes.forEach { it.isSelected = true }
            }
        }
        b.shopBtnResendOtp.setOnClickListener { vm.resend() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.resetEmail.collect { email ->
                        b.phaseEmail.isVisible = email == null
                        b.phaseOtp.isVisible = email != null
                        if (email != null) {
                            b.shopTxtOtpSubtitle.text = getString(R.string.otp_subtitle_fmt, AuthRules.maskEmail(email))
                            boxes[0].requestFocus()
                        }
                    }
                }
                launch {
                    vm.resendLeft.collect { left ->
                        b.shopBtnResendOtp.isEnabled = left == 0
                        b.shopBtnResendOtp.text = if (left > 0) getString(R.string.otp_resend_fmt, left) else getString(R.string.otp_resend)
                    }
                }
            }
        }
    }

    private fun sendCode(b: FragmentOtpBinding) {
        val error = vm.sendCode(b.shopInputForgotEmail.text.toString())
        b.shopInputForgotEmail.isSelected = error != null
        b.shopTxtForgotEmailError.isVisible = error != null
        error?.let { b.shopTxtForgotEmailError.setText(it) }
    }

    private fun clearOtpError(b: FragmentOtpBinding, boxes: List<EditText>) {
        b.shopTxtOtpError.isVisible = false
        boxes.forEach { it.isSelected = false }
    }
}
