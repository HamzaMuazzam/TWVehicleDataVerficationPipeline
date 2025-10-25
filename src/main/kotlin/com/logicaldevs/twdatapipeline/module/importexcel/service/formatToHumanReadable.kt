package com.logicaldevs.twdatapipeline.module.importexcel.service

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatToHumanReadable(input: String?): String? {
    if (input.isNullOrEmpty() || input.isBlank() || input.contains("null")) {
        return null
    }

    return try {
        // Detect format based on input length
        val inputFormatter = when {
            input.length == 16 -> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            input.length == 19 -> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            else -> throw IllegalArgumentException("Unknown date format: $input")
        }

        val dateTime = LocalDateTime.parse(input, inputFormatter)
        val outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a")
        dateTime.format(outputFormatter)
    } catch (e: Exception) {
        "Invalid date format: $input"
    }
}
