package com.logicaldevs.twdatapipeline.module.importexcel.controller

import com.logicaldevs.twdatapipeline.module.importexcel.service.ExcelLocationProcessor
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = ["*"])
class ExcelProcessController(
    private val excelProcessor: ExcelLocationProcessor
) {

    @PostMapping(
        "/processMasterExcel",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun processMasterExcel(@RequestParam("file") file: MultipartFile): ResponseEntity<ByteArray> {
        // Save uploaded file temporarily
        val inputFile = File.createTempFile("input_", ".xlsx")
        file.transferTo(inputFile)

        val outputFile = File.createTempFile("output_", ".xlsx")

        // Process Excel (your custom logic)
        excelProcessor.processExcel(inputFile, outputFile)

        // Prepare file for download
        val bytes = outputFile.readBytes()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.setContentDispositionFormData("attachment", "processed_${file.originalFilename}")

        // Clean up temporary files
        inputFile.delete()
        outputFile.deleteOnExit()

        return ResponseEntity.ok()
            .headers(headers)
            .body(bytes)
    }
}
