package com.ultron.android.tools.utility

import com.ultron.android.core.tools.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.pow

private val U = ToolCategory.UTILITY

// ---------- Calculator (recursive descent: + - * / % ^ ( )) ----------

private class Expr(private val s: String) {
    private var i = 0

    fun parse(): Double {
        val v = expr()
        skip()
        require(i == s.length) { "Expression samajh nahi aaya" }
        return v
    }

    private fun skip() { while (i < s.length && s[i].isWhitespace()) i++ }

    private fun expr(): Double {
        var v = term()
        while (true) {
            skip()
            if (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val r = term()
                v = if (op == '+') v + r else v - r
            } else return v
        }
    }

    private fun term(): Double {
        var v = factor()
        while (true) {
            skip()
            if (i < s.length && (s[i] == '*' || s[i] == '/' || s[i] == '%')) {
                val op = s[i++]
                val r = factor()
                v = when (op) { '*' -> v * r; '/' -> v / r; else -> v % r }
            } else return v
        }
    }

    private fun factor(): Double {
        skip()
        if (i < s.length && s[i] == '-') { i++; return -factor() }
        val base = atom()
        skip()
        if (i < s.length && s[i] == '^') { i++; return base.pow(factor()) }
        return base
    }

    private fun atom(): Double {
        skip()
        if (i < s.length && s[i] == '(') {
            i++
            val v = expr()
            skip()
            require(i < s.length && s[i] == ')') { "')' chahiye" }
            i++
            return v
        }
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        require(i > start) { "Number chahiye" }
        return s.substring(start, i).toDouble()
    }
}

private fun fmt(d: Double): String =
    if (d == d.toLong().toDouble() && kotlin.math.abs(d) < 1e15) d.toLong().toString()
    else "%.6f".format(d).trimEnd('0').trimEnd('.')

// ---------- Unit tables ----------

private val LENGTH_M = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "mi" to 1609.344, "ft" to 0.3048, "in" to 0.0254, "yd" to 0.9144)
private val WEIGHT_KG = mapOf("kg" to 1.0, "g" to 0.001, "mg" to 0.000001, "lb" to 0.45359237, "oz" to 0.028349523125)

private val MORSE = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
    'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
    'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
)

// (mahina, din, sign jo us date se shuru hota hai). Sorted by date.
private val ZODIAC = listOf(
    Triple(1, 20, "Aquarius (Kumbh)"), Triple(2, 19, "Pisces (Meen)"), Triple(3, 21, "Aries (Mesh)"),
    Triple(4, 20, "Taurus (Vrishabh)"), Triple(5, 21, "Gemini (Mithun)"), Triple(6, 21, "Cancer (Kark)"),
    Triple(7, 23, "Leo (Simha)"), Triple(8, 23, "Virgo (Kanya)"), Triple(9, 23, "Libra (Tula)"),
    Triple(10, 23, "Scorpio (Vrishchik)"), Triple(11, 22, "Sagittarius (Dhanu)"), Triple(12, 22, "Capricorn (Makar)")
)

private fun toRoman(n: Int): String {
    require(n in 1..3999) { "Roman sirf 1 se 3999 tak" }
    val vals = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var rem = n
    val sb = StringBuilder()
    for ((v, s) in vals) while (rem >= v) { sb.append(s); rem -= v }
    return sb.toString()
}

private fun fromRoman(s: String): Int {
    val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    val u = s.uppercase()
    require(u.isNotEmpty() && u.all { it in map }) { "Valid roman numeral nahi hai" }
    var total = 0
    for (i in u.indices) {
        val cur = map.getValue(u[i])
        val next = if (i + 1 < u.length) map.getValue(u[i + 1]) else 0
        total += if (cur < next) -cur else cur
    }
    return total
}

private fun passwordScore(p: String): Int {
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any { it.isLowerCase() } && p.any { it.isUpperCase() }) s++
    if (p.any { it.isDigit() }) s++
    if (p.any { !it.isLetterOrDigit() }) s++
    return s
}

// ---------- Tools ----------

class UtilityToolModule : ToolModule {
    override val tools: List<Tool> = listOf(
        SpecTool(
            id = "util_calculator", category = U, title = "Calculator",
            description = "Math expression solve karo, jaise 12*(3+4)/2 ya 2^10.",
            params = listOf(ToolParam("expression", "string", "Math expression", required = true))
        ) { args, _ ->
            val e = args.str("expression") ?: return@SpecTool ToolResult.Error("expression chahiye")
            val v = Expr(e.replace(" ", "")).parse()
            if (v.isNaN() || v.isInfinite()) ToolResult.Error("Result define nahi hai (shayad zero se divide)")
            else ToolResult.Ok("$e = ${fmt(v)}")
        },
        SpecTool(
            id = "util_unit_convert", category = U, title = "Unit converter",
            description = "Length (m, km, cm, mm, mi, ft, in, yd), weight (kg, g, mg, lb, oz) ya temperature (C, F, K) convert karo.",
            params = listOf(
                ToolParam("value", "number", "Value", required = true),
                ToolParam("from", "string", "Unit jisse convert karna hai", required = true),
                ToolParam("to", "string", "Unit jisme chahiye", required = true)
            )
        ) { args, _ ->
            val v = args.number("value") ?: return@SpecTool ToolResult.Error("value chahiye")
            val from = args.str("from")?.lowercase() ?: return@SpecTool ToolResult.Error("from chahiye")
            val to = args.str("to")?.lowercase() ?: return@SpecTool ToolResult.Error("to chahiye")
            val temps = setOf("c", "f", "k")
            val out = when {
                from in temps && to in temps -> {
                    val c = when (from) { "c" -> v; "f" -> (v - 32) * 5 / 9; else -> v - 273.15 }
                    when (to) { "c" -> c; "f" -> c * 9 / 5 + 32; else -> c + 273.15 }
                }
                from in LENGTH_M && to in LENGTH_M -> v * LENGTH_M.getValue(from) / LENGTH_M.getValue(to)
                from in WEIGHT_KG && to in WEIGHT_KG -> v * WEIGHT_KG.getValue(from) / WEIGHT_KG.getValue(to)
                else -> return@SpecTool ToolResult.Error("$from aur $to ek hi type ke units nahi hain")
            }
            ToolResult.Ok("${fmt(v)} $from = ${fmt(out)} $to")
        },
        SpecTool(
            id = "util_bmi", category = U, title = "BMI calculator",
            description = "Weight (kg) aur height (cm) se BMI aur category batao.",
            params = listOf(
                ToolParam("weight_kg", "number", "Weight kg", required = true),
                ToolParam("height_cm", "number", "Height cm", required = true)
            )
        ) { args, _ ->
            val w = args.number("weight_kg") ?: return@SpecTool ToolResult.Error("weight_kg chahiye")
            val h = args.number("height_cm") ?: return@SpecTool ToolResult.Error("height_cm chahiye")
            if (h <= 0 || w <= 0) return@SpecTool ToolResult.Error("Values positive honi chahiye")
            val bmi = w / ((h / 100) * (h / 100))
            val cat = when {
                bmi < 18.5 -> "underweight"
                bmi < 25 -> "normal"
                bmi < 30 -> "overweight"
                else -> "obese"
            }
            ToolResult.Ok("BMI ${"%.1f".format(bmi)} hai, category: $cat")
        },
        SpecTool(
            id = "util_tip", category = U, title = "Tip calculator",
            description = "Bill par tip, total aur per-person amount nikalo.",
            params = listOf(
                ToolParam("bill", "number", "Bill amount", required = true),
                ToolParam("percent", "number", "Tip percent (default 10)"),
                ToolParam("people", "integer", "Kitne log (default 1)")
            )
        ) { args, _ ->
            val bill = args.number("bill") ?: return@SpecTool ToolResult.Error("bill chahiye")
            val pct = args.number("percent") ?: 10.0
            val people = (args.int("people") ?: 1).coerceAtLeast(1)
            val tip = bill * pct / 100
            val total = bill + tip
            ToolResult.Ok("Tip ${fmt(tip)}, total ${fmt(total)}, per person ${"%.2f".format(total / people)}")
        },
        SpecTool(
            id = "util_emi", category = U, title = "Loan EMI calculator",
            description = "Loan amount, annual interest rate aur months se monthly EMI nikalo.",
            params = listOf(
                ToolParam("principal", "number", "Loan amount", required = true),
                ToolParam("annual_rate_percent", "number", "Saalana interest % ", required = true),
                ToolParam("months", "integer", "Kitne mahine", required = true)
            )
        ) { args, _ ->
            val p = args.number("principal") ?: return@SpecTool ToolResult.Error("principal chahiye")
            val rate = args.number("annual_rate_percent") ?: return@SpecTool ToolResult.Error("annual_rate_percent chahiye")
            val n = args.int("months") ?: return@SpecTool ToolResult.Error("months chahiye")
            if (n <= 0) return@SpecTool ToolResult.Error("months 1 se zyada hone chahiye")
            val emi = if (rate == 0.0) p / n else {
                val r = rate / 12 / 100
                p * r * (1 + r).pow(n.toDouble()) / ((1 + r).pow(n.toDouble()) - 1)
            }
            ToolResult.Ok("Monthly EMI ${"%.2f".format(emi)}, total payment ${"%.2f".format(emi * n)}")
        },
        SpecTool(
            id = "util_date_diff", category = U, title = "Date difference",
            description = "Do dates ke beech kitne din/hafte hain (format yyyy-MM-dd).",
            params = listOf(
                ToolParam("from", "string", "Start date yyyy-MM-dd", required = true),
                ToolParam("to", "string", "End date yyyy-MM-dd (default aaj)")
            )
        ) { args, _ ->
            val from = LocalDate.parse(args.str("from") ?: return@SpecTool ToolResult.Error("from chahiye"))
            val to = args.str("to")?.let { LocalDate.parse(it) } ?: LocalDate.now()
            val days = ChronoUnit.DAYS.between(from, to)
            ToolResult.Ok("$days din (lagbhag ${days / 7} hafte) ka farak hai")
        },
        SpecTool(
            id = "util_countdown", category = U, title = "Countdown",
            description = "Kisi date tak kitne din bache hain (yyyy-MM-dd).",
            params = listOf(ToolParam("date", "string", "Target date yyyy-MM-dd", required = true))
        ) { args, _ ->
            val target = LocalDate.parse(args.str("date") ?: return@SpecTool ToolResult.Error("date chahiye"))
            val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
            ToolResult.Ok(
                when {
                    days > 0 -> "$days din baaki hain"
                    days == 0L -> "Aaj hi hai"
                    else -> "${-days} din pehle ho chuka hai"
                }
            )
        },
        SpecTool(
            id = "util_roman", category = U, title = "Roman numeral converter",
            description = "Number ko roman mein ya roman ko number mein convert karo.",
            params = listOf(ToolParam("value", "string", "Number (jaise 2024) ya roman (jaise MMXXIV)", required = true))
        ) { args, _ ->
            val v = args.str("value")?.trim() ?: return@SpecTool ToolResult.Error("value chahiye")
            val out = if (v.all { it.isDigit() }) toRoman(v.toInt()) else fromRoman(v).toString()
            ToolResult.Ok("$v = $out")
        },
        SpecTool(
            id = "util_number_base", category = U, title = "Number base converter",
            description = "Binary, octal, decimal ya hex number ko sab formats mein dikhao.",
            params = listOf(
                ToolParam("value", "string", "Number, jaise 1011 ya FF", required = true),
                ToolParam("from_base", "integer", "2, 8, 10 ya 16", required = true)
            )
        ) { args, _ ->
            val raw = args.str("value")?.trim() ?: return@SpecTool ToolResult.Error("value chahiye")
            val base = args.int("from_base") ?: return@SpecTool ToolResult.Error("from_base chahiye")
            if (base !in listOf(2, 8, 10, 16)) return@SpecTool ToolResult.Error("from_base 2, 8, 10 ya 16 hona chahiye")
            val n = raw.toLongOrNull(base) ?: return@SpecTool ToolResult.Error("$raw base $base mein valid nahi hai")
            ToolResult.Ok("Decimal $n, Binary ${n.toString(2)}, Octal ${n.toString(8)}, Hex ${n.toString(16).uppercase()}")
        },
        SpecTool(
            id = "util_random", category = U, title = "Random number",
            description = "Do numbers ke beech random integer do.",
            params = listOf(
                ToolParam("min", "integer", "Minimum", required = true),
                ToolParam("max", "integer", "Maximum", required = true)
            )
        ) { args, _ ->
            val min = args.long("min") ?: return@SpecTool ToolResult.Error("min chahiye")
            val max = args.long("max") ?: return@SpecTool ToolResult.Error("max chahiye")
            if (min > max) return@SpecTool ToolResult.Error("min, max se chhota hona chahiye")
            ToolResult.Ok("Random number: ${(min..max).random()}")
        },
        SpecTool(
            id = "util_password_strength", category = U, title = "Password strength",
            description = "Password kitna strong hai check karo. Password kabhi wapas nahi dohraya jaata.",
            params = listOf(ToolParam("password", "string", "Check karne wala password", required = true)),
            risk = RiskLevel.LOW
        ) { args, _ ->
            val p = args.str("password") ?: return@SpecTool ToolResult.Error("password chahiye")
            val s = passwordScore(p)
            val label = when { s <= 2 -> "weak"; s <= 3 -> "medium"; else -> "strong" }
            ToolResult.Ok("Strength: $label ($s/5). Length ${p.length}.")
        },
        SpecTool(
            id = "util_morse", category = U, title = "Morse code",
            description = "Text ko morse code mein encode karo ya morse ko text mein decode karo.",
            params = listOf(
                ToolParam("text", "string", "Text ya morse", required = true),
                ToolParam("mode", "string", "encode ya decode", required = true)
            )
        ) { args, _ ->
            val text = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            when (args.str("mode")) {
                "encode" -> ToolResult.Ok(
                    text.uppercase().split(" ").joinToString(" / ") { w ->
                        w.mapNotNull { MORSE[it] }.joinToString(" ")
                    }
                )
                "decode" -> {
                    val rev = MORSE.entries.associate { it.value to it.key }
                    ToolResult.Ok(
                        text.split(" / ").joinToString(" ") { w ->
                            w.split(" ").mapNotNull { rev[it] }.joinToString("")
                        }
                    )
                }
                else -> ToolResult.Error("mode encode ya decode hona chahiye")
            }
        },
        SpecTool(
            id = "util_caesar_cipher", category = U, title = "Caesar cipher",
            description = "Text ko Caesar shift cipher se encrypt ya decrypt karo (sirf letters par).",
            params = listOf(
                ToolParam("text", "string", "Text", required = true),
                ToolParam("shift", "integer", "Shift number (default 3)"),
                ToolParam("mode", "string", "encrypt ya decrypt (default encrypt)")
            )
        ) { args, _ ->
            val text = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            var shift = (args.int("shift") ?: 3) % 26
            if (args.str("mode") == "decrypt") shift = -shift
            val out = text.map { c ->
                when (c) {
                    in 'a'..'z' -> 'a' + ((c - 'a' + shift + 26) % 26)
                    in 'A'..'Z' -> 'A' + ((c - 'A' + shift + 26) % 26)
                    else -> c
                }
            }.joinToString("")
            ToolResult.Ok(out)
        },
        SpecTool(
            id = "util_text_transform", category = U, title = "Text tools",
            description = "Text ko upper/lower/reverse/title case mein badlo, ya word aur character count nikalo.",
            params = listOf(
                ToolParam("text", "string", "Text", required = true),
                ToolParam("op", "string", "upper, lower, reverse, title, count", required = true)
            )
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok(
                when (args.str("op")) {
                    "upper" -> t.uppercase()
                    "lower" -> t.lowercase()
                    "reverse" -> t.reversed()
                    "title" -> t.split(" ").joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
                    "count" -> "Words: ${t.trim().split(Regex("\\s+")).count { it.isNotEmpty() }}, Characters: ${t.length}"
                    else -> return@SpecTool ToolResult.Error("op upper, lower, reverse, title ya count hona chahiye")
                }
            )
        },
        SpecTool(
            id = "util_color_convert", category = U, title = "Color converter",
            description = "Hex color (#FF6A00) ko RGB mein, ya RGB (255,106,0) ko hex mein convert karo.",
            params = listOf(ToolParam("color", "string", "#RRGGBB ya r,g,b", required = true))
        ) { args, _ ->
            val c = args.str("color")?.trim() ?: return@SpecTool ToolResult.Error("color chahiye")
            if (c.startsWith("#") || c.length == 6) {
                val hex = c.removePrefix("#")
                val n = hex.toIntOrNull(16) ?: return@SpecTool ToolResult.Error("Hex valid nahi hai")
                if (hex.length != 6) return@SpecTool ToolResult.Error("Hex 6 characters ka hona chahiye")
                ToolResult.Ok("R ${n shr 16 and 255}, G ${n shr 8 and 255}, B ${n and 255}")
            } else {
                val parts = c.split(",").map { it.trim().toIntOrNull() }
                if (parts.size != 3 || parts.any { it == null || it !in 0..255 }) {
                    ToolResult.Error("RGB 0-255 ke teen numbers hone chahiye")
                } else {
                    val (r, g, b) = parts.map { it!! }
                    ToolResult.Ok("#%02X%02X%02X".format(r, g, b))
                }
            }
        },
        SpecTool(
            id = "util_percent", category = U, title = "Percent calculator",
            description = "Kisi value ka percent ya ek number doosre ka kitne percent hai.",
            params = listOf(
                ToolParam("value", "number", "Value", required = true),
                ToolParam("total", "number", "Total", required = true)
            )
        ) { args, _ ->
            val v = args.number("value") ?: return@SpecTool ToolResult.Error("value chahiye")
            val t = args.number("total") ?: return@SpecTool ToolResult.Error("total chahiye")
            if (t == 0.0) return@SpecTool ToolResult.Error("total zero nahi ho sakta")
            ToolResult.Ok("${fmt(v)} is ${"%.2f".format(v / t * 100)}% of ${fmt(t)}")
        },
        SpecTool(
            id = "util_water_intake", category = U, title = "Water intake",
            description = "Weight ke hisaab se roz ka lagbhag paani (liter) batao.",
            params = listOf(ToolParam("weight_kg", "number", "Weight kg", required = true))
        ) { args, _ ->
            val w = args.number("weight_kg") ?: return@SpecTool ToolResult.Error("weight_kg chahiye")
            ToolResult.Ok("Roz lagbhag ${"%.1f".format(w * 35 / 1000)} liter paani peeyein (approx 35 ml per kg).")
        },
        SpecTool(
            id = "util_zodiac", category = U, title = "Zodiac sign",
            description = "Din aur mahine se zodiac sign batao.",
            params = listOf(
                ToolParam("day", "integer", "Din", required = true),
                ToolParam("month", "integer", "Mahina 1-12", required = true)
            )
        ) { args, _ ->
            val d = args.int("day") ?: return@SpecTool ToolResult.Error("day chahiye")
            val m = args.int("month") ?: return@SpecTool ToolResult.Error("month chahiye")
            if (m !in 1..12 || d !in 1..31) return@SpecTool ToolResult.Error("Valid din aur mahina do")
            // Last sign jiska start din (m,d) se pehle ya barabar ho. Jan 1-19 = Capricorn (fallback).
            val sign = ZODIAC.lastOrNull { (mon, cut, _) -> mon * 100 + cut <= m * 100 + d }?.third
                ?: "Capricorn (Makar)"
            ToolResult.Ok(sign)
        },
        SpecTool(
            id = "util_world_time", category = U, title = "World clock",
            description = "Kisi timezone mein abhi ka time (jaise Asia/Kolkata, America/New_York, Europe/London).",
            params = listOf(ToolParam("zone", "string", "Timezone ID", required = true))
        ) { args, _ ->
            val zone = args.str("zone") ?: return@SpecTool ToolResult.Error("zone chahiye")
            val z = try { ZoneId.of(zone) } catch (e: Exception) {
                return@SpecTool ToolResult.Error("$zone valid timezone nahi hai")
            }
            val now = ZonedDateTime.now(z)
            ToolResult.Ok("$zone: ${now.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy, HH:mm"))}")
        },
        SpecTool(
            id = "util_time_now", category = U, title = "Current date & time",
            description = "Phone ke time zone mein abhi ka date aur time batao.",
            params = emptyList()
        ) { _, _ ->
            ToolResult.Ok(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy, HH:mm")))
        }
    )
}
