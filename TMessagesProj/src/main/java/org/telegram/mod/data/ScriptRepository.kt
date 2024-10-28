package org.telegram.mod.data

import org.json.JSONArray
import org.telegram.mod.TeleModConst
import java.net.URL

data class BotScript(
    val name: String,
    val id: String,
    val items: List<ScriptItem>
)

data class ScriptItem(
    val title: String,
    val version: String,
    val icon: String,
    val author: String,
    val url: String,
    val ref: String
)

object ScriptRepository {
    private fun parseJson(jsonString: String): List<BotScript> {
        val jsonArray = JSONArray(jsonString)
        return List(jsonArray.length()) { i ->
            val botObject = jsonArray.getJSONObject(i)
            val botId = botObject.getString("id")
            val botName = botObject.getString("name")

            val scriptArray = botObject.getJSONArray("items")
            val scriptItems = List(scriptArray.length()) { j ->
                scriptArray.getJSONObject(j).run {
                    ScriptItem(
                        getString("title"),
                        getString("version"),
                        getString("icon"),
                        getString("author"),
                        getString("url"),
                        optString("ref")
                    )
                }
            }
            BotScript(botName, botId, scriptItems)
        }
    }

    fun loadItems(): List<BotScript>{
        val json = URL(TeleModConst.SCRIPT_REPO_URL).readText()
        return parseJson(json)
    }
}