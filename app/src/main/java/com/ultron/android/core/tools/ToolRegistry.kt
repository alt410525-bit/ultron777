package com.ultron.android.core.tools

interface ToolModule {
    val tools: List<Tool>
}

class ToolRegistry(modules: List<ToolModule>) {

    private val tools: Map<String, Tool>

    init {
        val all = modules.flatMap { it.tools }
        val dupes = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dupes.isEmpty()) { "Duplicate tool ids: $dupes" }
        tools = all.associateBy { it.id }
    }

    fun all(): List<Tool> = tools.values.toList()
    fun find(id: String): Tool? = tools[id]

    /** Sirf user ke message se milte-julte top-K tools LLM ko bhejo. */
    fun select(query: String, k: Int = 25): List<Tool> {
        val words = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        return all()
            .sortedByDescending { t ->
                "${t.id} ${t.title} ${t.description}".lowercase()
                    .split(Regex("\\W+")).count { it in words }
            }
            .take(k)
    }
}
