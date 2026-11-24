package com.automation.practice.domain

val VA_BANKS = listOf("BCA", "BNI", "Mandiri")

private val VA_PREFIX = mapOf("BCA" to "8077", "BNI" to "8808", "Mandiri" to "8900")

/** Tanggal Order ID mode deterministik; BCA pada tanggal ini tetap `8077 0000 1234 5678` (contract §4.3). */
private const val DETERMINISTIC_DATE = "20260115"

/** Bank dari `methodLabel` ("Virtual Account BNI", atau label lama "BNI Virtual Account"); tanpa nama bank = BCA. */
fun vaBankOf(methodLabel: String) = VA_BANKS.firstOrNull { it in methodLabel } ?: "BCA"

/** Nomor VA dari Order ID dan bank, sama dengan web (`vaNumber`): awalan bank + digit Order ID, dikelompokkan 4 digit. */
fun vaNumber(orderId: String, bank: String): String {
    val digits = if (bank == "BCA" && DETERMINISTIC_DATE in orderId) "8077000012345678" else VA_PREFIX.getValue(bank) + orderId.filter(Char::isDigit)
    return digits.chunked(4).joinToString(" ")
}
