package org.telegram.mod

import android.content.Context
import android.os.Environment
import io.noties.markwon.Markwon
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import java.io.File

object TeleModConst {
    const val SCRIPT_REPO_URL = "https://raw.githubusercontent.com/Zelegram/Scripts/main/telemod.json"
    fun getScriptBaseDirectory(context: Context): File {
        return context.getExternalFilesDir("TeleModScript") ?: File(Environment.getExternalStorageDirectory(),"Android/data/${context.packageName}/files").also { if (!it.exists()) it.mkdirs() }
    }

    private fun getSharedPreferences(context: Context) = context.getSharedPreferences("TeleMod", Context.MODE_PRIVATE)
    private const val PREF_SHOW_NOTICE = "show_notice"

    fun showNotice(context: Context){
        val sharedPref = getSharedPreferences(context)
        if (sharedPref.getBoolean(PREF_SHOW_NOTICE,true)) {
            AlertDialog(context, AlertDialog.ALERT_TYPE_MESSAGE).apply {
                setCancelable(false)
                setTopAnimation(R.raw.utyan_gigagroup,Theme.getColor(Theme.key_dialogTopBackground))
                setTitle(
                    context.getString(
                        R.string.telemod_version,
                        BuildConfig.BUILD_VERSION_STRING
                    )
                )
                setMessage(
                    Markwon.create(context).toMarkdown(
                        context.assets.open("mod/notice.md").bufferedReader().use { it.readText() }
                    )
                )
                setPositiveButton(LocaleController.getString(R.string.ChannelJoin)) { _, _ ->
                    context.openInternalLink("https://t.me/ZelegramApp")
                }
                setNegativeButton(LocaleController.getString(R.string.got_it)) { _, _ ->
                    sharedPref.edit().putBoolean(PREF_SHOW_NOTICE,false).apply()
                }
            }.show()
        }
    }
}