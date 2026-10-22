package com.kozaz.decoder

enum class Mode(val label: String) {
    AUTO("Auto"),
    EN_TO_FA("EN → FA"),
    FA_TO_EN("FA → EN");

    fun next(): Mode = values()[(ordinal + 1) % values().size]
}

object Decoder {
    // Standard Windows Persian layout. Edit here if a character is wrong.
    private const val ENG = "qwertyuiop[]asdfghjkl;'zxcvbnm,./\\"
    private const val FA = "ضصثقفغعهخحجچشسیبلاتنمکگظطزرذدئو/.پ"

    // Shift layer (uppercase English <-> diacritics and extra letters)
    private const val ENG_S = "QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM?"
    private const val FA_S =
        "\u0652\u064C\u064D\u064B\u064F\u0650\u064E\u0651][}{" +
        "\u0624\u0626\u064A\u0625\u0623\u0622\u0629\u00BB\u00AB:\u061B" +
        "\u0643\u0653\u0698\u0670\u200C\u0654\u0621\u061F"

    private val en2fa = HashMap<Char, Char>()
    private val fa2en = HashMap<Char, Char>()

    private fun add(en: Char, fa: Char) {
        en2fa[en] = fa
        if (!fa2en.containsKey(fa)) fa2en[fa] = en
    }

    init {
        ENG.forEachIndexed { i, c -> add(c, FA[i]) }
        ENG_S.forEachIndexed { i, c -> add(c, FA_S[i]) }
        for (d in 0..9) add('0' + d, '\u06F0' + d)   // Persian digits
        fa2en['\u060C'] = ','                         // Persian comma from phone keyboards
    }

    private fun hasPersian(s: String) = s.any { it in '\u0600'..'\u06FF' }

    private fun apply(s: String, map: Map<Char, Char>): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(map[c] ?: c)
        return sb.toString()
    }

    fun convert(text: String, mode: Mode): String = when (mode) {
        Mode.EN_TO_FA -> apply(text, en2fa)
        Mode.FA_TO_EN -> apply(text, fa2en)
        Mode.AUTO -> Regex("\\s+|\\S+").replace(text) { m ->
            val w = m.value
            if (w.isBlank()) w else apply(w, if (hasPersian(w)) fa2en else en2fa)
        }
    }
}
