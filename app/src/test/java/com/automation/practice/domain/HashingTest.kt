package com.automation.practice.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingTest {
    @Test fun `hash tidak memuat plaintext dan bisa diverifikasi`() {
        val h = Hashing.hash("Practice123!")
        assertFalse("Practice123!" in h)
        assertTrue(Hashing.verify("Practice123!", h))
        assertFalse(Hashing.verify("practice123!", h))
    }

    @Test fun `salt berbeda tiap hash`() = assertNotEquals(Hashing.hash("123456"), Hashing.hash("123456"))

    @Test fun `format rusak ditolak tanpa crash`() {
        assertFalse(Hashing.verify("123456", ""))
        assertFalse(Hashing.verify("123456", "pbkdf2\$x\$zz\$zz"))
        assertFalse(Hashing.verify("123456", "123456"))
    }
}
