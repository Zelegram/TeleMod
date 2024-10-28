package org.telegram.mod.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.mod.data.RunAt
import org.telegram.mod.data.ScriptContentCompat
import org.telegram.mod.data.ScriptHeader
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory

class ScriptEditorActivity(
    private val name: String,
    private val input: String,
    private val callback: (content: String, runAt: RunAt?) -> Unit
): BaseFragment() {

    companion object{
        val SYMBOLS = arrayOf(
            "->", "{", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )
        val SYMBOL_INSERT_TEXT = arrayOf(
            "\t", "{}", "}", "(", ")",
            ",", ".", ";", "\"", "?",
            "+", "-", "*", "/", "<",
            ">", "[", "]", ":"
        )
    }

    private var editorView: CodeEditor? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun createView(context: Context?): View {
        setupActionBar()

        val rootView = LayoutInflater.from(context).inflate(R.layout.activity_script_editor,null,false)
        editorView = rootView.findViewById<CodeEditor>(R.id.editor).apply {
            setEditorLanguage(EditorCompat.language())
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            typefaceText = Typeface.MONOSPACE
        }.also {
            it.setText(input)
        }
        val inputView = rootView.findViewById<SymbolInputView>(R.id.symbol_input)
        inputView.bindEditor(editorView)
        inputView.addSymbols(SYMBOLS, SYMBOL_INSERT_TEXT)
        inputView.forEachButton { it.typeface = Typeface.MONOSPACE }

        // To fix BulletinFactory
        fragmentView = rootView

        return fragmentView
    }

    private fun setupActionBar() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(name)
        actionBar.setAllowOverlayTitle(true)

        val menu = actionBar.menu ?: actionBar.createMenu()
        menu.clearItems()

        val menuUndo = R.drawable.round_undo_24
        val menuRedo = R.drawable.round_redo_24
        val menuClear = R.drawable.round_delete_24
        val menuEval = R.drawable.round_play_arrow_24
        val menuSave = R.drawable.round_save_24dp
        val runSubMenuIds = RunAt.entries.associateWith { View.generateViewId() }

        val menuMore = R.drawable.ic_ab_other

        menu.addItem(menuUndo,menuUndo)
        menu.addItem(menuRedo,menuRedo)
        menu.addItem(menuClear,menuClear)
        menu.addItem(menuEval,menuEval)

        menu.addItem(menuSave,menuSave).also {
            runSubMenuIds.forEach { (runAt, id) ->
                it.addSubItem(id, runAt.value)
            }
        }

        val demoPath = "mod/demo"
        val moreSubMenuIds = context.assets.list(demoPath)?.associateWith { View.generateViewId() } ?: emptyMap()
        menu.addItem(menuMore,menuMore).also {
            moreSubMenuIds.forEach { (name, id) ->
                val title = "Demo $name".substringBeforeLast(".")
                    .split(Regex("_| | -"))
                    .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                it.addSubItem(id, title)
            }
        }

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                when(id){
                    -1 -> finishFragment()
                    menuUndo -> editorView?.undo()
                    menuRedo -> editorView?.redo()
                    menuClear -> editorView?.clearText()
                    menuEval -> runScript(null)
                    else -> {
                        runSubMenuIds.entries.firstOrNull { it.value == id }?.let { (runAt, _) ->
                            runScript(runAt)
                        }
                        moreSubMenuIds.entries.firstOrNull { it.value == id}?.let { (name, _) ->
                            loadDemo("$demoPath/$name")
                        }
                    }
                }
            }
        })
    }

    private fun runScript(runAt: RunAt?){
        val content = editorView?.text.toString()

        val bulletinFactory = BulletinFactory.of(this)
        if (content.isEmpty()){
            bulletinFactory.createErrorBulletin(LocaleController.getString(R.string.script_editor_empty)).show()
            return
        }

        val script = ScriptContentCompat.getScriptWithoutHeader(content)
        if (script.isEmpty()){
            bulletinFactory.createErrorBulletin(LocaleController.getString(R.string.script_editor_no_body)).show()
            return
        }

        val header = ScriptContentCompat.parseHeader(content)
            ?.copy(runAt = runAt)
            ?: ScriptHeader(name, runAt = runAt)

        val formattedCode = buildString {
            append(header.toHeader())
            append("\n\n")
            append(script)
        }

        callback.invoke(formattedCode, runAt)

        finishFragment()
    }

    private fun CodeEditor.clearText(){
        if (text.isNotEmpty()) text.delete(0,text.length)
    }

    private fun loadDemo(path: String){
        val script = context.assets.open(path).bufferedReader().use { it.readText() }
        editorView?.let {
            it.clearText()
            it.pasteText(script)
        }
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        editorView?.release()
    }
}