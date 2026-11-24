package com.automation.practice.domain

/** Aturan Auth (PRD FR-AUTH-12…16). Kotlin murni, tanpa Android. */
object AuthRules {
    const val MAX_FAILED_LOGINS = 3
    const val LOCK_SECONDS = 30

    enum class Strength { NONE, WEAK, MEDIUM, STRONG }

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
}
