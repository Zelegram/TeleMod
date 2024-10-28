package org.telegram.mod.browser

import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

internal class ModWebViewClient : WebViewClient() {
    private val clients = mutableListOf<WebViewClient>()

    fun addClient(client: WebViewClient) {
        clients.add(client)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        clients.forEach { it.onPageFinished(view,url) }
        super.onPageFinished(view, url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        clients.forEach { it.onPageStarted(view, url, favicon) }
        super.onPageStarted(view, url, favicon)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return clients.firstNotNullOfOrNull { client ->
                client.shouldInterceptRequest(view, request)
            } ?: super.shouldInterceptRequest(view, request)
        }
        return super.shouldInterceptRequest(view, request)
    }
}