package com.example.utils

fun normalizeArabic(text: String): String {
    if (text.isBlank()) return text
    var normalized = text.replace(Regex("[\u0617-\u061A\u064B-\u0652]"), "") // Remove diacritics
    normalized = normalized.replace(Regex("[أإآ]"), "ا")
    normalized = normalized.replace("ى", "ي")
    normalized = normalized.replace("ة", "ه")
    return normalized.trim().lowercase()
}

fun levenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length

    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,      // deletion
                dp[i][j - 1] + 1,      // insertion
                dp[i - 1][j - 1] + cost // substitution
            )
        }
    }

    return dp[s1.length][s2.length]
}
