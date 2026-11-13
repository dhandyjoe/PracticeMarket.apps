package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.ItemPinKeyBinding
import com.automation.practice.databinding.SheetPinBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/** PIN PracticePay 6 digit (FR-PAYM-10, FR-PAY-06): PIN valid `123456`, 3× salah mengunci 30 dtk. */
class PinSheetFragment : BottomSheetDialogFragment() {
    private val vm: ShopViewModel by activityViewModels()
    private var binding: SheetPinBinding? = null
    private var pin = ""
    private val dots = mutableListOf<View>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val b = SheetPinBinding.inflate(inflater, container, false)
        binding = b
        b.shopSheetPin.applySystemInsets(bottom = true)
        repeat(PIN_LENGTH) {
            val dot = View(requireContext())
            val size = resources.getDimensionPixelSize(R.dimen.pm_space_4)
            dot.layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.pm_space_2_5)
                marginEnd = resources.getDimensionPixelSize(R.dimen.pm_space_2_5)
            }
            b.shopIndicatorPin.addView(dot)
            dots += dot
        }
        // Susunan tombol: 1-9, lalu kosong, 0, hapus.
        (listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", DELETE)).forEach { key ->
            val button = ItemPinKeyBinding.inflate(inflater, b.shopPadPin, false).root
            when (key) {
                "" -> button.visibility = View.INVISIBLE
                DELETE -> {
                    button.icon = requireContext().getDrawable(R.drawable.ic_backspace)
                    button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                    button.iconPadding = 0
                    button.iconTint = requireContext().getColorStateList(R.color.pm_ink)
                    button.contentDescription = getString(R.string.pin_delete)
                    button.setOnClickListener { pin = pin.dropLast(1); render() }
                }
                else -> {
                    button.text = key
                    button.setOnClickListener { press(key) }
                }
            }
            b.shopPadPin.addView(button, GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0
            })
        }
        render()
        return b.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        binding = null
        dots.clear()
        super.onDestroyView()
    }

    private fun press(digit: String) {
        if (pin.length >= PIN_LENGTH) return
        val session = vm.session
        val lockLeft = (session.pinLockedUntil - System.currentTimeMillis()).coerceAtLeast(0)
        if (lockLeft > 0) return showError(getString(R.string.pin_locked_fmt, ((lockLeft + 999) / 1000).toInt()))
        pin += digit
        render()
        if (pin.length < PIN_LENGTH) return
        if (pin == VALID_PIN) {
            session.pinFailures = 0
            setFragmentResult(RESULT_KEY, bundleOf(RESULT_OK to true))
            dismiss()
            return
        }
        session.pinFailures++
        pin = ""
        render()
        if (session.pinFailures >= MAX_FAILURES) {
            session.pinFailures = 0
            session.pinLockedUntil = System.currentTimeMillis() + LOCK_MS
            showError(getString(R.string.pin_locked_fmt, (LOCK_MS / 1000).toInt()))
        } else {
            showError(getString(R.string.pin_wrong_fmt, MAX_FAILURES - session.pinFailures))
        }
    }

    private fun showError(message: String) {
        binding?.shopTxtPinError?.apply { text = message; isInvisible = false }
    }

    private fun render() {
        val b = binding ?: return
        dots.forEachIndexed { i, dot -> dot.setBackgroundResource(if (i < pin.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot) }
        b.shopIndicatorPin.contentDescription = getString(R.string.pin_digits_cd_fmt, pin.length)
        if (pin.isNotEmpty()) b.shopTxtPinError.isInvisible = true
    }

    companion object {
        const val RESULT_KEY = "pin_result"
        const val RESULT_OK = "ok"
        private const val DELETE = "⌫"
        private const val PIN_LENGTH = 6
        private const val VALID_PIN = "123456"
        private const val MAX_FAILURES = 3
        private const val LOCK_MS = 30_000L
    }
}
