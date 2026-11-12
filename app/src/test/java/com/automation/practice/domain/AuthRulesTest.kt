package com.automation.practice.domain

import com.automation.practice.domain.AuthRules.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRulesTest {
    @Test fun emailFormat() {
        assertTrue(AuthRules.isValidEmail("rina@contoh.id"))
        assertFalse(AuthRules.isValidEmail("rina@contoh"))
        assertFalse(AuthRules.isValidEmail("rina contoh@x.id"))
    }

    @Test fun strength() {
        assertEquals(Strength.NONE, AuthRules.passwordStrength(""))
        assertEquals(Strength.WEAK, AuthRules.passwordStrength("abc123"))
        assertEquals(Strength.WEAK, AuthRules.passwordStrength("abcdefgh"))
        assertEquals(Strength.MEDIUM, AuthRules.passwordStrength("rahasia1"))
        assertEquals(Strength.STRONG, AuthRules.passwordStrength("Rahasia123!xy"))
    }

    @Test fun newPasswordChecks() {
        assertTrue(AuthRules.passwordChecks("Baru2026", "Baru2026").all)
        val c = AuthRules.passwordChecks("Baru2026", "Baru202")
        assertTrue(c.minLength && c.hasLetter && c.hasDigit)
        assertFalse(c.matches)
        assertFalse(AuthRules.passwordChecks("", "").matches)
    }

    @Test fun maskEmail() {
        assertEquals("r***@contoh.id", AuthRules.maskEmail("rina@contoh.id"))
    }
}
