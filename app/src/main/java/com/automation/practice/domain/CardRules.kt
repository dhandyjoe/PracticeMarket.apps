package com.automation.practice.domain

/** Nomor kartu tanpa spasi dengan nilai tetap bagi 3DS/hasil (PRD tidak menetapkan pemicu, lihat docs/locators/checkout.md). */
const val DECLINED_CARD = "4000000000000002"
const val PENDING_CARD = "4000000000000119"

private const val MIN_CARD_DIGITS = 13
private const val MAX_CARD_DIGITS = 16

fun String.digitsOnly() = filter(Char::isDigit)

/** `4111111111111111` → `4111 1111 1111 1111`. */
fun formatCardNumber(raw: String) = raw.digitsOnly().take(MAX_CARD_DIGITS).chunked(4).joinToString(" ")

fun isLuhnValid(number: String): Boolean {
    val d = number.digitsOnly()
    if (d.length < MIN_CARD_DIGITS) return false
    var sum = 0
    d.reversed().forEachIndexed { i, ch ->
        var n = ch - '0'
        if (i % 2 == 1) { n *= 2; if (n > 9) n -= 9 }
        sum += n
    }
    return sum % 10 == 0
}

/** `MM/YY` → pesan galat, atau null bila masih berlaku. [nowYear] dua digit terakhir tahun. */
fun expiryError(mmYy: String, nowYear: Int, nowMonth: Int): String? {
    val d = mmYy.digitsOnly()
    if (d.length != 4) return "Format MM/YY tidak valid"
    val month = d.substring(0, 2).toInt()
    val year = d.substring(2).toInt()
    if (month !in 1..12) return "Format MM/YY tidak valid"
    return if (year < nowYear || (year == nowYear && month < nowMonth)) "Kartu sudah kedaluwarsa" else null
}

fun isCvvValid(cvv: String) = cvv.length == 3 && cvv.all(Char::isDigit)

enum class PayOutcome { SUCCESS, FAILED, PENDING }

fun cardOutcome(number: String) = when (number.digitsOnly()) {
    DECLINED_CARD -> PayOutcome.FAILED
    PENDING_CARD -> PayOutcome.PENDING
    else -> PayOutcome.SUCCESS
}
