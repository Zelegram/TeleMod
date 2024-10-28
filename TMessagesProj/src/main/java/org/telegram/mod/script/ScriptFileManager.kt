package org.telegram.mod.script

import android.content.Context
import org.telegram.mod.TeleModConst
import org.telegram.mod.data.RunAt
import org.telegram.mod.data.ScriptFile
import java.io.File

class ScriptFileManager(
    private val context: Context,
    private val botUserName: String
) {
    companion object{
        fun isDisabled(file: File): Boolean {
            return file.nameWithoutExtension.endsWith("_DISABLED")
        }

        fun toDisable(file: File): File{
            return File(file.parent, "${file.nameWithoutExtension}_DISABLED.${file.extension}")
        }

        fun toEnable(file: File): File {
            return File(file.parent, "${file.nameWithoutExtension.removeSuffix("_DISABLED")}.${file.extension}")
        }
    }
    private val scriptDirectory: File by lazy {
        File(TeleModConst.getScriptBaseDirectory(context), botUserName).apply { mkdirs() }
    }

    private val scriptFiles: Sequence<File>
        get() = scriptDirectory.walk().filter { it.extension == "js" }.sortedByDescending { it.lastModified() }

    fun getScripts(runAt: RunAt): List<ScriptFile> {
        return scriptFiles.filterNot { isDisabled(it) }
            .mapNotNull { file ->
                ScriptFile(file).takeIf { it.header?.name != null && it.header?.runAt == runAt }
            }
            .toList()
    }

    fun getScripts(): MutableList<ScriptFile> {
        return scriptFiles.mapNotNull { file ->
            ScriptFile(file).takeIf { it.header?.name != null }
        }.toMutableList()
    }

    fun getUniqueFile(name: String): File {
        var count = 0
        var file: File
        do {
            val fileName = if (count == 0) name else "${name}_$count"
            file = File(scriptDirectory, "$fileName.js")
            count++
        } while (file.exists())
        return file
    }

    fun toggleFile(file: File): File {
        val newFile = if (isDisabled(file))
            toEnable(file)
        else
            toDisable(file)

        if (newFile.exists()) newFile.delete()

        file.renameTo(newFile)
        return newFile
    }
}