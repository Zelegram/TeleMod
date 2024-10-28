package org.telegram.mod

import android.content.Context
import android.net.Uri
import org.telegram.messenger.browser.Browser

fun Context.openInternalLink(link: String){
    var path = link
    if (path.startsWith("/")) {
        path = path.substring(1)
    }

    if (!path.startsWith("http"))
        path = "https://t.me/$path"
    Browser.openUrl(this, Uri.parse(path), true, false, false, null, null, false, true)
}