package com.logicaldevs.twdatapipeline.module.pdfconvertor.dto

import jakarta.validation.constraints.NotBlank

data class ProcessRequest(
        @field:NotBlank(message = "PDF folder path is required")
        val pdfFolder: String,
        @field:NotBlank(message = "Excel folder path is required")
        val excelFolder: String
    )

    data class ProcessResponse(
        val success: Boolean,
        val message: String,
        val results: List<String>? = null
    )