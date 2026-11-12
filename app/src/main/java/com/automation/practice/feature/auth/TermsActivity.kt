package com.automation.practice.feature.auth

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.automation.practice.core.ui.applySystemBarsPadding
import com.automation.practice.core.web.webAssetLoader
import com.automation.practice.databinding.ActivityTermsBinding

/** S&K di WebView (konteks WEBVIEW_com.automation.practice), disajikan dari assets/web/terms.html. */
class TermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val b = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarsPadding()
        b.shopBtnBack.setOnClickListener { finish() }

        val loader = webAssetLoader(this, DOMAIN)
        b.shopWebTerms.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        if (savedInstanceState == null) b.shopWebTerms.loadUrl("https://$DOMAIN/terms.html")
        else b.shopWebTerms.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        findViewById<WebView>(com.automation.practice.R.id.shop_web_terms).saveState(outState)
    }

    private companion object {
        const val DOMAIN = "help.practicemart.test"
    }
}
