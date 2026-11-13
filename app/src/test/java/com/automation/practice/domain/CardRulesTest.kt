package com.automation.practice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardRulesTest {
    @Test fun `Luhn`() {
        assertTrue(isLuhnValid("4111 1111 1111 1111"))
        assertFalse(isLuhnValid("4111 1111 1111 1112"))
        assertFalse(isLuhnValid("4111"))
        assertTrue(isLuhnValid(DECLINED_CARD))
        assertTrue(isLuhnValid(PENDING_CARD))
    }

    @Test fun `format per 4 digit dan batas 16 digit`() {
        assertEquals("4111 1111 1111 1111", formatCardNumber("4111111111111111"))
        assertEquals("4111 11", formatCardNumber("411111"))
        assertEquals("4111 1111 1111 1111", formatCardNumber("4111-1111-1111-1111-99"))
    }

    @Test fun `kedaluwarsa dan format`() {
        assertNull(expiryError("12/30", nowYear = 26, nowMonth = 11))
        assertNull(expiryError("11/26", nowYear = 26, nowMonth = 11))
        assertEquals("Kartu sudah kedaluwarsa", expiryError("10/26", nowYear = 26, nowMonth = 11))
        assertEquals("Kartu sudah kedaluwarsa", expiryError("12/25", nowYear = 26, nowMonth = 11))
        assertEquals("Format MM/YY tidak valid", expiryError("13/30", nowYear = 26, nowMonth = 11))
        assertEquals("Format MM/YY tidak valid", expiryError("1/3", nowYear = 26, nowMonth = 11))
    }

    @Test fun `CVV 3 digit`() {
        assertTrue(isCvvValid("123"))
        assertFalse(isCvvValid("12"))
        assertFalse(isCvvValid("12a"))
    }

    @Test fun `hasil menurut nomor kartu`() {
        assertEquals(PayOutcome.FAILED, cardOutcome("4000 0000 0000 0002"))
        assertEquals(PayOutcome.PENDING, cardOutcome("4000 0000 0000 0119"))
        assertEquals(PayOutcome.SUCCESS, cardOutcome("4111 1111 1111 1111"))
    }
}
