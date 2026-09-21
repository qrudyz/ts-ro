package com.rudyunguru.trucks.core

import java.util.Locale

/** Romanian/European presentation helpers: `12 480 €`, `Ziua 4, 13:52`, `2h 15m`, `1 245 km`. */
object Format {

    private val roLocale: Locale = Locale("ro", "RO")

    fun money(value: Double, withSymbol: Boolean = true): String = money(value.toLong(), withSymbol)

    fun money(value: Long, withSymbol: Boolean = true): String {
        val neg = value < 0
        val abs = if (neg) -value else value
        val grouped = group(abs)
        return buildString {
            if (neg) append('-')
            append(grouped)
            if (withSymbol) append(" €")
        }
    }

    fun moneyShort(value: Double, withSymbol: Boolean = true): String {
        val abs = if (value < 0) -value else value
        val text = when {
            abs >= 1_000_000 -> String.format(roLocale, "%.1fM", abs / 1_000_000.0)
            abs >= 10_000 -> String.format(roLocale, "%.0fk", abs / 1_000.0)
            abs >= 1_000 -> String.format(roLocale, "%.1fk", abs / 1_000.0)
            else -> abs.toInt().toString()
        }
        val prefix = if (value < 0) "-" else ""
        return if (withSymbol) "$prefix$text €" else "$prefix$text"
    }

    fun group(value: Long): String {
        val digits = value.toString()
        val sb = StringBuilder()
        for ((i, c) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return sb.toString()
    }

    fun number(value: Double, decimals: Int = 0): String {
        val fixed = String.format(roLocale, "%.${decimals}f", value)
        val parts = fixed.split('.')
        val int = parts[0]
        val sign = if (int.startsWith("-")) "-" else ""
        val grouped = group(int.removePrefix("-").toLongOrNull() ?: 0L)
        return if (parts.size > 1) "$sign$grouped,${parts[1]}" else "$sign$grouped"
    }

    fun km(value: Double): String = "${number(value, if (value < 100) 1 else 0)} km"

    fun litres(value: Double): String = "${number(value, 0)} L"

    fun percent(value: Double): String = "${number(value, 0)}%"

    /** Game clock: minutes since day 0 -> `Ziua 3, 07:45`. */
    fun gameDateTime(totalMinutes: Long): String {
        val day = totalMinutes / 1440L + 1
        return "Ziua $day, ${clock(totalMinutes)}"
    }

    /** Minutes since day 0 -> `07:45`. */
    fun clock(totalMinutes: Long): String {
        val mins = ((totalMinutes % 1440L) + 1440L) % 1440L
        return String.format(roLocale, "%02d:%02d", mins / 60L, mins % 60L)
    }

    /** Minutes -> `2h 15m` / `45m`. */
    fun duration(minutes: Long): String {
        val total = if (minutes < 0) 0L else minutes
        val h = total / 60
        val m = total % 60
        return if (h == 0L) "${m}m" else "${h}h ${String.format(roLocale, "%02d", m)}m"
    }

    /** Seconds -> `01:23:45` or `12:45`. */
    fun countdown(seconds: Long): String {
        val s = if (seconds < 0) 0L else seconds
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(roLocale, "%02d:%02d:%02d", h, m, sec)
        else String.format(roLocale, "%02d:%02d", m, sec)
    }

    fun speed(kmh: Double): String = "${number(kmh, 0)}"

    fun temperature(celsius: Double): String = "${number(celsius, 1)}°C"

    fun relativeDay(value: Int): String = when (value) {
        0 -> "astăzi"
        1 -> "ieri"
        else -> "$value zile"
    }

    fun weight(tons: Double): String = "${number(tons, 1)} t"

    fun plate(seed: Int): String {
        val counties = listOf("B", "PH", "BV", "SB", "CJ", "BC", "IS", "CT", "DJ", "TM", "MS", "AR", "BH", "GL", "SV", "OT")
        val letters = "ABCDEFGHJKLMNPRSTUVWXYZ"
        val rng = Rng(seed)
        val county = counties[rng.int(0, counties.size - 1)]
        val n1 = String.format(roLocale, "%02d", rng.int(1, 99))
        val l1 = letters[rng.int(0, letters.length - 1)]
        val l2 = letters[rng.int(0, letters.length - 1)]
        val l3 = letters[rng.int(0, letters.length - 1)]
        val n2 = String.format(roLocale, "%03d", rng.int(1, 999))
        return "$county$n1 $l1$l2$l3"
    }
}
