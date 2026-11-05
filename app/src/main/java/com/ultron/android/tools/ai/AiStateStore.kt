package com.ultron.android.tools.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Todo aur command aliases ka on-device store. Ek hi JSON file mein rehta hai
 * (filesDir/ultron_ai_state.json). Yaadein MemoryStore mein hain, yahan nahi.
 */
class AiStateStore(context: Context) {

    private val file = File(context.filesDir, "ultron_ai_state.json")

    data class Todo(val id: Long, val text: String, val done: Boolean)

    @Synchronized
    private fun readRoot(): JSONObject =
        runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    @Synchronized
    private fun writeRoot(root: JSONObject) {
        file.writeText(root.toString())
    }

    @Synchronized
    fun todos(): List<Todo> {
        val arr = readRoot().optJSONArray("todos") ?: return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Todo(o.getLong("id"), o.getString("text"), o.optBoolean("done", false))
        }
    }

    @Synchronized
    fun saveTodos(list: List<Todo>) {
        val root = readRoot()
        root.put("todos", JSONArray(list.map {
            JSONObject().put("id", it.id).put("text", it.text).put("done", it.done)
        }))
        writeRoot(root)
    }

    @Synchronized
    fun aliases(): Map<String, String> {
        val o = readRoot().optJSONObject("aliases") ?: return emptyMap()
        return o.keys().asSequence().associateWith { o.getString(it) }
    }

    @Synchronized
    fun saveAliases(map: Map<String, String>) {
        val root = readRoot()
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        root.put("aliases", o)
        writeRoot(root)
    }
}
