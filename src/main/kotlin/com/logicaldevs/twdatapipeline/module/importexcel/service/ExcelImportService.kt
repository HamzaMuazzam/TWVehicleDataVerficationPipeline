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
import java.util.Locale
import kotlin.io.path.deleteIfExists

@Service
class ExcelImportService @Autowired constructor(
    private val locationHistoryRepo: LocationHistoryRepo,
) {


    suspend fun processExcels(excelFolder: String): List<String> = coroutineScope {
        val start = System.currentTimeMillis()
        val excelDir = Paths.get(excelFolder)
        val excelFiles = Files.walk(excelDir)
            .filter { Files.isRegularFile(it) && it.extension in listOf("xls", "xlsx") }
            .toList()

        println("Found ${excelFiles.size} Excel files in $excelFolder")

        val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
        val dispatcher = newFixedThreadPoolContext(maxThreads, "ExcelPool")

        try {
            val locationHistories = mutableListOf<LocationHistory>()
            val results = excelFiles.chunked(1000).flatMap { chunk ->
                withContext(dispatcher) {
                    chunk.map { path ->
                        async {
                            processExcelData(path,locationHistories)
                        }
                    }.awaitAll()
                }
            }
            val totalTime = (System.currentTimeMillis() - start) / 1000.0
            println("✅ Processed ${excelFiles.size} Excel files in ${"%.2f".format(totalTime)} sec using $maxThreads threads")
            locationHistoryRepo.saveAll(locationHistories)
            excelFiles.forEach {file->
                file.deleteIfExists()
            }
            results
        } finally {
            dispatcher.close()
        }
    }

    fun processExcelData(path: Path, locationHistories: MutableList<LocationHistory>): String {
        if(path.fileName.toString().contains("~$")) return "Not processed Hidden ${path.fileName}"
        FileInputStream(path.toFile()).use { fis ->
            val workbook = WorkbookFactory.create(fis)
            val sheet = workbook.getSheetAt(0)
            val noOfRows = sheet.rowIterator().asSequence().toMutableList()
            noOfRows.forEachIndexed { rowIndex, singleRow ->
                if(rowIndex != 0) {
                    val locationHistory = LocationHistory()
                    singleRow.forEachIndexed { columnIndex, value ->
                        try{
                            val cell = value.row.getCell(columnIndex)?.toString().orEmpty()
                            if(columnIndex==0){
                                locationHistory.groupName = cell
                            }
                            else if(columnIndex==1) {
                                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH)
                                val rdt: LocalDateTime = LocalDateTime.parse(cell, formatter)
                                locationHistory.rdt = rdt
                            }
                            else if(columnIndex==2) {
                                locationHistory.landMark = cell
                            }
                            else if(columnIndex==3) {
                                locationHistory.speed = cell.toDoubleOrNull()
                            }
                            else if(columnIndex==4) {
                                locationHistory.direction = cell.toDoubleOrNull()
                            }
                            else if(columnIndex==5) {
                                locationHistory.distance = cell.toDoubleOrNull()
                            }
                            else if(columnIndex==6) {
                                locationHistory.travelTime = cell
                            }
                            else if(columnIndex==7) {
                                locationHistory.stopTime = cell
                            }
                            else if(columnIndex==8) {
                                locationHistory.lat = cell.toDoubleOrNull()
                            }
                            else if(columnIndex==9) {
                                locationHistory.lng = cell.toDoubleOrNull()
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                    locationHistories.add(locationHistory)
                }
            }

            workbook.close()
            return "File: ${path.fileName}"
        }
    }


}