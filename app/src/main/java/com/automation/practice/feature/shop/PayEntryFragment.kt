package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automation.practice.R
import com.automation.practice.core.ui.compose.Pm
import com.automation.practice.core.ui.compose.PmAvatar
import com.automation.practice.core.ui.compose.PmChip
import com.automation.practice.core.ui.compose.PmOutlineButton
import com.automation.practice.core.ui.compose.PmPrimaryButton
import com.automation.practice.core.ui.compose.PmScreen
import com.automation.practice.core.ui.compose.PmText
import com.automation.practice.core.ui.compose.PmTopBar
import com.automation.practice.core.ui.compose.tag
import com.automation.practice.domain.MIN_TRANSFER
import com.automation.practice.domain.NOTE_MAX
import com.automation.practice.domain.amountError
import com.automation.practice.domain.parseAmount
import com.automation.practice.domain.toRupiah
import com.google.android.material.snackbar.Snackbar

/**
 * Dasar alur "nominal → PIN → berhasil" PracticePay (Transfer PAY-04 dan Bayar QR PAY-05), Compose + testTag.
 * PIN memakai [PinSheetFragment] yang sama dengan checkout; kunci 3× salah berlaku bersama (FR-PAY-06).
 */
abstract class PayEntryFragment : ShopFragment(0) {
    protected abstract val title: Int
    protected abstract val payeeName: String
    protected abstract val payeeSub: String

    /** Non-null = nominal tetap dan tidak bisa diubah (merchant TOKOBUKU). */
    protected abstract val fixedAmount: Long?
    protected abstract val initialAmount: Long
    protected abstract val isTransfer: Boolean
    @get:StringRes protected abstract val buttonRes: Int
    @get:StringRes protected abstract val successTitle: Int
    @get:StringRes protected abstract val successBodyRes: Int

    /** Kurangi saldo dan catat transaksi; false bila saldo tidak cukup. */
    protected abstract fun commit(amount: Long, note: String): Boolean

    private var done by mutableStateOf<Long?>(null)
        private set
    private var pending: Pair<Long, String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PmScreen {
                    val balance by vm.session.balance.collectAsState()
                    PayEntryScreen(
                        title = stringResource(title), name = payeeName, sub = payeeSub, balance = balance,
                        fixed = fixedAmount, initial = initialAmount, transfer = isTransfer,
                        buttonLabel = { stringResource(buttonRes, it.toRupiah()) },
                        done = done, successTitle = stringResource(successTitle),
                        successBody = done?.let { stringResource(successBodyRes, it.toRupiah(), payeeName) }.orEmpty(),
                        onBack = { nav.back() },
                        onSubmit = { amount, note ->
                            pending = amount to note
                            PinSheetFragment().show(requireActivity().supportFragmentManager, "pin")
                        },
                        // nav diambil dulu: selectTab melepas fragment ini, sesudahnya requireActivity() melempar.
                        onHistory = { val n = nav; n.selectTab(Tab.PAY); n.openHistory() },
                        onFinish = { nav.selectTab(Tab.PAY) },
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setelah sukses, Back kembali ke PracticePay, bukan ke form nominal yang sudah dipakai.
        val backToWallet = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, enabled = done != null) { nav.selectTab(Tab.PAY) }
        requireActivity().supportFragmentManager.setFragmentResultListener(PinSheetFragment.RESULT_KEY, viewLifecycleOwner) { _, r ->
            val (amount, note) = pending ?: return@setFragmentResultListener
            if (!r.getBoolean(PinSheetFragment.RESULT_OK)) return@setFragmentResultListener
            pending = null
            if (commit(amount, note)) { done = amount; backToWallet.isEnabled = true }
            else Snackbar.make(requireView(), "Saldo tidak cukup", Snackbar.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun PayEntryScreen(
    title: String, name: String, sub: String, balance: Long, fixed: Long?, initial: Long, transfer: Boolean,
    buttonLabel: @Composable (Long) -> String, done: Long?, successTitle: String, successBody: String,
    onBack: () -> Unit, onSubmit: (Long, String) -> Unit, onHistory: () -> Unit, onFinish: () -> Unit,
) {
    if (done != null) {
        Success(successTitle, successBody, onHistory, onFinish)
        return
    }
    var raw by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    var note by remember { mutableStateOf("") }
    val amount = fixed ?: parseAmount(raw)
    val err = if (transfer) amountError(amount, balance) else if (amount > balance) "Saldo tidak cukup" else null
    val valid = amount > 0 && err == null && (!transfer || amount >= MIN_TRANSFER)

    Column(Modifier.fillMaxSize().background(Pm.surface).imePadding()) {
        PmTopBar(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Pm.background).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PmAvatar(name.split(" ").take(2).joinToString("") { it.first().uppercase() }, 0, 48.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    PmText(name, 16, FontWeight.Bold, modifier = Modifier.tag("pay_txt_payee_name"))
                    PmText(sub, 13, color = Pm.muted)
                }
            }
            Column {
                PmText(stringResource(R.string.pay_amount_label), 13, FontWeight.Bold, Pm.inkSecondary)
                val shown = if (amount > 0) amount.toRupiah() else ""
                val hint = stringResource(R.string.pay_amount_hint)
                Box(
                    Modifier.padding(top = 6.dp).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.5.dp, if (err != null) Pm.error else Pm.primary), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = TextFieldValue(shown, TextRange(shown.length)),
                        onValueChange = { v -> raw = parseAmount(v.text).let { if (it == 0L) "" else it.toString() } },
                        enabled = fixed == null, singleLine = true,
                        textStyle = TextStyle(fontFamily = Pm.Grotesk, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Pm.ink),
                        cursorBrush = SolidColor(Pm.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().tag(if (transfer) "pay_input_amount" else "pay_input_qr_amount").semantics { contentDescription = hint },
                        decorationBox = { inner -> Box { if (shown.isEmpty()) PmText(hint, 30, FontWeight.Bold, Pm.muted, Pm.Grotesk); inner() } },
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (err != null) PmText(err, 12, FontWeight.Bold, Pm.error, modifier = Modifier.tag("pay_txt_amount_error"))
                    else PmText(if (transfer) stringResource(R.string.pay_min_hint) else "", 12, color = Pm.muted)
                    PmText(stringResource(R.string.pay_balance_fmt, balance.toRupiah()), 12, color = Pm.muted, modifier = Modifier.tag("pay_txt_balance"))
                }
            }
            if (transfer) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50_000L, 100_000L, 200_000L).forEach { n ->
                        PmChip(n.toRupiah(), amount == n, { raw = n.toString() }, tag = "pay_chip_${n / 1000}k")
                    }
                }
                Column {
                    PmText(stringResource(R.string.pay_note_label), 13, FontWeight.Bold, Pm.inkSecondary)
                    val noteHint = stringResource(R.string.pay_note_hint)
                    Box(
                        Modifier.padding(top = 6.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.5.dp, Pm.outlineStrong), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = note, onValueChange = { note = it.take(NOTE_MAX) }, singleLine = true,
                            textStyle = TextStyle(fontFamily = Pm.Inter, fontSize = 15.sp, color = Pm.ink),
                            cursorBrush = SolidColor(Pm.primary),
                            modifier = Modifier.fillMaxWidth().tag("pay_input_note").semantics { contentDescription = noteHint },
                            decorationBox = { inner -> Box { if (note.isEmpty()) PmText(noteHint, 15, color = Pm.muted); inner() } },
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().background(Pm.surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            PmPrimaryButton(
                if (transfer) stringResource(R.string.pay_next) else buttonLabel(amount), valid,
                { onSubmit(amount, note.trim()) }, tag = "pay_btn_next",
            )
        }
    }
}

@Composable
private fun Success(title: String, body: String, onHistory: () -> Unit, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Pm.surface).navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(Pm.container), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.ic_check), null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Pm.primary),
            )
        }
        PmText(title, 22, FontWeight.Bold, Pm.ink, Pm.Grotesk, Modifier.padding(top = 16.dp).tag("pay_txt_success"))
        PmText(body, 15, color = Pm.inkSecondary, modifier = Modifier.padding(top = 6.dp).tag("pay_txt_success_body"))
        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PmPrimaryButton(stringResource(R.string.pay_view_history), true, onHistory, tag = "pay_btn_history")
            PmOutlineButton(stringResource(R.string.pay_done), onFinish, tag = "pay_btn_done")
        }
    }
}
