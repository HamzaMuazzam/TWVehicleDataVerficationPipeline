package com.logicaldevs.twdatapipeline.module.importexcel.dto

import jakarta.validation.constraints.NotBlank

data class ProcessExcelRequest(
    @field:NotBlank(message = "Folder path is required")
        val excelFolder: String,
    )

    data class ProcessExcelResponse(
        val success: Boolean,
        val message: String,
        val results: List<String>? = null
    )