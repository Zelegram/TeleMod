package org.telegram.mod.data

import com.google.android.exoplayer2.util.Log
import java.io.File

enum class RunAt(val value: String) {
    LoadStart("load-start"),
    DocumentStart("document-start"),
    DocumentBody("document-body"),
    DocumentEnd("document-end"),
    DocumentIdle("document-idle"),
    ContextMenu("context-menu"),
    LoadDone("load-done");

    companion object {
        fun fromString(value: String?): RunAt? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

data class ScriptHeader(
    val name: String? = null,
    val version: String? = null,
    val description: String? = null,
    val author: String? = null,
    val icon: String? = null,
    val runAt: RunAt? = null
) {
    fun toHeader(): String = ScriptContentCompat.toHeader(this)
}

data class ScriptFile(val file: File) {
    private fun safeReadText(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e("ScriptManager", "Error reading file ${file.name}: ${e.message}")
            ""
        }
    }

    val content = safeReadText(file)
    val script by lazy { ScriptContentCompat.getScriptWithoutHeader(content) }
    val header by lazy { ScriptContentCompat.parseHeader(content) }
}

object ScriptContentCompat {
    private const val HEADER_START = "==TeleModScript=="
    private const val HEADER_END = "==/TeleModScript=="

    fun getScriptWithoutHeader(content: String): String =
        content.substringAfter(HEADER_END).trim()

    fun parseHeader(content: String): ScriptHeader? {
        val startIndex = content.indexOf(HEADER_START)
        val endIndex = content.indexOf(HEADER_END)

        return if (startIndex != -1 && endIndex != -1) {
            // Extract the header part between the tags
            val header = content.substring(startIndex, endIndex)

            // Simplified function to extract key-value pairs using regex
            fun extract(tag: String): String? {
                return """@$tag\s+(.+)""".toRegex().find(header)?.groupValues?.get(1)?.trim()
            }
            ScriptHeader(
                name = extract("name"),
                version = extract("version"),
                description = extract("description"),
                author = extract("author"),
                icon = extract("icon"),
                runAt = RunAt.fromString(extract("run-at"))
            )
        } else null
    }

    fun toHeader(metadata: ScriptHeader): String {
        val headerBuilder = StringBuilder()
        // List of key-value pairs to include only non-null values
        val metadataMap = mapOf(
            "name" to metadata.name,
            "version" to metadata.version,
            "description" to metadata.description,
            "icon" to metadata.icon,
            "author" to metadata.author,
            "run-at" to metadata.runAt?.value
        ).filterValues { it != null } // Filter out null values

        // Determine the longest key to adjust spaces dynamically
        val maxKeyLength = metadataMap.keys.maxOfOrNull { it.length } ?: 0
        val padding = 4 // Base padding after the key

        // Start of the TeleModScript
        headerBuilder.appendLine(HEADER_START)

        // Append each key-value pair with dynamic spacing
        for ((key, value) in metadataMap) {
            val formattedKey = key.padEnd(maxKeyLength + padding) // Dynamically pad the key
            headerBuilder.appendLine("@$formattedKey$value")
        }

        // End of the TeleModScript
        headerBuilder.appendLine(HEADER_END)

        return headerBuilder.toString()
            .split("\n")
            .filter { it.isNotEmpty() }
            .joinToString("\n") { "// $it" }
    }
}