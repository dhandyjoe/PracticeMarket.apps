package com.automation.practice.feature.shop

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemBarsPadding
import com.automation.practice.core.web.webAssetLoader
import com.automation.practice.databinding.ActivityThreeDsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * SHOP-13 3D Secure di WebView (konteks WEBVIEW_com.automation.practice), disajikan dari assets/web/3ds.html.
 * Hasil: RESULT_OK = OTP benar, RESULT_CANCELED = dibatalkan.
 */
class ThreeDsActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val b = ActivityThreeDsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.shopBtnCloseThreeds.setOnClickListener { finishWith(false) }

        val loader = webAssetLoader(this, DOMAIN)
        b.shopWebThreeds.settings.javaScriptEnabled = true
        b.shopWebThreeds.addJavascriptInterface(Bridge(), "PracticeBank")
        b.shopWebThreeds.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        // Tanpa WebChromeClient, confirm() di halaman selalu false; tampilkan sebagai dialog native.
        b.shopWebThreeds.webChromeClient = object : WebChromeClient() {
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                MaterialAlertDialogBuilder(this@ThreeDsActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }
        }
        if (savedInstanceState == null) {
            val amount = intent.getLongExtra(EXTRA_TOTAL, 0)
            val card = intent.getStringExtra(EXTRA_CARD).orEmpty()
            b.shopWebThreeds.loadUrl("https://$DOMAIN/3ds.html?amount=$amount&card=$card")
        } else {
            b.shopWebThreeds.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        findViewById<WebView>(R.id.shop_web_threeds).saveState(outState)
    }

    private fun finishWith(ok: Boolean) {
        setResult(if (ok) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    private inner class Bridge {
        @JavascriptInterface
        fun onResult(ok: Boolean) = runOnUiThread { finishWith(ok) }
    }

    companion object {
        private const val DOMAIN = "secure.practicebank.test"
        private const val EXTRA_TOTAL = "total"
        private const val EXTRA_CARD = "card"

        fun intent(context: Context, total: Long, cardLast4: String) =
            Intent(context, ThreeDsActivity::class.java).putExtra(EXTRA_TOTAL, total).putExtra(EXTRA_CARD, cardLast4)
    }
}
