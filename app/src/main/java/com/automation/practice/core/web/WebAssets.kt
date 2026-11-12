package com.automation.practice.core.web

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.IOException

/** Sajikan assets/web/ di domain palsu supaya tidak ada request keluar device (ARCHITECTURE.md §12). */
fun webAssetLoader(context: Context, domain: String): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .setDomain(domain)
        .addPathHandler("/") { path ->
            try {
                WebResourceResponse(mimeOf(path), "utf-8", context.assets.open("web/$path"))
            } catch (e: IOException) {
                null
            }
        }
        .build()

private fun mimeOf(path: String) = when (path.substringAfterLast('.')) {
    "css" -> "text/css"
    "js" -> "text/javascript"
    else -> "text/html"
}
