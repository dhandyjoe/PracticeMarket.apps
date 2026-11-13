package com.automation.practice.feature.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.data.repo.Behavior
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Courier
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.PayOutcome
import com.automation.practice.domain.Product
import com.automation.practice.domain.ProductFilter
import com.automation.practice.domain.Quote
import com.automation.practice.domain.cardOutcome
import com.automation.practice.domain.digitsOnly
import com.automation.practice.domain.quote
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Pilihan di layar Checkout; hidup di ViewModel supaya bertahan saat rotasi (FR-PAYM-04). */
data class CheckoutDraft(
    val courier: Courier = Courier.EXPRESS,
    val insured: Boolean = true,
    val note: String = "",
    /** Diisi oleh "Beli Sekarang": checkout hanya produk ini (FR-SHOP-11). */
    val buyNow: CartLine? = null,
)

/** Hasil akhir pembayaran untuk layar SHOP-15. [reason] kosong = pakai teks bawaan varian. */
data class PaymentResult(
    val outcome: PayOutcome,
    val order: Order?,
    val total: Long,
    val methodLabel: String,
    val reason: String = "",
)

/** Langkah berikutnya setelah "Bayar"; MainActivity yang membuka layarnya. */
sealed interface PayStep {
    data class ThreeDs(val total: Long, val cardLast4: String) : PayStep
    data object Va : PayStep
    data object Result : PayStep
}

/** Pesanan Virtual Account yang menunggu transfer. [deadline] dalam epoch ms. */
data class PendingVa(val orderId: String, val bank: String, val number: String, val total: Long, val deadline: Long)

/** Status layar Belanja yang dipakai bersama (hasil, filter, galeri, checkout). Scope: MainActivity. */
class ShopViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.app.container
    val catalog = container.catalog
    val session = container.shop

    /** Meniru bug `glitch_user` (PRD §8.8). */
    val glitch get() = container.auth.currentUser?.behavior == Behavior.GLITCH

    private val _filter = MutableStateFlow(ProductFilter())
    val filter: StateFlow<ProductFilter> = _filter.asStateFlow()

    /** Gambar galeri yang sedang tampil; dibagi antara Detail Produk dan Image Viewer. */
    val galleryIndex = MutableStateFlow(0)

    private val _checkout = MutableStateFlow(CheckoutDraft())
    val checkout: StateFlow<CheckoutDraft> = _checkout.asStateFlow()

    var result: PaymentResult? = null

    /** Pilihan di layar Metode Pembayaran; bertahan saat rotasi. */
    var chosenMethod = PayMethod.CARD
    var chosenBank = "BCA"

    val processing = MutableStateFlow(false)
    private val _steps = Channel<PayStep>(Channel.BUFFERED)
    val steps = _steps.receiveAsFlow()

    var va: PendingVa? = null
        private set
    private val vaByOrder = mutableMapOf<String, PendingVa>()

    /** Buka kembali instruksi VA pesanan yang masih menunggu (dari Pesanan Saya). */
    fun showVa(orderId: String): Boolean = vaByOrder[orderId]?.also { va = it } != null
    private var pendingCard = ""
    private var pendingCardLabel = ""

    fun results() = catalog.query(_filter.value, glitch)
    fun visual(p: Product) = p.visual(glitch)

    fun search(q: String) {
        session.addHistory(q)
        _filter.value = ProductFilter(query = q.trim())
    }

    fun browse(categorySlug: String) {
        _filter.value = ProductFilter(categories = setOf(categorySlug))
    }

    fun applyFilter(f: ProductFilter) { _filter.value = f }
    fun resetFilter() { _filter.value = ProductFilter(query = _filter.value.query) }

    // --- keranjang & checkout ---

    /** Total satu baris. Bug `glitch_user`: qty ≥ 3 dihitung satu kurang (PRD §8.8 no. 2). */
    fun lineTotal(line: CartLine): Long {
        val price = catalog.product(line.productId)?.price ?: 0
        val qty = if (glitch && line.qty >= GLITCH_QTY) line.qty - 1 else line.qty
        return price * qty
    }

    fun subtotal(lines: List<CartLine>) = lines.sumOf(::lineTotal)

    fun selectedLines() = session.cart.value.filter { it.selected && !it.saved }

    /** Baris yang akan dibayar: satu produk "Beli Sekarang", atau yang dicentang di keranjang. */
    fun checkoutLines(): List<CartLine> = _checkout.value.buyNow?.let { listOf(it) } ?: selectedLines()

    fun startCheckout(buyNow: CartLine? = null) {
        _checkout.value = CheckoutDraft(buyNow = buyNow)
        chosenMethod = PayMethod.CARD
        chosenBank = "BCA"
    }
    fun updateCheckout(f: (CheckoutDraft) -> CheckoutDraft) = _checkout.update(f)

    fun quote(): Quote {
        val d = _checkout.value
        return quote(subtotal(checkoutLines()), session.voucher.value, d.courier, d.insured)
    }

    // --- pesanan ---

    /**
     * Simpan pesanan: kurangi stok, kurangi saldo bila PracticePay, buang baris yang dibeli dari keranjang,
     * dan lepas voucher yang sudah terpakai.
     */
    fun placeOrder(method: PayMethod, label: String, status: OrderStatus): Order {
        val lines = checkoutLines()
        val total = quote().total
        val order = session.newOrder { id -> Order(id, lines, total, method, label, status) }
        lines.forEach { catalog.adjustStock(it, -it.qty) }
        if (method == PayMethod.WALLET) session.debit(total)
        if (_checkout.value.buyNow == null) session.removePurchased(lines)
        session.setVoucher(null)
        return order
    }

    /**
     * Proses pembayaran: overlay "Memproses pembayaran…" ±2 dtk (FR-PAYM-12), lalu lanjut sesuai metode.
     * Lama overlay = 2 dtk × pengali latency Developer Options (0× = langsung).
     */
    fun pay(method: PayMethod, label: String, cardNumber: String = "", bank: String = "") {
        if (processing.value) return
        processing.value = true
        viewModelScope.launch {
            delay(PROCESSING_MS * container.settings.prefs.value.latency)
            val total = quote().total
            when (method) {
                PayMethod.CARD -> {
                    pendingCard = cardNumber
                    pendingCardLabel = label
                    _steps.send(PayStep.ThreeDs(total, cardNumber.digitsOnly().takeLast(LAST_DIGITS)))
                }
                PayMethod.VA -> {
                    val order = placeOrder(method, label, OrderStatus.WAITING)
                    val deadline = container.now() + container.settings.prefs.value.vaWindowMs
                    session.setVaDeadline(order.id, deadline)
                    va = PendingVa(order.id, bank, vaNumber(bank), total, deadline).also { vaByOrder[order.id] = it }
                    _steps.send(PayStep.Va)
                }
                PayMethod.WALLET, PayMethod.COD -> finish(PayOutcome.SUCCESS, method, label)
            }
            processing.value = false
        }
    }

    /** Hasil 3D Secure: [ok] false = dibatalkan, kembali ke Metode Pembayaran. */
    fun onThreeDsDone(ok: Boolean) {
        if (!ok) return
        viewModelScope.launch { finish(cardOutcome(pendingCard), PayMethod.CARD, pendingCardLabel) }
    }

    fun vaPaid() {
        val pending = va ?: return
        markPaid(pending.orderId)
        val order = session.order(pending.orderId)
        result = PaymentResult(PayOutcome.SUCCESS, order, pending.total, order?.methodLabel.orEmpty())
        order?.let(::notifyCreated)
        viewModelScope.launch { _steps.send(PayStep.Result) }
    }

    fun vaExpired() {
        val pending = va ?: return
        cancelOrder(pending.orderId)
        result = PaymentResult(
            PayOutcome.FAILED, null, pending.total, getApplication<Application>().getString(R.string.pay_method_va_label_fmt, pending.bank),
            reason = getApplication<Application>().getString(R.string.va_expired),
        )
        viewModelScope.launch { _steps.send(PayStep.Result) }
    }

    private suspend fun finish(outcome: PayOutcome, method: PayMethod, label: String) {
        val total = quote().total
        val status = when (outcome) {
            PayOutcome.SUCCESS -> OrderStatus.PAID
            PayOutcome.PENDING -> OrderStatus.WAITING
            PayOutcome.FAILED -> null
        }
        val order = status?.let { placeOrder(method, label, it) }
        result = PaymentResult(outcome, order, total, label)
        if (order != null && status == OrderStatus.PAID) notifyCreated(order)
        _steps.send(PayStep.Result)
    }

    /** Nomor VA contoh per bank; awalan berbeda supaya tiap bank terlihat berbeda di test. */
    private fun vaNumber(bank: String) = when (bank) {
        "BNI" -> "8848 0000 1234 5678"
        "Mandiri" -> "8908 0000 1234 5678"
        else -> "8077 0000 1234 5678"
    }

    private fun notifyCreated(order: Order) = OrderNotifier.post(getApplication(), order)

    /** Pesanan dibatalkan (mis. VA kedaluwarsa, atau oleh pengguna): stok kembali. */
    fun cancelOrder(id: String, reason: String = "") = container.cancelOrder(id, reason)

    fun markPaid(id: String) = session.updateOrder(id, OrderStatus.PAID)

    private companion object {
        const val GLITCH_QTY = 3
        const val PROCESSING_MS = 2_000L
        const val LAST_DIGITS = 4
    }
}
