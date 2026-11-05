package com.ultron.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Ultron's memory ("Yaadein"). Everything stays on-device in the app's
 * private storage — mirrors Maya's privacy model (data never uploaded).
 */
class MemoryStore(private val context: Context) {

    private val memoryFile: File =
        File(context.filesDir, "ultron_memory.json")

    data class Memory(
        val id: Long,
        val text: String,
        val kind: String,      // "person" | "fact" | "preference" | "routine"
        val timestamp: Long
    )

    @Synchronized
    fun loadMemories(): List<Memory> {
        if (!memoryFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(memoryFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Memory(
                            id = o.getLong("id"),
                            text = o.getString("text"),
                            kind = o.optString("kind", "fact"),
                            timestamp = o.getLong("ts")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun addMemory(text: String, kind: String = "fact") {
        val memories = loadMemories().toMutableList()
        val mem = Memory(id = System.currentTimeMillis(), text = text, kind = kind, timestamp = System.currentTimeMillis())
        memories.add(mem)
        save(memories)
    }

    @Synchronized
    fun deleteMemory(id: Long) {
        save(loadMemories().filter { it.id != id })
    }

    @Synchronized
    fun clear() {
        memoryFile.delete()
    }

    private fun save(memories: List<Memory>) {
        val arr = JSONArray()
        memories.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("text", m.text)
                    .put("kind", m.kind)
                    .put("ts", m.timestamp)
            )
        }
        memoryFile.writeText(arr.toString())
    }

    suspend fun export(): String = withContext(Dispatchers.IO) {
        memoryFile.readText()
    }
}
