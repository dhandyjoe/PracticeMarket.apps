package com.automation.practice.feature.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.data.repo.Outcome
import com.automation.practice.domain.Address
import com.automation.practice.domain.CartLine
import com.automation.practice.domain.Courier
import com.automation.practice.domain.Order
import com.automation.practice.domain.OrderStatus
import com.automation.practice.domain.PayMethod
import com.automation.practice.domain.PayOutcome
import com.automation.practice.domain.Product
import com.automation.practice.domain.ProductFilter
import com.automation.practice.domain.Quote
import com.automation.practice.domain.VA_EXPIRED_REASON
import com.automation.practice.domain.cardOutcome
import com.automation.practice.domain.courierAvailable
import com.automation.practice.domain.digitsOnly
import com.automation.practice.domain.quote
import com.automation.practice.domain.vaBankOf
import com.automation.practice.domain.vaNumber
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
    /** Alamat pilihan pengguna; null = alamat utama. */
    val addressId: String? = null,
    /** Jam kirim Same Day `HH:mm`; kosong = belum dipilih. */
    val deliveryTime: String = "",
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

    /**
     * Buka instruksi VA pesanan yang masih menunggu. Dibangun dari dokumen pesanan (bank dari `methodLabel`,
     * nomor dari Order ID, batas dari `vaDeadline`), jadi pesanan buatan web atau sebelum app dimatikan juga bisa dibuka.
     */
    fun showVa(orderId: String): Boolean {
        val o = session.order(orderId)?.takeIf { it.status == OrderStatus.WAITING && it.method == PayMethod.VA && it.vaDeadline > 0 } ?: return false
        va = pendingVa(o)
        return true
    }

    private fun pendingVa(o: Order): PendingVa = vaBankOf(o.methodLabel).let { PendingVa(o.id, it, vaNumber(o.id, it), o.total, o.vaDeadline) }
    private var pendingCard = ""
    private var pendingCardLabel = ""

    fun results() = catalog.query(_filter.value)
    fun visual(p: Product) = p.visual()

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

    /** Total satu baris. */
    fun lineTotal(line: CartLine): Long = (catalog.product(line.productId)?.price ?: 0) * line.qty

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

    /** Alamat checkout: pilihan pengguna, selain itu alamat utama (atau yang pertama); null bila akun tanpa alamat. */
    fun checkoutAddress(): Address? {
        val list = session.addresses.value
        return list.firstOrNull { it.id == _checkout.value.addressId } ?: list.firstOrNull { it.primary } ?: list.firstOrNull()
    }

    /** Kurir yang berlaku: Same Day jatuh ke Express bila alamat bukan DKI Jakarta (sama dengan web). */
    fun checkoutCourier(): Courier {
        val c = _checkout.value.courier
        return if (courierAvailable(c, checkoutAddress()?.province.orEmpty())) c else Courier.EXPRESS
    }

    fun quote(): Quote = quote(subtotal(checkoutLines()), session.voucher.value, checkoutCourier(), _checkout.value.insured)

    // --- pesanan ---

    /**
     * Simpan pesanan lewat transaksi Firestore (stok, saldo bila PracticePay, `orderSeq`; contract §8.3). Bila berhasil,
     * buang baris yang dibeli dari keranjang dan lepas voucher; bila gagal, keranjang tetap utuh.
     */
    private suspend fun placeOrder(method: PayMethod, label: String, status: OrderStatus, vaDeadline: Long = 0): Outcome<Order> {
        val lines = checkoutLines()
        val r = session.placeOrder(Order("", lines, quote().total, method, label, status, vaDeadline = vaDeadline))
        if (r is Outcome.Done) {
            if (_checkout.value.buyNow == null) session.removePurchased(lines)
            session.setVoucher(null)
        }
        return r
    }

    /** Teks kegagalan transaksi (contract §6); null bila berhasil. */
    fun failureText(r: Outcome<*>): String? {
        val app = getApplication<Application>()
        return when (r) {
            is Outcome.Done -> null
            is Outcome.NoStock -> app.getString(R.string.stock_short_fmt, catalog.product(r.productId)?.name ?: r.productId)
            Outcome.NoBalance -> app.getString(R.string.pay_wallet_insufficient)
            Outcome.Offline -> app.getString(R.string.error_network)
            Outcome.Failed -> app.getString(R.string.load_failed)
        }
    }

    /**
     * Proses pembayaran: overlay "Memproses pembayaran…" ±2 dtk (FR-PAYM-12), lalu lanjut sesuai metode.
     * Lama overlay = 2 dtk × pengali latency Developer Options (0× = langsung).
     */
    fun pay(method: PayMethod, label: String, cardNumber: String = "") {
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
                    // Batas VA memakai jam asli: client lain menghitung countdown dari nilai yang sama (contract §5).
                    val deadline = container.realNow() + container.settings.prefs.value.vaWindowMs
                    when (val r = placeOrder(method, label, OrderStatus.WAITING, deadline)) {
                        is Outcome.Done -> {
                            va = pendingVa(r.value)
                            _steps.send(PayStep.Va)
                        }
                        else -> {
                            result = PaymentResult(PayOutcome.FAILED, null, total, label, failureText(r).orEmpty())
                            _steps.send(PayStep.Result)
                        }
                    }
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

    /** "Saya sudah bayar": WAITING → PAID lewat transaksi kondisional. [onError] menerima teks bila gagal (offline). */
    fun vaPaid(onError: (String) -> Unit) {
        val pending = va ?: return
        viewModelScope.launch {
            session.advance(pending.orderId)
            val order = session.order(pending.orderId)
            if (order?.status == OrderStatus.WAITING) {
                onError(getApplication<Application>().getString(R.string.error_network))
                return@launch
            }
            result = PaymentResult(PayOutcome.SUCCESS, order, pending.total, order?.methodLabel.orEmpty())
            order?.let(::notifyCreated)
            _steps.send(PayStep.Result)
        }
    }

    fun vaExpired() {
        val pending = va ?: return
        viewModelScope.launch {
            session.cancelOrder(pending.orderId, VA_EXPIRED_REASON)
            result = PaymentResult(
                PayOutcome.FAILED, null, pending.total, getApplication<Application>().getString(R.string.pay_method_va_label_fmt, pending.bank),
                reason = getApplication<Application>().getString(R.string.va_expired),
            )
            _steps.send(PayStep.Result)
        }
    }

    private suspend fun finish(outcome: PayOutcome, method: PayMethod, label: String) {
        val total = quote().total
        val status = when (outcome) {
            PayOutcome.SUCCESS -> OrderStatus.PAID
            PayOutcome.PENDING -> OrderStatus.WAITING
            PayOutcome.FAILED -> null
        }
        val r = status?.let { placeOrder(method, label, it) }
        val order = (r as? Outcome.Done)?.value
        result = if (r == null || order != null) PaymentResult(outcome, order, total, label)
        else PaymentResult(PayOutcome.FAILED, null, total, label, failureText(r).orEmpty())
        if (order != null && status == OrderStatus.PAID) notifyCreated(order)
        _steps.send(PayStep.Result)
    }

    private fun notifyCreated(order: Order) = OrderNotifier.post(getApplication(), order)

    /** Pesanan dibatalkan oleh pengguna: stok kembali (FR-ORD-05). */
    suspend fun cancelOrder(id: String, reason: String = "") = session.cancelOrder(id, reason)

    private companion object {
        const val PROCESSING_MS = 2_000L
        const val LAST_DIGITS = 4
    }
}
