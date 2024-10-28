package org.telegram.mod.editor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import org.telegram.ui.ActionBar.Theme

object EditorCompat {
    fun language(): TextMateLanguage {
        val languageScopeName = "source.js" // The scope name of target language
        return TextMateLanguage.create(
            languageScopeName, true /* true for enabling auto-completion */
        )
    }

    fun setupTextmate(context: Context) {
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.assets)
        )
        val isDarkMode = Theme.isCurrentThemeDark()
        val basePath = "mod/textmate"
        val themeRegistry = ThemeRegistry.getInstance()
        val name = if (isDarkMode) "solarized-dark" else "solarized-light"
        val themeAssetsPath = "$basePath/$name.json"
        themeRegistry.loadTheme(
            ThemeModel(
                IThemeSource.fromInputStream(
                    FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath), themeAssetsPath, null
                ),
                name
            ).apply {
                isDark = isDarkMode
            }
        )
        ThemeRegistry.getInstance().setTheme(name)
        GrammarRegistry.getInstance().loadGrammars("$basePath/languages.json")
    }
}