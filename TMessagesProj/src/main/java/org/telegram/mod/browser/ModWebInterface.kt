package org.telegram.mod.browser

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import org.telegram.messenger.AndroidUtilities
import org.telegram.mod.script.ScriptManager

@SuppressLint("JavascriptInterface")
internal class ModWebInterface(
    private val scriptManager: ScriptManager
) {
    @JavascriptInterface
    fun setKeepScreenOn(state: Boolean = false) = AndroidUtilities.runOnUIThread {
        scriptManager.keepScreenOn = state
    }

    @JavascriptInterface
    fun onDocumentStart() = AndroidUtilities.runOnUIThread {
        scriptManager.onDocumentStart()
    }

    @JavascriptInterface
    fun onDocumentBody() = AndroidUtilities.runOnUIThread {
        scriptManager.onDocumentBody()
    }

    @JavascriptInterface
    fun onDocumentEnd() = AndroidUtilities.runOnUIThread {
        scriptManager.onDocumentEnd()
    }

    @JavascriptInterface
    fun onDocumentIdle() = AndroidUtilities.runOnUIThread {
        scriptManager.onDocumentIdle()
    }

    @JavascriptInterface
    fun onContextMenu() = AndroidUtilities.runOnUIThread {
        scriptManager.onContextMenu()
    }
}