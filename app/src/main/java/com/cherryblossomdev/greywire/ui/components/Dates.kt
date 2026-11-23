package com.cherryblossomdev.greywire.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Dates from the backend are ISO 8601 in UTC ("2026-11-21T22:59:23.000Z") or
// plain dates ("2026-11-21"). SimpleDateFormat rather than java.time, which
// needs API 26 (minSdk is 24).
private val backendPatterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd")

/** Parses a backend timestamp, or null if it isn't one. */
fun parseBackendDate(value: String): Date? {
    for (pattern in backendPatterns) {
        try {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.isLenient = false
            return format.parse(value) ?: continue
        } catch (e: Exception) { /* try the next pattern */ }
    }
    return null
}

/** "Nov 21, 2026" from a backend timestamp; anything else is shown as given. */
fun formatShortDate(value: String): String =
    parseBackendDate(value)?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it) } ?: value

/** "3:59 PM" (local time) from a backend timestamp, or "" if it isn't one. */
fun formatTime(value: String): String =
    parseBackendDate(value)?.let { SimpleDateFormat("h:mm a", Locale.getDefault()).format(it) } ?: ""
