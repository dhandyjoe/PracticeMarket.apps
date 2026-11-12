package com.automation.practice.feature.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.automation.practice.R
import com.automation.practice.databinding.FragmentOnboardingBinding
import com.automation.practice.databinding.ItemOnboardingSlideBinding

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    private val nav get() = requireActivity() as AuthNavigator

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentOnboardingBinding.bind(view)
        b.shopPagerOnboarding.adapter = SlideAdapter(SLIDES)

        val small = resources.getDimensionPixelSize(R.dimen.pm_dot)
        val active = resources.getDimensionPixelSize(R.dimen.pm_dot_active)
        val dots = SLIDES.map {
            View(requireContext()).also { dot ->
                b.onboardingDots.addView(
                    dot,
                    LinearLayout.LayoutParams(small, small).apply { marginStart = small / 2; marginEnd = small / 2 },
                )
            }
        }

        fun render(page: Int) {
            dots.forEachIndexed { i, dot ->
                dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply { width = if (i == page) active else small }
                dot.setBackgroundResource(if (i == page) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            }
            val last = page == SLIDES.lastIndex
            b.shopBtnNext.isVisible = !last
            b.shopBtnStart.isVisible = last
        }
        render(b.shopPagerOnboarding.currentItem)
        b.shopPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = render(position)
        })

        b.shopBtnNext.setOnClickListener { b.shopPagerOnboarding.currentItem += 1 }
        b.shopBtnSkip.setOnClickListener { complete() }
        b.shopBtnStart.setOnClickListener { complete() }
    }

    // FR-AUTH-02: izin notifikasi diminta setelah onboarding (Android 13+).
    private fun complete() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else finish()
    }

    private fun finish() = nav.onboardingDone(requireArguments().getBoolean(ARG_TO_HOME))

    private class Slide(@DrawableRes val image: Int, @StringRes val title: Int, @StringRes val body: Int)

    private class SlideAdapter(private val slides: List<Slide>) : RecyclerView.Adapter<SlideAdapter.Holder>() {
        class Holder(val b: ItemOnboardingSlideBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemOnboardingSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = slides.size

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.b) {
            slides[position].let {
                shopImgSlide.setImageResource(it.image)
                shopTxtOnboardingTitle.setText(it.title)
                shopTxtOnboardingBody.setText(it.body)
            }
        }
    }

    companion object {
        private const val ARG_TO_HOME = "to_home"
        private val SLIDES = listOf(
            Slide(R.drawable.ill_onboarding_1, R.string.onboarding_1_title, R.string.onboarding_1_body),
            Slide(R.drawable.ill_onboarding_2, R.string.onboarding_2_title, R.string.onboarding_2_body),
            Slide(R.drawable.ill_onboarding_3, R.string.onboarding_3_title, R.string.onboarding_3_body),
        )

        /** [toHome]: tampil ulang untuk `new_user` setelah login, jadi selesai langsung ke Beranda. */
        fun newInstance(toHome: Boolean) = OnboardingFragment().apply { arguments = bundleOf(ARG_TO_HOME to toHome) }
    }
}
