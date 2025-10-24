package com.logicaldevs.twdatapipeline.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.regex.Pattern

fun extractFromToDates(input: String): String {
    try {
        // Regex to capture "From : <date>" and "To: <date>"
        val fromPattern = Pattern.compile("From\\s*:\\s*([A-Za-z]+,\\s+[A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})")
        val toPattern = Pattern.compile("To\\s*:\\s*([A-Za-z]+,\\s+[A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})")

        val fromMatcher = fromPattern.matcher(input)
        val toMatcher = toPattern.matcher(input)

        if (fromMatcher.find() && toMatcher.find()) {
            val fromDateStr = fromMatcher.group(1)
            val toDateStr = toMatcher.group(1)

            val parser = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH)
            val formatter = SimpleDateFormat("dd-MMMM-yyyy", Locale.ENGLISH)

            val fromDate: Date = parser.parse(fromDateStr)
            val toDate: Date = parser.parse(toDateStr)

            return "${formatter.format(fromDate)} ${formatter.format(toDate)}"
        }
    } catch (e: Exception) {
        // Ignore parsing errors and return empty string
    }

    return ""
}

// Example usage:
fun main() {
    val input = """
        12/09/2023 04:11:50 AM 1||PSO (Thor Filling Station)N35Tehsil ChilasGilgit- Baltistan 0 205 0.00 00:02:00 00:30:00 35.4878 73.8558 
        Total 1786.00 0d, 07:24:03 0d, 21:59:30 Activity Report (AA112) 
        Reg No: AA112 
        From : Monday, September 11, 2023 
        To: Tuesday, September 12, 2023 
        Time Zone: GMT +05:00
    """.trimIndent()

    println(extractFromToDates(input))
}
