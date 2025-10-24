package com.logicaldevs.twdatapipeline.module.pdfconvertor.controller

import com.logicaldevs.twdatapipeline.module.pdfconvertor.service.PdfProcessingService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
@Validated
class PdfController @Autowired constructor(
    private val pdfProcessingService: PdfProcessingService
) {

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

    @PostMapping("/process")
    fun processPdfs(@Valid @RequestBody request: ProcessRequest): ResponseEntity<ProcessResponse> {
        return try {
            // Simple test first - just check if folders exist
            val pdfPath = Paths.get(request.pdfFolder)
            val excelPath = Paths.get(request.excelFolder)

            if (!Files.exists(pdfPath)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProcessResponse(
                    success = false,
                    message = "PDF folder does not exist: ${request.pdfFolder}",
                    results = null
                ))
            }

            if (!Files.exists(excelPath)) {
                Files.createDirectories(excelPath)
            }

            val results = runBlocking {
                pdfProcessingService.processPdfs(request.pdfFolder, request.excelFolder)
            }
            ResponseEntity.ok(ProcessResponse(
                success = true,
                message = "Processing completed successfully",
                results = results
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResponse(
                success = false,
                message = "Error processing PDFs: ${e.message}",
                results = null
            ))
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP", "service" to "PDF to Excel Converter"))
    }

    @PostMapping("/test")
    fun test(@Valid @RequestBody request: ProcessRequest): ResponseEntity<ProcessResponse> {
        return try {
            ResponseEntity.ok(ProcessResponse(
                success = true,
                message = "Test successful - PDF folder: ${request.pdfFolder}, Excel folder: ${request.excelFolder}",
                results = listOf("Test result 1", "Test result 2")
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessResponse(
                success = false,
                message = "Test failed: ${e.message}",
                results = null
            ))
        }
    }
}