package com.xorcall.engine

import java.util.Locale
import java.util.regex.Pattern

/**
 * PhoneticSpeechNormalizer
 * Normalizes speech recognition output from US English acoustic models (like Vosk small-en)
 * into canonical cyber extortion, kidnap, and digital arrest intent tokens.
 *
 * Handles:
 * 1. Number words to numeric digits ("five" -> "5", "fifty" -> "50")
 * 2. Indian currency terms transcribed phonetically by US models ("five lacks", "five lock" -> "5 lakh")
 * 3. Split syllables ("kid nap" -> "kidnap", "ex torsion" -> "extortion")
 * 4. Spelled-out law enforcement / regulatory acronyms ("c b i" -> "cbi", "r b i" -> "rbi", "f e d e x" -> "fedex")
 */
object PhoneticSpeechNormalizer {

    private val NUMBER_MAP = mapOf(
        "zero" to "0",
        "one" to "1",
        "two" to "2",
        "three" to "3",
        "four" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8",
        "nine" to "9",
        "ten" to "10",
        "twenty" to "20",
        "thirty" to "30",
        "forty" to "40",
        "fifty" to "50",
        "sixty" to "60",
        "seventy" to "70",
        "eighty" to "80",
        "ninety" to "90",
        "hundred" to "100",
        "thousand" to "1000",
        "million" to "million",
        "crore" to "crore"
    )

    // Regex for Indian currency words transcribed phonetically by US models
    private val LAKH_PHONETICS_REGEX = Pattern.compile(
        "\\b(lack|lacks|lock|locks|luck|lucks|lac|lacs|lax|lake|lakes)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for spelled out Indian/Federal agencies
    private val CBI_REGEX = Pattern.compile("\\b(c\\s+b\\s+i|see\\s+bee\\s+eye)\\b", Pattern.CASE_INSENSITIVE)
    private val RBI_REGEX = Pattern.compile("\\b(r\\s+b\\s+i|are\\s+bee\\s+eye)\\b", Pattern.CASE_INSENSITIVE)
    private val TRAI_REGEX = Pattern.compile("\\b(t\\s+r\\s+a\\s+i|tee\\s+are\\s+ay\\s+eye)\\b", Pattern.CASE_INSENSITIVE)
    private val FEDEX_REGEX = Pattern.compile("\\b(f\\s+e\\s+d\\s+e\\s+x|fed\\s+ex)\\b", Pattern.CASE_INSENSITIVE)
    private val DHL_REGEX = Pattern.compile("\\b(d\\s+h\\s+l|dee\\s+aich\\s+ell)\\b", Pattern.CASE_INSENSITIVE)
    private val MDMA_REGEX = Pattern.compile("\\b(m\\s+d\\s+m\\s+a|em\\s+dee\\s+em\\s+ay)\\b", Pattern.CASE_INSENSITIVE)
    private val UPI_REGEX = Pattern.compile("\\b(u\\s+p\\s+i|you\\s+pee\\s+eye)\\b", Pattern.CASE_INSENSITIVE)
    private val FD_REGEX = Pattern.compile("\\b(f\\s+d|ef\\s+dee)\\b", Pattern.CASE_INSENSITIVE)

    // Regex for split threat words
    private val KIDNAP_REGEX = Pattern.compile("\\b(kid\\s+nap|kid\\s+napped|kid\\s+napping)\\b", Pattern.CASE_INSENSITIVE)
    private val EXTORTION_REGEX = Pattern.compile("\\b(ex\\s+torsion|distortion)\\b", Pattern.CASE_INSENSITIVE)
    private val DIGITAL_ARREST_REGEX = Pattern.compile("\\b(digital\\s+a\\s*rest)\\b", Pattern.CASE_INSENSITIVE)

    /**
     * Normalizes a raw spoken transcript into canonical threat terminology.
     */
    fun normalize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText.lowercase(Locale.ROOT).trim()

        // 1. Replace spoken number words with digits (word boundaries)
        for ((word, digit) in NUMBER_MAP) {
            text = text.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), digit)
        }

        // 2. Normalize phonetic Indian currency variants to "lakh"
        text = LAKH_PHONETICS_REGEX.matcher(text).replaceAll("lakh")

        // 3. Normalize agency and institutional acronyms
        text = CBI_REGEX.matcher(text).replaceAll("cbi")
        text = RBI_REGEX.matcher(text).replaceAll("rbi")
        text = TRAI_REGEX.matcher(text).replaceAll("trai")
        text = FEDEX_REGEX.matcher(text).replaceAll("fedex")
        text = DHL_REGEX.matcher(text).replaceAll("dhl")
        text = MDMA_REGEX.matcher(text).replaceAll("mdma")
        text = UPI_REGEX.matcher(text).replaceAll("upi")
        text = FD_REGEX.matcher(text).replaceAll("fd")

        // 4. Normalize split threat keywords
        text = KIDNAP_REGEX.matcher(text).replaceAll("kidnap")
        text = EXTORTION_REGEX.matcher(text).replaceAll("extortion")
        text = DIGITAL_ARREST_REGEX.matcher(text).replaceAll("digital arrest")

        // 5. Clean up extra whitespace
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }

    /**
     * Checks if normalized or raw text contains an unequivocal violent extortion,
     * kidnap, or ransom phrase.
     */
    fun containsHighSeverityMarker(text: String): Boolean {
        val norm = normalize(text)
        val highMarkers = listOf(
            "5 lakh", "5 lakhs", "500000", "50 lakh", "crore", "ransom",
            "kidnap", "kill", "killing", "murder", "son", "daughter", "child",
            "extort", "extortion", "accident", "digital arrest", "rbi escrow",
            "cbi", "narcotics", "mdma", "parcel intercepted"
        )
        return highMarkers.any { marker ->
            norm.contains(marker, ignoreCase = true) || text.contains(marker, ignoreCase = true)
        }
    }
}
