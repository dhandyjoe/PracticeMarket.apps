package com.automation.practice.domain

import java.util.Calendar
import java.util.TimeZone

data class Profile(
    val name: String,
    val email: String,
    /** `YYYY-MM-DD` atau kosong. */
    val birth: String = "",
    val gender: String = "",
    val photoUri: String? = null,
)

data class Address(
    val id: Int,
    val label: String,
    val recipient: String,
    val phone: String,
    val province: String,
    val city: String,
    val street: String,
    val postal: String,
    val primary: Boolean,
)

const val POSTAL_LENGTH = 5

/** Provinsi → kota untuk dropdown bertingkat (FR-ACC-03). */
val REGIONS = linkedMapOf(
    "DKI Jakarta" to listOf("Jakarta Pusat", "Jakarta Selatan", "Jakarta Barat", "Jakarta Timur", "Jakarta Utara"),
    "Jawa Barat" to listOf("Bandung", "Bekasi", "Bogor", "Depok", "Cimahi"),
    "Jawa Tengah" to listOf("Semarang", "Surakarta", "Magelang"),
    "Jawa Timur" to listOf("Surabaya", "Malang", "Kediri"),
    "Bali" to listOf("Denpasar", "Badung", "Gianyar"),
)

/** Lokasi mock PRD §11.3: -6.2088, 106.8228 → DKI Jakarta, Jakarta Pusat. */
const val MOCK_PROVINCE = "DKI Jakarta"
const val MOCK_CITY = "Jakarta Pusat"

/** Seed alamat `standard_user` (PRD §11.3). */
val SEED_ADDRESSES = listOf(
    Address(1, "Rumah", "Standard User", "0812-3456-7890", "Jawa Barat", "Bandung", "Jl. Merdeka No. 10", "40111", true),
    Address(2, "Kantor", "Standard User", "0812-3456-7890", "DKI Jakarta", "Jakarta Pusat", "Jl. Sudirman No. 1", "10220", false),
)

/** Kode pos valid = tepat 5 digit. */
fun isPostalValid(s: String) = s.length == POSTAL_LENGTH && s.all(Char::isDigit)

private val JAKARTA = TimeZone.getTimeZone("Asia/Jakarta")
private val MONTHS = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

/** Epoch ms untuk tanggal-jam WIB; bulan 1–12. */
fun jakartaTime(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, sec: Int = 0): Long =
    Calendar.getInstance(JAKARTA).apply { clear(); set(y, m - 1, d, h, min, sec) }.timeInMillis

private fun cal(ms: Long) = Calendar.getInstance(JAKARTA).apply { timeInMillis = ms }

/** `15 Januari 2026`. */
fun formatDate(ms: Long) = cal(ms).let { "${it[Calendar.DAY_OF_MONTH]} ${MONTHS[it[Calendar.MONTH]]} ${it[Calendar.YEAR]}" }

private val SHORT_MONTHS = MONTHS.map { it.take(3) }

/** `15 Jan 2026`. */
fun formatDateShort(ms: Long) = cal(ms).let { "${it[Calendar.DAY_OF_MONTH]} ${SHORT_MONTHS[it[Calendar.MONTH]]} ${it[Calendar.YEAR]}" }

/** `09:12`. */
fun formatHm(ms: Long) = cal(ms).let { "%02d:%02d".format(it[Calendar.HOUR_OF_DAY], it[Calendar.MINUTE]) }

/** `09:12:07`. */
fun formatHms(ms: Long) = formatHm(ms) + ":%02d".format(cal(ms)[Calendar.SECOND])

/** Tanggal dari `YYYY-MM-DD` ke epoch ms UTC tengah malam (format yang dipakai MaterialDatePicker). */
fun isoToUtcMillis(iso: String): Long? = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(iso)?.destructured?.let { (y, m, d) ->
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y.toInt(), m.toInt() - 1, d.toInt()) }.timeInMillis
}

fun utcMillisToIso(ms: Long): String = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
    .let { "%04d-%02d-%02d".format(it[Calendar.YEAR], it[Calendar.MONTH] + 1, it[Calendar.DAY_OF_MONTH]) }
