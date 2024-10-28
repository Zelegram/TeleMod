package org.telegram.mod.script

import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.mod.TeleModActivity
import org.telegram.mod.browser.WebEvents
import org.telegram.mod.data.RunAt
import org.telegram.mod.data.ScriptFile
import org.telegram.mod.editor.EditorCompat
import org.telegram.mod.editor.ScriptEditorActivity
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.NotificationsCheckCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.LaunchActivity
import java.io.File

class ScriptManager(
    private val webView: WebView,
    private val scope: CoroutineScope,
    private val botId: Long
) : WebEvents {
    private val context get() = webView.context

    var keepScreenOn: Boolean
        get() = webView.keepScreenOn
        set(value) {
            webView.keepScreenOn = value
        }

    private val botUser by lazy {
        MessagesController.getInstance(UserConfig.selectedAccount).getUser(botId)
    }

    private val botUserName: String
        get() = UserObject.getPublicUsername(botUser) ?: botUser.username

    private val botChannelName: String
        get() = listOfNotNull(
            botUser.first_name,
            botUser.last_name
        ).joinToString(" ")

    private val currentUsername by lazy { UserConfig.getInstance(UserConfig.selectedAccount).currentUser.username }
    private val scriptFileManager by lazy { ScriptFileManager(context, botUserName) }

    init {
        EditorCompat.setupTextmate(context)
    }

    private fun injectScript(runAt: RunAt) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { scriptFileManager.getScripts(runAt) }
            }

            result.onSuccess {
                it.forEach { item ->
                    evaluateJavascript(item.script)
                }
            }
        }
    }

    fun toggleConsole() {
        evaluateJavascript("window.toggleEruda();")
    }

    override fun onPageStarted(url: String?) {
        val initScript = context.assets.open("mod/init.js").bufferedReader().use { it.readText() }
        evaluateJavascript(initScript)
        logState("load-start")
        injectScript(RunAt.LoadStart)
    }

    override fun onDocumentStart() {
        logState("document-start")
        injectScript(RunAt.DocumentStart)
    }

    override fun onDocumentEnd() {
        val script = context.assets.open("mod/eruda.js").bufferedReader().use { it.readText() }
        evaluateJavascript(script)
        logState("document-end")
        injectScript(RunAt.DocumentEnd)
    }

    override fun onDocumentBody() {
        logState("document-body")
        injectScript(RunAt.DocumentBody)
    }

    override fun onDocumentIdle() {
        logState("document-idle")
        injectScript(RunAt.DocumentIdle)
    }

    override fun onContextMenu() {
        logState("context-menu")
        injectScript(RunAt.ContextMenu)
    }

    override fun onPageFinished(url: String?) {
        logState("load-done")
        injectScript(RunAt.LoadDone)
    }

    private fun logState(vararg args: String) {
        val values = args.joinToString(",") { "'$it'" }
        evaluateJavascript("window.safeLog($values);")
    }

    private fun evaluateJavascript(script: String, callback: ValueCallback<String>? = null) {
        webView.evaluateJavascript(script, callback)
    }

    fun toggleScript() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { scriptFileManager.getScripts() }
            }

            result.onSuccess { scriptContentItems ->
                if (scriptContentItems.isNotEmpty()) {
                    showScriptListDialog(scriptContentItems)
                } else {
                    showEmptyScriptDialog()
                }
            }.onFailure {
                Toast.makeText(context, it.message ?: "Error fetching scripts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyScriptDialog() {
        AlertDialog.Builder(context).apply {
            setTitle(botChannelName)
            setTopAnimation(R.raw.utyan_empty, Theme.getColor(Theme.key_dialogTopBackground))
            setMessage(LocaleController.getString(R.string.empty_scripts))
            setPositiveButton(LocaleController.getString(R.string.new_script)) { _, _ -> newScript() }
            setNegativeButton(LocaleController.getString(R.string.find_scripts)) { _, _ -> findScripts() }
            setNeutralButton(LocaleController.getString(R.string.Dismiss), null)
        }.show()
    }

    private fun findScripts() {
        LaunchActivity.getSafeLastFragment()?.let { currentFragment ->
            scope.launch {
                val scriptsResult = withContext(Dispatchers.IO) {
                    runCatching { scriptFileManager.getScripts() }
                }

                scriptsResult.onSuccess { initialItems ->
                    currentFragment.showAsSheet(TeleModActivity {
                        refreshScriptsIfChanged(initialItems)
                    })
                }.onFailure {
                    Toast.makeText(context, it.message ?: "Error fetching scripts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun newScript() {
        openScript(createScriptTemplate(), scriptFileManager.getUniqueFile(currentUsername))
    }

    private fun refreshScriptsIfChanged(initialItems: List<ScriptFile>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { scriptFileManager.getScripts() }
            }.onSuccess { newItems ->
                if (newItems != initialItems) requestReload()
            }
        }
    }

    private fun showScriptListDialog(items: MutableList<ScriptFile>) {
        val tempItems = items.toMutableList()
        val scriptAdapter = LocalScriptAdapter(items)
        val recyclerListView = RecyclerListView(context).apply {
            adapter = scriptAdapter
            layoutManager = LinearLayoutManager(context)
        }
        var shouldReload = true
        val dialog = AlertDialog.Builder(context).apply {
            setView(recyclerListView)
            setPositiveButton(LocaleController.getString(R.string.new_script)) { _, _ ->
                shouldReload = false
                newScript()
            }
            setNegativeButton(LocaleController.getString(R.string.find_scripts)) { _, _ ->
                shouldReload = false
                findScripts()
            }
            setNeutralButton(LocaleController.getString(R.string.Dismiss), null)
            setOnDismissListener {
                if (shouldReload && items != tempItems)
                    requestReload()
            }
        }.show()

        recyclerListView.setOnItemClickListener { view, position, x, _ ->
            // position is not safe here, because of swipe to remove
            // Find the item based on the view's tag
            val item = items.find { it.content == view.tag } ?: return@setOnItemClickListener
            if (scriptAdapter.isToggleSwitchArea(view, x)) {
                val newFile = scriptFileManager.toggleFile(item.file)
                items[items.indexOf(item)] = item.copy(file = newFile)
                (view as NotificationsCheckCell).isChecked = !ScriptFileManager.isDisabled(newFile)
                if (shouldReload && items != tempItems)
                    Toast.makeText(context,R.string.required_reload_message,Toast.LENGTH_SHORT).show()
            } else {
                shouldReload = false
                dialog.dismiss()
                openScript(item.content, item.file)
            }
        }

        ItemTouchHelper(LocalScriptItemTouchCallback(context) { position ->
            items[position].file.delete()
            items.removeAt(position)
            scriptAdapter.notifyItemRemoved(position)
            if (items.isEmpty())
                dialog.dismiss()
        }).attachToRecyclerView(recyclerListView)
    }

    private fun requestReload() {
        AlertDialog(context, AlertDialog.ALERT_TYPE_MESSAGE).apply {
            setTopAnimation(R.raw.utyan_gigagroup, Theme.getColor(Theme.key_dialogTopBackground))
            setMessage(LocaleController.getString(R.string.required_reload_message))
            setPositiveButton(LocaleController.getString(R.string.OK)) { _, _ ->
                webView.animate().cancel()
                webView.reload()
            }
            setNegativeButton(LocaleController.getString(R.string.Cancel), null)
        }.show()
    }

    private fun createScriptTemplate(): String {
        val currentUser = UserConfig.getInstance(UserConfig.selectedAccount).currentUser.username
        return context.assets.open("mod/template.js").bufferedReader().use { it.readText() }
            .replace("#NAME", botChannelName)
            .replace("#AUTHOR", currentUser)
    }

    private fun openScript(input: String, file: File) {
        val lastFragment = LaunchActivity.getSafeLastFragment() ?: return
        val scriptActivity = ScriptEditorActivity(botChannelName, input) { output, runAt ->
            scope.launch {
                withContext(Dispatchers.IO){ file.writeText(output) }
                if (runAt == null){
                    evaluateJavascript(output)
                }else{
                    requestReload()
                }
            }
        }
        lastFragment.showAsSheet(scriptActivity)
    }
}