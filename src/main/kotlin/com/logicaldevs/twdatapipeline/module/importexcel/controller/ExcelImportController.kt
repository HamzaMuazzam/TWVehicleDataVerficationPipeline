package com.logicaldevs.twdatapipeline.module.importexcel.controller

import com.logicaldevs.twdatapipeline.module.importexcel.dto.ProcessExcelRequest
import com.logicaldevs.twdatapipeline.module.importexcel.dto.ProcessExcelResponse
import com.logicaldevs.twdatapipeline.module.importexcel.service.ExcelImportService
import com.logicaldevs.twdatapipeline.module.pdfconvertor.dto.ProcessResponse
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("api/importexcel")
@CrossOrigin(origins = ["*"])
@Validated
class ExcelImportController @Autowired constructor(private val excelImportService: ExcelImportService) {

    @PostMapping("/process")
    fun processExcels(@Valid @RequestBody request: ProcessExcelRequest): ResponseEntity<ProcessExcelResponse> {
        return try {
            val excelPath = Paths.get(request.excelFolder)

            if (!Files.exists(excelPath)) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessExcelResponse(
                    success = false,
                    message = "Error processing Excels: folder does not exist ${excelPath.toAbsolutePath()}",
                    results = null
                ))            }

            val results = runBlocking {
                excelImportService.processExcels(request.excelFolder)
            }
            ResponseEntity.ok(ProcessExcelResponse(
                success = true,
                message = "Processing completed successfully",
                results = results
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProcessExcelResponse(
                success = false,
                message = "Error processing Excels: ${e.message}",
                results = null
            ))
        }
    }
}