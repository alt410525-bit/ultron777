package com.ultron.android.tools.ai

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import com.ultron.android.core.tools.*
import com.ultron.android.data.MemoryStore
import com.ultron.android.data.SettingsStore
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val A = ToolCategory.ASSISTANT

// Persona list. 18+ / intense mode yahan NAHI hai: wo Settings ke adult-confirmation gate se hi badlega.
private val PERSONAS = listOf("Mature", "Friendly", "Strict", "Professional", "Boss")

private val PROMPT_TEMPLATES = mapOf(
    "summarize" to "Is text ka 3 line mein summary do:\n{text}",
    "explain_simple" to "Ise bahut simple shabdon mein samjhao, jaise 5 saal ke bachche ko samjha rahe ho:\n{text}",
    "hinglish" to "Is text ko saral Hinglish mein likho:\n{text}",
    "polite_email" to "Ek polite email likho. Topic: {text}. Subject aur body dono do.",
    "action_items" to "Is text se action items ki numbered list nikalo:\n{text}"
)

private val POSITIVE = setOf("good", "great", "happy", "love", "thanks", "nice", "awesome", "accha", "badhiya", "khush", "shukriya", "mast")
private val NEGATIVE = setOf("bad", "sad", "angry", "hate", "problem", "tension", "worst", "bura", "dukhi", "gussa", "pareshan")

private val INTENTS = mapOf(
    "call" to listOf("call", "dial", "phone", "lagao"),
    "message" to listOf("sms", "message", "text", "whatsapp", "bhej"),
    "music" to listOf("song", "music", "play", "gaana"),
    "weather" to listOf("weather", "mausam", "temperature", "barish"),
    "reminder" to listOf("alarm", "timer", "remind", "yaad"),
    "calendar" to listOf("meeting", "event", "schedule", "calendar"),
    "github" to listOf("github", "repo", "issue", "commit", "pull"),
    "tasks" to listOf("todo", "task", "note", "kaam"),
    "device" to listOf("wifi", "bluetooth", "volume", "brightness", "flashlight", "battery")
)

private fun words(s: String): List<String> =
    s.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

private fun summarize(text: String, n: Int): String {
    val sentences = text.split(Regex("(?<=[.!?।])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    if (sentences.size <= n) return sentences.joinToString(" ")
    val freq = words(text).filter { it.length > 2 }.groupingBy { it }.eachCount()
    val keep = sentences.mapIndexed { i, s ->
        i to words(s).filter { it.length > 2 }.sumOf { freq[it] ?: 0 }
    }.sortedByDescending { it.second }.take(n).map { it.first }.sorted()
    return keep.joinToString(" ") { sentences[it] }
}

private fun batteryPct(ctx: Context): Int =
    (ctx.getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

class AiToolModule(private val settings: SettingsStore) : ToolModule {

    override val tools: List<Tool> = listOf(

        // ---------- Memory (Yaadein) ----------
        SpecTool(
            id = "ai_memory_save", category = A, title = "Yaad rakho",
            description = "Koi baat yaad rakho. kind: person, fact, preference ya routine.",
            params = listOf(
                ToolParam("text", "string", "Yaad rakhne wali baat", required = true),
                ToolParam("kind", "string", "person, fact, preference ya routine (default fact)")
            )
        ) { args, ctx ->
            val text = args.str("text")?.trim()?.takeIf { it.isNotEmpty() } ?: return@SpecTool ToolResult.Error("text chahiye")
            val kind = args.str("kind") ?: "fact"
            if (kind !in listOf("person", "fact", "preference", "routine")) return@SpecTool ToolResult.Error("kind galat hai")
            MemoryStore(ctx.appContext).addMemory(text, kind)
            ToolResult.Ok("Yaad rakh liya.")
        },
        SpecTool(
            id = "ai_memory_search", category = A, title = "Yaad dhoondho",
            description = "Yaadon mein keyword search karo.",
            params = listOf(ToolParam("query", "string", "Keyword", required = true))
        ) { args, ctx ->
            val q = args.str("query") ?: return@SpecTool ToolResult.Error("query chahiye")
            val hits = MemoryStore(ctx.appContext).loadMemories().filter { it.text.contains(q, ignoreCase = true) }
            ToolResult.Ok(if (hits.isEmpty()) "Kuch nahi mila." else hits.joinToString("\n") { "[${it.id}] ${it.text}" })
        },
        SpecTool(
            id = "ai_memory_list", category = A, title = "Yaadein dekho",
            description = "Saari yaadein ya kisi kind ki yaadein dikhao.",
            params = listOf(ToolParam("kind", "string", "person, fact, preference, routine (optional)"))
        ) { args, ctx ->
            val kind = args.str("kind")
            val list = MemoryStore(ctx.appContext).loadMemories().filter { kind == null || it.kind == kind }
            ToolResult.Ok(if (list.isEmpty()) "Abhi koi yaad nahi hai." else list.joinToString("\n") { "[${it.id}] (${it.kind}) ${it.text}" })
        },
        SpecTool(
            id = "ai_memory_delete", category = A, title = "Yaad mitao",
            description = "Ek yaad id se mitao (id ai_memory_list se milti hai).",
            params = listOf(ToolParam("id", "integer", "Memory id", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val id = args.long("id") ?: return@SpecTool ToolResult.Error("id chahiye")
            MemoryStore(ctx.appContext).deleteMemory(id)
            ToolResult.Ok("Yaad mita di.")
        },
        SpecTool(
            id = "ai_memory_clear", category = A, title = "Saari yaadein mitao",
            description = "Saari yaadein permanently mita do. Ye wapas nahi aata.",
            risk = RiskLevel.HIGH
        ) { _, ctx ->
            MemoryStore(ctx.appContext).clear()
            ToolResult.Ok("Saari yaadein mita di.")
        },
        SpecTool(
            id = "ai_memory_stats", category = A, title = "Yaad ke aankde",
            description = "Kitni yaadein hain aur kis kind ki kitni hain."
        ) { _, ctx ->
            val all = MemoryStore(ctx.appContext).loadMemories()
            val byKind = all.groupingBy { it.kind }.eachCount()
            ToolResult.Ok("Kul ${all.size} yaadein. " + (if (byKind.isEmpty()) "" else byKind.entries.joinToString(", ") { "${it.key}: ${it.value}" }))
        },

        // ---------- Persona ----------
        SpecTool(
            id = "ai_persona_get", category = A, title = "Persona dekho",
            description = "Abhi Ultron ka persona kya hai batao."
        ) { _, _ ->
            ToolResult.Ok("Persona: ${settings.persona}. Available: ${PERSONAS.joinToString(", ")}")
        },
        SpecTool(
            id = "ai_persona_set", category = A, title = "Persona badlo",
            description = "Ultron ka persona badlo: Mature, Friendly, Strict, Professional ya Boss.",
            params = listOf(ToolParam("persona", "string", PERSONAS.joinToString(", "), required = true))
        ) { args, _ ->
            val p = args.str("persona") ?: return@SpecTool ToolResult.Error("persona chahiye")
            val match = PERSONAS.firstOrNull { it.equals(p, ignoreCase = true) }
                ?: return@SpecTool ToolResult.Error("Persona sirf ${PERSONAS.joinToString(", ")} ho sakta hai")
            settings.persona = match
            ToolResult.Ok("Persona ab $match hai")
        },

        // ---------- Prompt templates ----------
        SpecTool(
            id = "ai_prompt_template_list", category = A, title = "Prompt templates",
            description = "Available prompt templates ki list: " + PROMPT_TEMPLATES.keys.joinToString(", ")
        ) { _, _ ->
            ToolResult.Ok(PROMPT_TEMPLATES.keys.joinToString(", "))
        },
        SpecTool(
            id = "ai_prompt_template_render", category = A, title = "Prompt banao",
            description = "Template mein text bharkar ready-to-send prompt banao.",
            params = listOf(
                ToolParam("template", "string", PROMPT_TEMPLATES.keys.joinToString(", "), required = true),
                ToolParam("text", "string", "Template mein bharne wala text", required = true)
            )
        ) { args, _ ->
            val name = args.str("template") ?: return@SpecTool ToolResult.Error("template chahiye")
            val tpl = PROMPT_TEMPLATES[name] ?: return@SpecTool ToolResult.Error("Template nahi mila: $name")
            ToolResult.Ok(tpl.replace("{text}", args.str("text") ?: ""))
        },

        // ---------- Text analysis (on-device, no API) ----------
        SpecTool(
            id = "ai_token_estimate", category = A, title = "Token estimate",
            description = "Text mein lagbhag kitne tokens lagenge (approx, 1 token ~ 4 characters).",
            params = listOf(ToolParam("text", "string", "Text", required = true))
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok("Lagbhag ${(t.length + 3) / 4} tokens, ${words(t).size} words, ${t.length} characters")
        },
        SpecTool(
            id = "ai_summarize_text", category = A, title = "Text summary (offline)",
            description = "Lambe text ka chhota extractive summary banao (bina internet ke). Sabse important sentences rakhta hai.",
            params = listOf(
                ToolParam("text", "string", "Text", required = true),
                ToolParam("sentences", "integer", "Kitne sentences (default 3)")
            )
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            val n = (args.int("sentences") ?: 3).coerceIn(1, 10)
            ToolResult.Ok(summarize(t, n))
        },
        SpecTool(
            id = "ai_sentiment", category = A, title = "Sentiment",
            description = "Text positive hai, negative hai ya neutral (English + Hinglish keywords se).",
            params = listOf(ToolParam("text", "string", "Text", required = true))
        ) { args, _ ->
            val w = words(args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye"))
            val pos = w.count { it in POSITIVE }
            val neg = w.count { it in NEGATIVE }
            ToolResult.Ok(
                when {
                    pos > neg -> "Positive (positive words: $pos, negative: $neg)"
                    neg > pos -> "Negative (positive words: $pos, negative: $neg)"
                    else -> "Neutral (positive words: $pos, negative: $neg)"
                }
            )
        },
        SpecTool(
            id = "ai_intent_classify", category = A, title = "Intent guess",
            description = "Ek user message kis kaam ke liye lagta hai: call, message, music, weather, reminder, calendar, github, tasks, device ya other.",
            params = listOf(ToolParam("text", "string", "User message", required = true))
        ) { args, _ ->
            val w = words(args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")).toSet()
            val best = INTENTS.mapValues { (_, kws) -> kws.count { it in w } }.maxByOrNull { it.value }
            ToolResult.Ok(if (best == null || best.value == 0) "other" else best.key)
        },

        // ---------- Context ----------
        SpecTool(
            id = "ai_context_snapshot", category = A, title = "Context snapshot",
            description = "Abhi ka time, date, din ka hissa aur battery level ek saath batao (AI ko context dene ke liye)."
        ) { _, ctx ->
            val now = ZonedDateTime.now()
            val part = when (now.hour) {
                in 5..11 -> "subah"
                in 12..16 -> "dopahar"
                in 17..20 -> "shaam"
                else -> "raat"
            }
            ToolResult.Ok(
                "Time: ${now.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy, HH:mm"))} ($part), " +
                    "Battery: ${batteryPct(ctx.appContext)}%"
            )
        },

        // ---------- Command aliases ----------
        SpecTool(
            id = "ai_alias_add", category = A, title = "Alias banao",
            description = "Ek chhota voice command banao jo kisi lambi command ko chalaye. Jaise 'ghar' = 'battery status batao'.",
            params = listOf(
                ToolParam("alias", "string", "Chhota naam", required = true),
                ToolParam("command", "string", "Jo command chalana hai", required = true)
            )
        ) { args, ctx ->
            val alias = args.str("alias")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return@SpecTool ToolResult.Error("alias chahiye")
            val cmd = args.str("command")?.trim()?.takeIf { it.isNotEmpty() } ?: return@SpecTool ToolResult.Error("command chahiye")
            val store = AiStateStore(ctx.appContext)
            store.saveAliases(store.aliases() + (alias to cmd))
            ToolResult.Ok("Alias '$alias' save kar diya")
        },
        SpecTool(
            id = "ai_alias_list", category = A, title = "Alias list",
            description = "Saare saved aliases dikhao."
        ) { _, ctx ->
            val map = AiStateStore(ctx.appContext).aliases()
            ToolResult.Ok(if (map.isEmpty()) "Koi alias nahi hai." else map.entries.joinToString("\n") { "${it.key} -> ${it.value}" })
        },
        SpecTool(
            id = "ai_alias_remove", category = A, title = "Alias hatao",
            description = "Ek alias hatao.",
            params = listOf(ToolParam("alias", "string", "Alias naam", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val alias = args.str("alias")?.trim()?.lowercase() ?: return@SpecTool ToolResult.Error("alias chahiye")
            val store = AiStateStore(ctx.appContext)
            val current = store.aliases()
            if (alias !in current) return@SpecTool ToolResult.Error("'$alias' naam ka alias nahi hai")
            store.saveAliases(current - alias)
            ToolResult.Ok("Alias '$alias' hata diya")
        },

        // ---------- Todo ----------
        SpecTool(
            id = "ai_todo_add", category = A, title = "Todo add",
            description = "Todo list mein naya kaam jodo.",
            params = listOf(ToolParam("text", "string", "Kaam", required = true))
        ) { args, ctx ->
            val t = args.str("text")?.trim()?.takeIf { it.isNotEmpty() } ?: return@SpecTool ToolResult.Error("text chahiye")
            val store = AiStateStore(ctx.appContext)
            val list = store.todos()
            val id = (list.maxOfOrNull { it.id } ?: 0L) + 1
            store.saveTodos(list + AiStateStore.Todo(id, t, false))
            ToolResult.Ok("Todo jod diya (#$id): $t")
        },
        SpecTool(
            id = "ai_todo_list", category = A, title = "Todo list",
            description = "Todo list dikhao. show: all ya pending.",
            params = listOf(ToolParam("show", "string", "all ya pending (default pending)"))
        ) { args, ctx ->
            val all = args.str("show") == "all"
            val list = AiStateStore(ctx.appContext).todos().filter { all || !it.done }
            ToolResult.Ok(
                if (list.isEmpty()) "Todo list khaali hai."
                else list.joinToString("\n") { "#${it.id} [${if (it.done) "x" else " "}] ${it.text}" }
            )
        },
        SpecTool(
            id = "ai_todo_done", category = A, title = "Todo complete",
            description = "Ek todo ko complete mark karo (id se).",
            params = listOf(ToolParam("id", "integer", "Todo id", required = true))
        ) { args, ctx ->
            val id = args.long("id") ?: return@SpecTool ToolResult.Error("id chahiye")
            val store = AiStateStore(ctx.appContext)
            val list = store.todos()
            if (list.none { it.id == id }) return@SpecTool ToolResult.Error("#$id nahi mila")
            store.saveTodos(list.map { if (it.id == id) it.copy(done = true) else it })
            ToolResult.Ok("#$id complete ho gaya")
        },

        // ---------- Backup & restore ----------
        SpecTool(
            id = "ai_backup_create", category = A, title = "Backup banao",
            description = "Yaadein, todos, aliases aur persona ka backup JSON text banao."
        ) { _, ctx ->
            val state = AiStateStore(ctx.appContext)
            val root = JSONObject()
                .put("persona", settings.persona)
                .put("memories", JSONArray(MemoryStore(ctx.appContext).loadMemories().map {
                    JSONObject().put("text", it.text).put("kind", it.kind).put("ts", it.timestamp)
                }))
                .put("todos", JSONArray(state.todos().map {
                    JSONObject().put("text", it.text).put("done", it.done)
                }))
                .put("aliases", JSONObject(state.aliases()))
            ToolResult.Ok(root.toString())
        },
        SpecTool(
            id = "ai_backup_restore", category = A, title = "Backup restore",
            description = "ai_backup_create wala JSON text paste karke restore karo. Todos aur aliases replace ho jaayenge, yaadein jud jaayengi.",
            params = listOf(ToolParam("json", "string", "Backup JSON", required = true)),
            risk = RiskLevel.HIGH
        ) { args, ctx ->
            val root = JSONObject(args.str("json") ?: return@SpecTool ToolResult.Error("json chahiye"))
            val mem = MemoryStore(ctx.appContext)
            val arr = root.optJSONArray("memories") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                mem.addMemory(o.getString("text"), o.optString("kind", "fact"))
            }
            val state = AiStateStore(ctx.appContext)
            val todoArr = root.optJSONArray("todos") ?: JSONArray()
            state.saveTodos((0 until todoArr.length()).map {
                val o = todoArr.getJSONObject(it)
                AiStateStore.Todo((it + 1).toLong(), o.getString("text"), o.optBoolean("done", false))
            })
            root.optJSONObject("aliases")?.let { o ->
                state.saveAliases(o.keys().asSequence().associateWith { o.getString(it) })
            }
            root.optString("persona").takeIf { it in PERSONAS }?.let { settings.persona = it }
            ToolResult.Ok("Restore ho gaya: ${arr.length()} yaadein, ${todoArr.length()} todos")
        }
    )
}
