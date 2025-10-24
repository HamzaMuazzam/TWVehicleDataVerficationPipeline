package com.logicaldevs.twdatapipeline.module.pdfconvertor.service

import com.logicaldevs.twdatapipeline.sockets.ProgressWebSocketHandler
import com.logicaldevs.twdatapipeline.utils.removeByString
import com.logicaldevs.twdatapipeline.utils.extractFromToDates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

@Service
class PdfProcessingService @Autowired constructor(
    private val progressWebSocketHandler: ProgressWebSocketHandler
) {

    suspend fun processPdfs(pdfFolder: String, excelFolder: String): List<String> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()

        try {
            Files.createDirectories(Paths.get(excelFolder))
            safeProgressUpdate(5, "Created Excel output directory")

            val pdfFiles = Files.list(Paths.get(pdfFolder))
                .filter { it.toString().lowercase().endsWith(".pdf") }
                .toList()

            safeProgressUpdate(10, "Found ${pdfFiles.size} PDF files to process")

            if (pdfFiles.isEmpty()) {
                safeProgressUpdate(100, "No PDF files found in the specified folder")
                return@withContext listOf("No PDF files found in $pdfFolder")
            }

            val maxThreads = Runtime.getRuntime().availableProcessors()
            val dispatcher = newFixedThreadPoolContext(maxThreads, "pdfPool")

            // Launch coroutines for each PDF
            val jobs = pdfFiles.mapIndexed { index, pdfPath ->
                async(dispatcher) {
                    val progress = 10 + (index * 80 / pdfFiles.size)
                    safeProgressUpdate(progress, "Processing ${pdfPath.fileName}")
                    processPdf(pdfPath, excelFolder)
                }
            }

            // Await all results
            val results = jobs.awaitAll()

            dispatcher.close() // release the thread pool

            val totalTime = (System.currentTimeMillis() - start) / 1000.0
            safeProgressUpdate(100, "All PDFs processed in ${String.format("%.2f", totalTime)} seconds")

            results
        } catch (e: Exception) {
            safeProgressUpdate(0, "Error: ${e.message}")
            throw e
        }
    }

    private fun safeProgressUpdate(progress: Int, message: String) {
        try {
            progressWebSocketHandler.sendProgressUpdate(progress, message)
        } catch (e: Exception) {
            // Ignore WebSocket errors, just log them
            println("WebSocket update failed: ${e.message}")
        }
    }

    private fun processPdf(pdfPath: Path, excelFolder: String): String {
        val pdfFileName = pdfPath.fileName.toString()
        var pdfNameWithDates = ""
        var excelName = ""

        return try {
            PDDocument.load(pdfPath.toFile()).use { document ->
                val workbook: Workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Data")
                val headerRow = sheet.createRow(0)
                val headers = arrayOf(
                    "Group Name", "RDT", "LandMark", "Speed", "Direction", "Distance",
                    "Travel Time", "Stop Time", "LAT", "LON"/*, "PDF File", "Page", "Row"*/
                )
                val headerFont = workbook.createFont().apply {
                    bold = true
                    color = IndexedColors.WHITE.index
                    fontHeightInPoints = 12
                }
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = IndexedColors.GREY_80_PERCENT.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER

                    // Add thin borders around
                    borderTop = BorderStyle.THIN
                    borderBottom = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN

                    setFont(headerFont)
                }

// Create header cells
                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerStyle
                }
                val stripper = PDFTextStripper()
                val totalPages = document.numberOfPages
                var rowCounter = 1
                var secondTableStarted = false

                val rowStartPattern = Pattern.compile(
                    "(?:0?[1-9]|[12][0-9]|3[01])/(?:0?[1-9]|1[0-2])/\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+(?:AM|PM)",
                    Pattern.CASE_INSENSITIVE
                )

                for (page in 1..totalPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val text = stripper.getText(document)

                    val normalized = text.replace(Regex("\\r\\n|\\r|\\n"), " ")
                        .replace(Regex("\\s+AM\\s+"), " AM ")
                        .replace(Regex("\\s+PM\\s+"), " PM ")

                    val matcher = rowStartPattern.matcher(normalized)
                    val starts = mutableListOf<Int>()
                    while (matcher.find()) {
                        starts.add(matcher.start())
                    }

                    val rows = mutableListOf<String>()
                    if (starts.isEmpty()) {
                        normalized.split(" ").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                            rows.add(it)
                        }
                    } else {
                        for (i in starts.indices) {
                            val begin = starts[i]
                            val end = if (i + 1 < starts.size) starts[i + 1] else normalized.length
                            val row = normalized.substring(begin, end).trim()
                            if (row.isNotEmpty()) rows.add(row)
                        }
                    }

                    for (trimmed in rows) {
                        if (!secondTableStarted && trimmed.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{4}.*"))) {
                            secondTableStarted = true
                        }

                        if (!secondTableStarted) continue

                        var matcherRow = trimmed.split(" ").toMutableList()
                        if (trimmed.contains("From")) {
                            val extractFromToDates = extractFromToDates(trimmed)
                            pdfNameWithDates = extractFromToDates
                            if(trimmed.contains("Total")){
                                matcherRow = removeByString(trimmed, "Total").split(" ").toMutableList()
                            } else if(matcherRow.contains("Activity")){
                                matcherRow = removeByString(trimmed, "Activity").split(" ").toMutableList()
                            }
                        }

                        if (matcherRow.isEmpty()) continue
                        if(matcherRow.size - 7 < 3) continue

                        val subList = matcherRow.subList(matcherRow.size - 7, matcherRow.size)
                        val rdt = matcherRow[0] + " " + matcherRow[1] + " " + matcherRow[2]
                        val landmarkList: List<String> = matcherRow.subList(3, matcherRow.size - 7)
                        val landmark = landmarkList.joinToString(" ")
                        val speed = subList[0]
                        val direction = subList[1]
                        val distance = subList[2]
                        val travel = subList[3]
                        val stop = subList[4]
                        val lat = subList[5]
                        val lon = subList[6]

                        val record = arrayOf(
                            pdfFileName.split(" ")[0], rdt, landmark, speed, direction, distance,
                            travel, stop, lat, lon
                        )

                        val row = sheet.createRow(rowCounter++)
                        record.forEachIndexed { i, value -> row.createCell(i).setCellValue(value) }
                    }
                }

                excelName = pdfFileName.replace(Regex("(?i)\\.pdf$"), " $pdfNameWithDates.xlsx")
                val excelPath = Paths.get(excelFolder, excelName)
                FileOutputStream(excelPath.toFile()).use { output ->
                    workbook.write(output)
                }
                workbook.close()
            }

            // Delete PDF after Excel is written
            Files.deleteIfExists(pdfPath)

            "Saved Excel: $excelName and deleted PDF: $pdfFileName"
        } catch (e: Exception) {
            e.printStackTrace()
            "Error processing $pdfFileName: ${e.message}"
        }
    }
}