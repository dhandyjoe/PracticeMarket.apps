package com.automation.practice.domain

enum class PayMethod { CARD, VA, WALLET, COD }

/** Urutan enum = urutan timeline Tracking (PRD FR-ORD-02); [CANCELLED] di luar alur. */
enum class OrderStatus {
    WAITING, PAID, PACKED, SHIPPED, DONE, CANCELLED;

    /** Status berikutnya di timeline; null bila sudah akhir atau dibatalkan. */
    fun next(): OrderStatus? = if (this == CANCELLED || this == DONE) null else entries[ordinal + 1]

    /** Pesanan boleh dibatalkan hanya bila status ≤ Dikemas (FR-ORD-03). */
    val cancellable get() = this == WAITING || this == PAID || this == PACKED
}

data class Order(
    val id: String,
    val lines: List<CartLine>,
    val total: Long,
    val method: PayMethod,
    val methodLabel: String,
    val status: OrderStatus,
    /** Waktu status terakhir berubah (epoch ms); dibaca simulator untuk interval. */
    val statusAt: Long = System.currentTimeMillis(),
    /** Waktu masuk tiap status (epoch ms), untuk timeline Tracking. */
    val stamps: Map<OrderStatus, Long> = emptyMap(),
    val cancelReason: String = "",
    /** Batas bayar VA (epoch ms); 0 = tidak ada. */
    val vaDeadline: Long = 0,
)

/** Order ID deterministik `PM-20260115-0001`, `-0002`, … (PRD FR-PAYM-13). */
fun orderId(sequence: Int) = "PM-20260115-%04d".format(sequence)

/** Cashback PracticePay: 5% total, maks Rp10.000, masuk saat pesanan Selesai (PRD §8.7). */
fun cashback(total: Long) = minOf(total * 5 / 100, 10_000L)
