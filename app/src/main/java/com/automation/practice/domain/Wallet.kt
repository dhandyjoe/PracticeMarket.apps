package com.automation.practice.domain

const val MIN_TRANSFER = 10_000L
const val MIN_TOPUP = 10_000L
const val MAX_TOPUP = 10_000_000L
const val PAY_PIN = "123456"
const val NOTE_MAX = 50

/** Transaksi PracticePay. [amount] bertanda: positif = masuk. */
data class Tx(val id: String, val title: String, val amount: Long, val at: Long) {
    val incoming get() = amount > 0
}

data class Contact(val name: String, val phone: String, val recent: Boolean = false)

/** [fixedAmount] null = nominal bebas (PRD §11.4). */
data class Merchant(val id: String, val name: String, val fixedAmount: Long?)

val MERCHANTS = listOf(Merchant("KOPIPAGI", "Kopi Pagi", null), Merchant("TOKOBUKU", "Toko Buku", 45_000))

fun merchantOf(id: String?) = MERCHANTS.firstOrNull { it.id.equals(id, ignoreCase = true) }

/** Kontak uji bawaan (PRD §11.4); tetap dipakai bila izin kontak ditolak (FR-PAY-04). */
val SEED_CONTACTS = listOf(
    Contact("Andi Pratama", "0812-1111-0001"),
    Contact("Anisa Rahma", "0812-1111-0002"),
    Contact("Budi Santoso", "0812-3456-7890", recent = true),
    Contact("Bunga Lestari", "0812-1111-0004"),
    Contact("Citra Dewi", "0812-1111-0005", recent = true),
    Contact("Dewi Anggraini", "0812-1111-0006"),
    Contact("Dimas Saputra", "0812-1111-0007"),
    Contact("Eka Putri", "0812-1111-0008"),
    Contact("Fajar Nugroho", "0812-1111-0009"),
    Contact("Gilang Ramadhan", "0812-1111-0010"),
)

/** Cari nama (substring) atau nomor HP (digit saja), tanpa membedakan huruf besar. */
fun List<Contact>.search(q: String): List<Contact> {
    val t = q.trim()
    if (t.isEmpty()) return this
    val digits = t.filter(Char::isDigit)
    return filter { it.name.contains(t, ignoreCase = true) || (digits.isNotEmpty() && it.phone.filter(Char::isDigit).contains(digits)) }
}

/** Galat nominal transfer/bayar, atau null bila valid. Pesan persis PRD FR-PAY-05. */
fun amountError(amount: Long, balance: Long): String? = when {
    amount in 1 until MIN_TRANSFER -> "Minimal transfer Rp10.000"
    amount > balance -> "Saldo tidak cukup"
    else -> null
}

/** Galat nominal Top Up, atau null bila valid (nominal 0 = belum diisi). */
fun topUpError(amount: Long): String? = when {
    amount in 1 until MIN_TOPUP -> "Minimal top up Rp10.000"
    amount > MAX_TOPUP -> "Maksimal top up Rp10.000.000"
    else -> null
}

/** Digit saja dari input nominal; kosong = 0. Dibatasi 9 digit supaya tidak overflow. */
fun parseAmount(input: String): Long = input.filter(Char::isDigit).take(9).toLongOrNull() ?: 0L

const val HISTORY_PAGE = 20

// Judul transaksi ditulis ke Firestore dan dibaca web: selalu Bahasa Indonesia, tidak ikut locale (contract §8.3).
const val TX_TOPUP_TITLE = "Top Up Saldo"
fun transferTitle(name: String) = "Transfer ke $name"
fun qrTitle(merchant: String) = "Bayar QR · $merchant"

/** Saring riwayat: [kind] 0 = semua, 1 = masuk, 2 = keluar; [range] opsional (epoch ms, inklusif). Terbaru di atas. */
fun filterHistory(all: List<Tx>, kind: Int, range: LongRange? = null): List<Tx> =
    all.filter { (kind == 0 || (kind == 1) == it.incoming) && (range == null || it.at in range) }.sortedByDescending { it.at }
