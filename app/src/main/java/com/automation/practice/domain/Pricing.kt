package com.automation.practice.domain

enum class Courier(val fee: Long) {
    REGULER(10_000), EXPRESS(20_000),

    /** Hanya DKI Jakarta (PRD §8.7). */
    SAME_DAY(35_000),
}

data class Voucher(val code: String, val title: String, val note: String, val expired: Boolean = false)

const val INSURANCE_FEE = 2_000L
const val SERVICE_FEE = 1_000L
const val COD_LIMIT = 1_000_000L

private const val HEMAT10_MAX = 50_000L
private const val ONGKIR_MAX = 20_000L
private const val MIN100K_OFF = 15_000L
private const val MIN100K_THRESHOLD = 100_000L

/** Daftar voucher di sheet; urutan sesuai desain. */
val VOUCHERS = listOf(
    Voucher("HEMAT10", "Diskon 10%", "Maks Rp50.000 · tanpa minimum"),
    Voucher("ONGKIR", "Potongan ongkir", "Maks Rp20.000"),
    Voucher("MIN100K", "Potongan Rp15.000", "Min. belanja Rp100.000"),
    Voucher("EXPIRED", "Diskon 20%", "Sudah kedaluwarsa", expired = true),
)

/** Pesan galat persis PRD §8.7, atau null bila [code] boleh dipasang pada [subtotal]. */
fun voucherError(code: String, subtotal: Long): String? {
    val c = code.trim().uppercase()
    return when {
        c == "EXPIRED" -> "Voucher sudah kedaluwarsa"
        c == "MIN100K" && subtotal < MIN100K_THRESHOLD -> "Minimal belanja Rp100.000"
        VOUCHERS.none { it.code == c } -> "Voucher tidak ditemukan"
        else -> null
    }
}

data class Quote(
    val subtotal: Long,
    val voucherCode: String?,
    val discount: Long,
    val shipping: Long,
    val insurance: Long,
    val service: Long,
) {
    /** Total = Subtotal − diskon voucher + (ongkir − potongan ongkir) + asuransi + biaya layanan. */
    val total get() = subtotal - discount + shipping + insurance + service
}

/** Voucher yang valid dan memotong harga barang. `ONGKIR` bernilai 0 di sini karena kurir belum dipilih (Keranjang). */
fun itemDiscount(subtotal: Long, code: String?): Long {
    val c = code?.trim()?.uppercase()?.takeIf { voucherError(it, subtotal) == null }
    return when (c) {
        "HEMAT10" -> minOf(subtotal * 10 / 100, HEMAT10_MAX)
        "MIN100K" -> MIN100K_OFF
        else -> 0L
    }
}

/**
 * Hitung ringkasan harga. Potongan `ONGKIR` dicatat sebagai diskon voucher (nilainya sama dengan
 * mengurangi ongkir), jadi `Same Day` + `ONGKIR` tetap membayar Rp15.000.
 * Voucher yang tidak valid untuk [subtotal] diabaikan.
 */
fun quote(subtotal: Long, voucherCode: String?, courier: Courier, insured: Boolean): Quote {
    val code = voucherCode?.trim()?.uppercase()?.takeIf { voucherError(it, subtotal) == null }
    val discount = if (code == "ONGKIR") minOf(courier.fee, ONGKIR_MAX) else itemDiscount(subtotal, code)
    return Quote(subtotal, code, discount, courier.fee, if (insured) INSURANCE_FEE else 0, SERVICE_FEE)
}
