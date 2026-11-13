package com.automation.practice.feature.shop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.automation.practice.BuildConfig
import com.automation.practice.R
import com.automation.practice.app
import com.automation.practice.applyAppearance
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.data.prefs.Prefs
import com.automation.practice.databinding.FragmentSettingsBinding
import com.automation.practice.databinding.ItemSettingGroupBinding
import com.automation.practice.databinding.ItemSettingRowBinding
import com.automation.practice.feature.auth.AuthActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** ACC-04 Pengaturan umum (FR-ACC-06) + Developer Options (PRD §10); semua tersimpan di [com.automation.practice.data.prefs.SettingsStore]. */
class SettingsFragment : ShopFragment(R.layout.fragment_settings) {
    private lateinit var b: FragmentSettingsBinding
    private val store get() = requireContext().app.container.settings
    private var devGroup: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentSettingsBinding.bind(view)
        b.shopTopbar.setup(R.string.acc_settings)
        b.accScrollSettings.applySystemInsets(bottom = true)
        val p = store.prefs.value

        group(R.string.set_group_general, dev = false) {
            seg(it, R.id.acc_seg_language, R.string.set_language, 0, listOf("Indonesia", "English"), if (p.language == "en") 1 else 0) { i ->
                change { c -> c.copy(language = if (i == 1) "en" else "id") }
            }
            seg(it, R.id.acc_seg_theme, R.string.set_theme, 0, listOf(getString(R.string.set_theme_system), getString(R.string.set_theme_light), getString(R.string.set_theme_dark)), p.theme) { i ->
                change { c -> c.copy(theme = i) }
            }
            switch(it, R.id.acc_sw_notifications, R.string.set_notifications, R.string.set_notifications_sub, p.orderNotifications) { on -> change { c -> c.copy(orderNotifications = on) } }
        }
        devGroup = group(R.string.set_group_dev, dev = true) {
            switch(it, R.id.dev_sw_deterministic, R.string.dev_deterministic, R.string.dev_deterministic_sub, p.deterministic) { on -> change { c -> c.copy(deterministic = on) } }
            seg(it, R.id.dev_seg_latency, R.string.dev_latency, 0, listOf("0×", "1×", "2×", "3×"), p.latency) { i -> change { c -> c.copy(latency = i) } }
            switch(it, R.id.dev_sw_animations, R.string.dev_animations, 0, p.animations) { on -> change { c -> c.copy(animations = on) } }
            seg(it, R.id.dev_seg_flaky, R.string.dev_flaky, R.string.dev_flaky_sub, listOf("0%", "30%", "50%"), FLAKY.indexOf(p.flaky)) { i -> change { c -> c.copy(flaky = FLAKY[i]) } }
            switch(it, R.id.dev_sw_hints, R.string.dev_hints, R.string.dev_hints_sub, p.locatorHints) { on -> change { c -> c.copy(locatorHints = on) } }
            switch(it, R.id.dev_sw_reset_on_launch, R.string.dev_reset_on_launch, R.string.dev_reset_on_launch_sub, p.resetOnLaunch) { on -> change { c -> c.copy(resetOnLaunch = on) } }
        }
        group(R.string.set_group_sim, dev = true) {
            seg(it, R.id.dev_seg_va, R.string.dev_va_window, 0, listOf("30 dtk", "2 mnt", "15 mnt"), VA_WINDOWS.indexOf(p.vaWindowMs)) { i -> change { c -> c.copy(vaWindowMs = VA_WINDOWS[i]) } }
            seg(it, R.id.dev_seg_interval, R.string.dev_interval, 0, listOf("3 dtk", "10 dtk", "30 dtk", getString(R.string.dev_interval_manual)), INTERVALS.indexOf(p.statusIntervalMs)) { i -> change { c -> c.copy(statusIntervalMs = INTERVALS[i]) } }
        }
        resetSection()
        about()

        if (requireArguments().getBoolean(ARG_DEV)) b.accScrollSettings.post { b.accScrollSettings.scrollTo(0, devGroup?.top ?: 0) }
    }

    private fun pad() = resources.getDimensionPixelSize(R.dimen.pm_space_1)

    private fun change(f: (Prefs) -> Prefs) {
        val before = store.prefs.value
        store.update(f)
        val after = store.prefs.value
        if (before.theme != after.theme || before.language != after.language) applyAppearance(after)
    }

    private fun group(@StringRes title: Int, dev: Boolean, fill: (LinearLayout) -> Unit): View {
        val g = ItemSettingGroupBinding.inflate(layoutInflater, b.accListSettings, false)
        g.devTxtGroupTitle.setText(title)
        g.devBadgeDev.isVisible = dev
        fill(g.devGroupItems)
        b.accListSettings.addView(g.root)
        return g.root
    }

    private fun row(parent: LinearLayout, @StringRes label: Int, @StringRes sub: Int): ItemSettingRowBinding {
        val r = ItemSettingRowBinding.inflate(layoutInflater, parent, false)
        r.devTxtLabel.setText(label)
        if (sub != 0) { r.devTxtSub.setText(sub); r.devTxtSub.isVisible = true }
        parent.addView(r.root)
        return r
    }

    private fun switch(parent: LinearLayout, @IdRes id: Int, @StringRes label: Int, @StringRes sub: Int, on: Boolean, onChange: (Boolean) -> Unit) {
        val r = row(parent, label, sub)
        r.devSw.id = id
        r.devSw.isVisible = true
        r.devSw.contentDescription = getString(label)
        r.devSw.isChecked = on
        r.devSw.setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }

    private fun seg(parent: LinearLayout, @IdRes id: Int, @StringRes label: Int, @StringRes sub: Int, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
        val r = row(parent, label, sub)
        r.devSeg.id = id
        r.devSeg.isVisible = true
        r.devSeg.setBackgroundResource(R.drawable.bg_segment)
        r.devSeg.setPadding(pad(), pad(), pad(), pad())
        val ids = options.map { text ->
            (layoutInflater.inflate(R.layout.item_segment_button, r.devSeg, false) as MaterialButton).apply {
                this.id = View.generateViewId()
                this.text = text
                r.devSeg.addView(this)
            }.id
        }
        r.devSeg.check(ids[selected.coerceIn(ids.indices)])
        r.devSeg.addOnButtonCheckedListener { _, checkedId, checked -> if (checked) onPick(ids.indexOf(checkedId)) }
    }

    private fun resetSection() {
        val col = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        fun danger(@IdRes id: Int, @StringRes text: Int, onClick: () -> Unit) =
            (layoutInflater.inflate(R.layout.item_danger_button, col, false) as MaterialButton).apply {
                this.id = id
                setText(text)
                setOnClickListener { onClick() }
            }
        TextView(requireContext()).apply {
            setTextAppearance(R.style.TextAppearance_PracticeMart_Caption)
            setText(R.string.dev_reset_hint)
            col.addView(this)
        }
        col.addView(danger(R.id.dev_btn_reset_data, R.string.dev_reset_data) { confirmReset(R.string.dev_reset_data_title, R.string.dev_reset_data_body) { resetData() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = resources.getDimensionPixelSize(R.dimen.pm_space_3) })
        col.addView(danger(R.id.dev_btn_reset_prefs, R.string.dev_reset_prefs) { confirmReset(R.string.dev_reset_prefs_title, R.string.dev_reset_prefs_body) { resetPrefs() } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = resources.getDimensionPixelSize(R.dimen.pm_space_2_5) })
        b.accListSettings.addView(col, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.pm_space_4) })
    }

    private fun confirmReset(@StringRes title: Int, @StringRes body: Int, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title).setMessage(body)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dev_reset_confirm) { _, _ -> action() }
            .show()
    }

    /** Pengaturan kembali ke default; Tema dan Bahasa ikut diterapkan ulang. */
    private fun resetPrefs() {
        store.resetPrefs()
        applyAppearance(store.prefs.value)
        parentFragmentManager.popBackStack()
        nav.openSettings(dev = true)
    }

    /** Database, sesi login, dan status onboarding kembali ke awal tanpa kill proses (PRD §10, ARCHITECTURE §11). */
    private fun resetData() {
        requireContext().app.container.resetAll()
        startActivity(Intent(requireContext(), AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun about() {
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        listOf(
            Triple(R.id.dev_txt_version, R.string.about_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
            Triple(R.id.dev_txt_commit, R.string.about_commit, BuildConfig.COMMIT_SHA),
            Triple(R.id.dev_txt_build_date, R.string.about_build_date, BuildConfig.BUILD_DATE),
            Triple(R.id.dev_txt_contract, R.string.about_contract, "v1"),
        ).forEach { (id, key, value) ->
            val line = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, pad(), 0, pad()) }
            line.addView(TextView(requireContext()).apply { setTextAppearance(R.style.TextAppearance_PracticeMart_Body); setText(key) }, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(TextView(requireContext()).apply {
                this.id = id
                text = value
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono)
                setTextColor(requireContext().getColor(R.color.pm_ink))
            })
            box.addView(line)
        }
        b.accListSettings.addView(box)
    }

    companion object {
        private const val ARG_DEV = "dev"
        private val FLAKY = listOf(0, 30, 50)
        private val VA_WINDOWS = listOf(30_000L, 120_000L, 900_000L)
        private val INTERVALS = listOf(3_000L, 10_000L, 30_000L, 0L)
        fun newInstance(dev: Boolean) = SettingsFragment().apply { arguments = bundleOf(ARG_DEV to dev) }
    }
}
