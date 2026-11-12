package com.automation.practice.domain

/** Aturan Auth (PRD FR-AUTH-12…16). Kotlin murni, tanpa Android. */
object AuthRules {
    const val OTP_CODE = "123456"
    const val MAX_FAILED_LOGINS = 3
    const val LOCK_SECONDS = 30
    const val RESEND_SECONDS = 30

    enum class Strength { NONE, WEAK, MEDIUM, STRONG }

    data class PasswordChecks(val minLength: Boolean, val hasLetter: Boolean, val hasDigit: Boolean, val matches: Boolean) {
        val all get() = minLength && hasLetter && hasDigit && matches
    }

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValidEmail(email: String) = EMAIL.matches(email)

    fun passwordStrength(password: String): Strength {
        if (password.isEmpty()) return Strength.NONE
        val letter = password.any { it in 'A'..'Z' || it in 'a'..'z' }
        val digit = password.any { it in '0'..'9' }
        val special = password.any { !(it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9') }
        return when {
            password.length >= 12 && letter && digit && special -> Strength.STRONG
            password.length >= 8 && letter && digit -> Strength.MEDIUM
            else -> Strength.WEAK
        }
    }

    fun passwordChecks(password: String, confirm: String) = PasswordChecks(
        minLength = password.length >= 8,
        hasLetter = password.any { it in 'A'..'Z' || it in 'a'..'z' },
        hasDigit = password.any { it in '0'..'9' },
        matches = password.isNotEmpty() && password == confirm,
    )

    /** `rina@contoh.id` → `r***@contoh.id` */
    fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        return if (at <= 0) email else email.take(1) + "***" + email.substring(at)
    }
}
