package org.telegram.mod.browser

interface WebEvents {
    fun onPageStarted(url: String?)
    fun onDocumentStart()
    fun onDocumentEnd()
    fun onDocumentBody()
    fun onDocumentIdle()
    fun onContextMenu()
    fun onPageFinished(url: String?)
}