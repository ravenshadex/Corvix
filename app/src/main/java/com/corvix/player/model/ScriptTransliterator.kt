package com.corvix.player.model

/**
 * Offline phonetic transliteration from Tamil, Hindi (Devanagari), Malayalam, and Telugu
 * into readable Romanized / English characters so users can read any lyrics smoothly.
 */
object ScriptTransliterator {

    fun hasIndicNonLatin(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code in 0x0900..0x0D7F) return true
        }
        return false
    }

    fun transliterateToEnglish(input: String): String {
        if (!hasIndicNonLatin(input)) return input
        val out = StringBuilder(input.length + 16)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '\u093C') {
                i += 1
                continue
            }
            val cons = consonantMap[ch]
            if (cons != null) {
                var nextIdx = i + 1
                if (input.getOrNull(nextIdx) == '\u093C') nextIdx += 1
                val next = input.getOrNull(nextIdx)
                val virama = next != null && viramas.contains(next)
                val matra = next?.let { matraMap[it] }
                when {
                    virama -> {
                        out.append(cons)
                        i = nextIdx + 1
                    }
                    matra != null -> {
                        out.append(cons).append(matra)
                        i = nextIdx + 1
                    }
                    else -> {
                        out.append(cons).append('a')
                        i = nextIdx
                    }
                }
            } else {
                val mapped = standaloneMap[ch]
                if (mapped != null) {
                    out.append(mapped)
                } else {
                    out.append(ch)
                }
                i += 1
            }
        }
        return out.toString()
    }

    private val viramas = setOf('\u094D', '\u0BCD', '\u0D4D', '\u0C4D')

    private val standaloneMap: Map<Char, String> = mapOf(
        // Devanagari vowels & signs
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
        'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au", 'ऋ' to "ri",
        'ं' to "n", 'ँ' to "n", 'ः' to "h",
        // Tamil vowels & signs
        'அ' to "a", 'ஆ' to "aa", 'இ' to "i", 'ஈ' to "ee", 'உ' to "u", 'ஊ' to "oo",
        'எ' to "e", 'ஏ' to "ae", 'ஐ' to "ai", 'ஒ' to "o", 'ஓ' to "oo", 'ஔ' to "au", 'ஃ' to "h",
        // Malayalam vowels & signs
        'അ' to "a", 'ആ' to "aa", 'ഇ' to "i", 'ഈ' to "ee", 'ഉ' to "u", 'ഊ' to "oo",
        'എ' to "e", 'ഏ' to "ae", 'ഐ' to "ai", 'ഒ' to "o", 'ഓ' to "oo", 'ഔ' to "au",
        'ം' to "m", 'ഃ' to "h", 'ൻ' to "n", 'ൽ' to "l", 'ൾ' to "l", 'ർ' to "r", 'ൺ' to "n",
        // Telugu vowels & signs
        'అ' to "a", 'ఆ' to "aa", 'ఇ' to "i", 'ఈ' to "ee", 'ఉ' to "u", 'ఊ' to "oo",
        'ఎ' to "e", 'ఏ' to "ae", 'ఐ' to "ai", 'ఒ' to "o", 'ఓ' to "oo", 'ఔ' to "au",
        'ం' to "m", 'ః' to "h"
    )

    private val matraMap: Map<Char, String> = mapOf(
        // Devanagari
        'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
        'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ृ' to "ri",
        // Tamil
        'ா' to "aa", 'ி' to "i", 'ீ' to "ee", 'ு' to "u", 'ூ' to "oo",
        'ெ' to "e", 'ே' to "ae", 'ை' to "ai", 'ொ' to "o", 'ோ' to "oo", 'ௌ' to "au",
        // Malayalam
        'ാ' to "aa", 'ി' to "i", 'ീ' to "ee", 'ു' to "u", 'ൂ' to "oo",
        'െ' to "e", 'േ' to "ae", 'ൈ' to "ai", 'ൊ' to "o", 'ോ' to "oo", 'ൗ' to "au",
        // Telugu
        'ా' to "aa", 'ి' to "i", 'ీ' to "ee", 'ు' to "u", 'ూ' to "oo",
        'ె' to "e", 'ే' to "ae", 'ై' to "ai", 'ొ' to "o", 'ో' to "oo", 'ౌ' to "au"
    )

    private val consonantMap: Map<Char, String> = mapOf(
        // Devanagari
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "ny",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h", 'ळ' to "l",
        '\u0958' to "q", '\u0959' to "kh", '\u095A' to "gh", '\u095B' to "z", '\u095E' to "f",
        // Tamil
        'க' to "k", 'ங' to "ng", 'ச' to "s", 'ஞ' to "ny", 'ட' to "d", 'ண' to "n",
        'த' to "th", 'ந' to "n", 'ன' to "n", 'ப' to "p", 'ம' to "m", 'ய' to "y",
        'ர' to "r", 'ற' to "r", 'ல' to "l", 'ள' to "l", 'ழ' to "zh", 'வ' to "v",
        'ஜ' to "j", 'ஷ' to "sh", 'ஸ' to "s", 'ஹ' to "h",
        // Malayalam
        'ക' to "k", 'ഖ' to "kh", 'ഗ' to "g", 'ഘ' to "gh", 'ങ' to "ng",
        'ച' to "ch", 'ഛ' to "chh", 'ജ' to "j", 'ഝ' to "jh", 'ഞ' to "ny",
        'ട' to "t", 'ഠ' to "th", 'ഡ' to "d", 'ഢ' to "dh", 'ണ' to "n",
        'ത' to "th", 'ഥ' to "th", 'ദ' to "d", 'ധ' to "dh", 'ന' to "n",
        'പ' to "p", 'ഫ' to "ph", 'ബ' to "b", 'ഭ' to "bh", 'മ' to "m",
        'യ' to "y", 'ര' to "r", 'റ' to "r", 'ല' to "l", 'ള' to "l", 'ഴ' to "zh", 'വ' to "v",
        'ശ' to "sh", 'ഷ' to "sh", 'സ' to "s", 'ഹ' to "h",
        // Telugu
        'క' to "k", 'ఖ' to "kh", 'గ' to "g", 'ఘ' to "gh", 'చ' to "ch", 'జ' to "j",
        'ట' to "t", 'డ' to "d", 'ణ' to "n", 'త' to "th", 'ద' to "d", 'ధ' to "dh",
        'న' to "n", 'ప' to "p", 'ఫ' to "ph", 'బ' to "b", 'భ' to "bh", 'మ' to "m",
        'య' to "y", 'ర' to "r", 'ల' to "l", 'ళ' to "l", 'వ' to "v",
        'శ' to "sh", 'ష' to "sh", 'స' to "s", 'హ' to "h"
    )
}
