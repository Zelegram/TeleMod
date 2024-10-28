package org.telegram.mod.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.telegram.mod.script.ScriptManager
import org.telegram.tgnet.TLRPC
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

open class TeleModWebView(
    context: Context,
    botUser: TLRPC.User
) : WebView(context) {

    companion object{
        const val TAG = "TeleModWebView"
    }

    var queryData: String? = null
    var appUrl: String? = null
    var fullAppUrl: String? = null
    var botId: Long = botUser.id

    private val modWebClient = ModWebViewClient()
    private lateinit var scriptManager: ScriptManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val customWebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            scriptManager.onPageFinished(url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            scriptManager.onPageStarted(url)
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()

            if (!request.isForMainFrame || !request.method.equals("GET", ignoreCase = true)) {
                return null
            }
            val headers = request.requestHeaders.toMutableMap()
            if (headers["content-type"] == "application/x-www-form-urlencoded") {
                return null
            }
            CookieManager.getInstance().getCookie(url)?.let { cookie ->
                headers["cookie"] = cookie
            }

            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    headers.forEach { (key, value) -> setRequestProperty(key, value) }
                    connect()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseHeaders = connection.headerFields.mapValues { entry ->
                        entry.value.joinToString(", ")
                    }.filterKeys { key ->
                        key != "Content-Security-Policy" && key != "X-Content-Security-Policy"
                    }

                    val contentType = connection.contentType?.split(";")?.first()?.trim() ?: "text/html"
                    val contentEncoding = connection.contentEncoding?.takeIf { it.isNotBlank() } ?: "utf-8"

                    return WebResourceResponse(
                        contentType,
                        contentEncoding,
                        connection.responseCode,
                        connection.responseMessage,
                        responseHeaders,
                        connection.inputStream
                    )
                } else {
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    init {
        initMod()
    }

    @SuppressLint("JavascriptInterface")
    fun initMod() {
        modWebClient.addClient(customWebViewClient)
        scriptManager = ScriptManager(this,scope,botId)
        addJavascriptInterface(ModWebInterface(scriptManager), "TeleMod")
    }

    fun toggleConsole() = scriptManager.toggleConsole()

    fun toggleScript() = scriptManager.toggleScript()

    private fun parseBotUrl(url: String){
        try {
            if (url.contains("#tgWebAppData=")) {
                fullAppUrl = url
                appUrl = url.substringBefore("#tgWebAppData=")
                queryData = url.substringAfter("#tgWebAppData=").substringBefore("&tgWebAppVersion=")
                queryData = URLDecoder.decode(queryData, "UTF-8")
            }
        }catch (ignored: Exception){}
    }

    override fun setWebViewClient(client: WebViewClient) {
        // Add the new client to the composite client
        modWebClient.addClient(client)
        super.setWebViewClient(modWebClient) // Always set the composite client
    }

    override fun loadUrl(url: String) {
        parseBotUrl(url)
        super.loadUrl(url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        parseBotUrl(url)
        super.loadUrl(url, additionalHttpHeaders)
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }
}