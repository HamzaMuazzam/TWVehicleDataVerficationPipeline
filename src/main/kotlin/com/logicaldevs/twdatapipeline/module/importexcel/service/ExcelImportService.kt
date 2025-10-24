package com.logicaldevs.twdatapipeline.module.importexcel.service

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory
import com.logicaldevs.twdatapipeline.module.importexcel.repo.LocationHistoryRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths

import kotlinx.coroutines.*
import java.nio.file.*
import kotlin.io.path.extension
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import kotlin.io.path.deleteIfExists

@Service
class ExcelImportService @Autowired constructor(
    private val locationHistoryRepo: LocationHistoryRepo,
) {


    suspend fun processExcels(excelFolder: String, batchSize: Int = 50): Int = coroutineScope {
        val start = System.currentTimeMillis()
        val excelDir = Paths.get(excelFolder)
        val excelFiles = Files.walk(excelDir)
            .filter { Files.isRegularFile(it) && it.extension in listOf("xls", "xlsx") }
            .toList()

        println("Found ${excelFiles.size} Excel files in $excelFolder")

        val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
        val dispatcher = newFixedThreadPoolContext(maxThreads, "ExcelPool")

        var totalProcessed = 0

        try {
            // Sequentially process batches of files
            excelFiles.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                println("\n🟢 Starting batch ${batchIndex + 1} with ${batch.size} files...")

                val locationHistories = Collections.synchronizedList(mutableListOf<LocationHistory>())

                // Process each file in parallel within this batch
                val processedFiles = batch.map { path ->
                    async(dispatcher) {
                        try {
                            processExcelData(path, locationHistories)
                            path // return the path of successfully processed file
                        } catch (e: Exception) {
                            println("⚠ Skipping file ${path.fileName} due to error: ${e.message}")
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                // Save all valid data to DB
                if (locationHistories.isNotEmpty()) {
                    try {
                        locationHistoryRepo.saveAll(locationHistories)
                        println("💾 Saved ${locationHistories.size} records from batch ${batchIndex + 1}")
                    } catch (e: Exception) {
                        println("❌ DB error in batch ${batchIndex + 1}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                // Delete successfully processed files
                processedFiles.forEach { file ->
                    try {
                        Files.deleteIfExists(file)
                    } catch (e: Exception) {
                        println("⚠ Failed to delete ${file.fileName}: ${e.message}")
                    }
                }

                println("🗑️ Deleted ${processedFiles.size} files in batch ${batchIndex + 1}")
                totalProcessed += processedFiles.size
            }

            val totalTime = (System.currentTimeMillis() - start) / 1000.0
            println("✅ Processed $totalProcessed Excel files in ${"%.2f".format(totalTime)} sec using $maxThreads threads")

            return@coroutineScope totalProcessed
        } finally {
            dispatcher.close()
        }
    }

    fun processExcelData(path: Path, locationHistories: MutableList<LocationHistory>): String {
        if (path.fileName.toString().contains("~$")) return "Skipped hidden file: ${path.fileName}"

        FileInputStream(path.toFile()).use { fis ->
            val workbook = WorkbookFactory.create(fis)
            val sheet = workbook.getSheetAt(0)
            val rows = sheet.rowIterator().asSequence().toList()

            rows.drop(1).forEach { row ->
                try {
                    val locationHistory = LocationHistory()
                    row.forEachIndexed { columnIndex, cell ->
                        val value = cell.toString()
                        when (columnIndex) {
                            0 -> locationHistory.groupName = value
                            1 -> {
                                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH)
                                locationHistory.rdt = LocalDateTime.parse(value, formatter)
                            }
                            2 -> locationHistory.landMark = value
                            3 -> locationHistory.speed = value.toDoubleOrNull()
                            4 -> locationHistory.direction = value.toDoubleOrNull()
                            5 -> locationHistory.distance = value.toDoubleOrNull()
                            6 -> locationHistory.travelTime = value
                            7 -> locationHistory.stopTime = value
                            8 -> locationHistory.lat = value.toDoubleOrNull()
                            9 -> locationHistory.lng = value.toDoubleOrNull()
                        }
                    }
                    locationHistories.add(locationHistory)
                } catch (e: Exception) {
                    println("⚠ Skipping row ${row.rowNum} in file ${path.fileName} due to error: ${e.message}")
                }
            }

            workbook.close()
            return "Processed file: ${path.fileName}"
        }
    }


}