package com.logicaldevs.twdatapipeline.module.importexcel.service

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths

import kotlinx.coroutines.*
import java.nio.file.*
import kotlin.io.path.extension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service

class ExcelImportService @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
) {

    suspend fun processExcels(
        excelFolder: String,
        batchSize: Int = 500,
        dbChunkSize: Int = 10_000
    ): Int = coroutineScope {
        val start = System.currentTimeMillis()
        val excelDir = Paths.get(excelFolder)

        val excelFiles = Files.walk(excelDir)
            .filter { Files.isRegularFile(it) && it.extension in listOf("xls", "xlsx") }
            .toList()

        println("Found ${excelFiles.size} Excel files in $excelFolder")
        var totalProcessed = 0

        // Parallel dispatcher for file extraction
        val extractDispatcher = Dispatchers.IO.limitedParallelism(
            Runtime.getRuntime().availableProcessors().coerceAtLeast(4) * 2
        )

        // Parallel dispatcher for DB insert
        val dbDispatcher = Dispatchers.IO.limitedParallelism(4)

        for ((batchIndex, batch) in excelFiles.chunked(batchSize).withIndex()) {
            println("\n🟢 Starting batch ${batchIndex + 1} with ${batch.size} files...")

            val results = batch.map { path ->
                async(extractDispatcher) {
                    try {
                        processExcelData(path)
                    } catch (e: Exception) {
                        println("⚠ Skipping file ${path.fileName} due to error: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()

            val locationHistories = results.flatten()
            println("💾 File data Extracted ${locationHistories.size} records from batch ${batchIndex + 1}")

            if (locationHistories.isNotEmpty()) {
                val dbChunks = locationHistories.chunked(dbChunkSize)
                dbChunks.mapIndexed { i, chunk ->
                    async(dbDispatcher) {
                        saveBatchJdbc(chunk)
                        println("✅ Inserted chunk ${i + 1}/${dbChunks.size} of batch ${batchIndex + 1} (${chunk.size} rows)")
                    }
                }.awaitAll()

                println("💾 Saved ${locationHistories.size} records from batch ${batchIndex + 1}")
            }

            // Delete processed files
            batch.forEach { file ->
                try {
                    Files.deleteIfExists(file)
                } catch (e: Exception) {
                    println("⚠ Failed to delete ${file.fileName}: ${e.message}")
                }
            }

            println("🗑️ Deleted ${batch.size} files in batch ${batchIndex + 1}")
            totalProcessed += batch.size
        }

        val totalTime = (System.currentTimeMillis() - start) / 1000.0
        println("✅ Processed $totalProcessed Excel files in ${"%.2f".format(totalTime)} sec")
        return@coroutineScope totalProcessed
    }

    /**
     * Extracts data from a single Excel file into a list of LocationHistory.
     */
    fun processExcelData(path: Path): List<LocationHistory> {
        if (path.fileName.toString().contains("~$")) return emptyList()

        val list = mutableListOf<LocationHistory>()

        FileInputStream(path.toFile()).use { fis ->
            val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(fis)
            val sheet = workbook.getSheetAt(0)
            val rows = sheet.rowIterator().asSequence().toList()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH)

            rows.drop(1).forEach { row ->
                try {
                    val locationHistory = LocationHistory()
                    row.forEachIndexed { columnIndex, cell ->
                        val value = cell.toString()
                        when (columnIndex) {
                            0 -> locationHistory.groupName = value
                            1 -> locationHistory.rdt = runCatching { LocalDateTime.parse(value, formatter) }.getOrNull()
                            2 -> locationHistory.landMark = value
                            3 -> locationHistory.speed = value.toDoubleOrNull()
                            4 -> locationHistory.direction = value.toDoubleOrNull()
                            5 -> locationHistory.distance = value.toDoubleOrNull()
                            6 -> locationHistory.travelTime = value
                            7 -> locationHistory.stopTime = value
                            8 -> locationHistory.lat = value.toDoubleOrNull()
                            9 -> locationHistory.lng = value.toDoubleOrNull()
                            10 -> locationHistory.fileName = value
                            11 -> locationHistory.page = value.toIntOrNull()
                            12 -> locationHistory.row = value.toIntOrNull()
                        }
                    }
                    list.add(locationHistory)
                } catch (e: Exception) {
                    println("⚠ Skipping row ${row.rowNum} in file ${path.fileName}: ${e.message}")
                }
            }

            workbook.close()
        }

        return list
    }

    /**
     * Fast JDBC batch insert for LocationHistory (10x+ faster than JPA)
     */
    fun saveBatchJdbc(records: List<LocationHistory>) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO location_history (
                lat, lng, rdt, land_mark, speed, direction, distance, travel_time, stop_time, group_name,file_name,page,row
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            records,
            1000
        ) { ps, record ->
            ps.setObject(1, record.lat)
            ps.setObject(2, record.lng)
            ps.setObject(3, record.rdt)
            ps.setString(4, record.landMark)
            ps.setObject(5, record.speed)
            ps.setObject(6, record.direction)
            ps.setObject(7, record.distance)
            ps.setString(8, record.travelTime)
            ps.setString(9, record.stopTime)
            ps.setString(10, record.groupName)
            ps.setString(11, record.fileName)
            ps.setObject(12, record.page)
            ps.setObject(13, record.row)
        }
    }
}
