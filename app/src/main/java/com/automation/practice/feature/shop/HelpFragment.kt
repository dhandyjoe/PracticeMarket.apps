package com.automation.practice.feature.shop

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.core.web.webAssetLoader
import com.automation.practice.databinding.FragmentHelpBinding

/** HELP-01 Pusat Bantuan: WebView lokal dengan FAQ dan JS bridge ke toast native (FR-HELP-01). */
class HelpFragment : ShopFragment(R.layout.fragment_help) {
    private var web: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHelpBinding.bind(view)
        b.shopTopbar.setup(R.string.acc_help)
        b.helpBarChat.applySystemInsets(bottom = true)
        web = b.helpWeb

        val loader = webAssetLoader(requireContext(), DOMAIN)
        b.helpWeb.settings.javaScriptEnabled = true
        b.helpWeb.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        b.helpWeb.addJavascriptInterface(Bridge(), "Android")
        if (savedInstanceState == null) b.helpWeb.loadUrl("https://$DOMAIN/help.html") else b.helpWeb.restoreState(savedInstanceState)
        b.helpBtnChat.setOnClickListener { contact() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web?.saveState(outState)
    }

    override fun onDestroyView() {
        web?.destroy()
        web = null
        super.onDestroyView()
    }

    private fun contact() = Toast.makeText(requireContext(), R.string.help_connecting, Toast.LENGTH_SHORT).show()

    private inner class Bridge {
        /** Dipanggil dari tombol "Hubungi kami" di halaman web (thread JavaBridge → pindah ke UI). */
        @JavascriptInterface
        fun contact() {
            activity?.runOnUiThread { this@HelpFragment.contact() }
        }
    }

    private companion object {
        const val DOMAIN = "help.practicemart.test"
    }
}
