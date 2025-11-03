package com.logicaldevs.twdatapipeline.module.importexcel.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


import org.apache.poi.ss.usermodel.*

import java.text.SimpleDateFormat


@Service
class ExcelLocationProcessor(
    private val locationHistoryService: LocationHistoryService
) {

    private val haversineRadiusMeters = 500.0

    fun processExcel(inputFile: File, outputFile: File) {
        val workbook = XSSFWorkbook(FileInputStream(inputFile))
        val sheet = workbook.getSheetAt(0)
        val evaluator = workbook.creationHelper.createFormulaEvaluator()

        // Use coroutine scope for parallel processing
        runBlocking {
            val rows = (1..sheet.lastRowNum).mapNotNull { rowIdx ->
                sheet.getRow(rowIdx)?.let { row -> rowIdx to row }
            }

            // Parallel processing per row
            rows.chunked(500).map { chunk ->
                async (Dispatchers.Default) {
                    chunk.forEach { (rowIdx, row) ->
                        try {
                            processRow(rowIdx, row, evaluator)
                        } catch (e: Exception) {
                            println("Error processing row $rowIdx: ${e.message}")
                        }
                    }
                }
            }.awaitAll() // wait for all chunks
        }

        // Write back Excel
        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
    }

    private fun processRow(rowIdx: Int, row: Row, evaluator: FormulaEvaluator) {
        val fromDateStr = getCellStringValue(row.getCell(1), evaluator) ?: return
        val endDateStr = getCellStringValue(row.getCell(2), evaluator) ?: return
        val truckNumber = getCellStringValue(row.getCell(8), evaluator) ?: return
        val lat = getCellNumericValue(row.getCell(5), evaluator) ?: return
        val lng = getCellNumericValue(row.getCell(6), evaluator) ?: return

        val start = parseDateStartOfDay(fromDateStr)
        val end = parseDateEndOfDay(endDateStr)

        // Fetch DB rows using index — should be fast now
        val dbList = locationHistoryService.getByRdtRangeAndGroup(truckNumber, start, end)

        val any = dbList.firstOrNull() { db ->
            db.lat != null && db.lng != null && haversine(lat, lng, db.lat!!, db.lng!!) <= haversineRadiusMeters
            
        }
       

        val dataStatus = if (any!=null) "YES" else "NO"
//        val reference = "$start - $end"
        val fileName = dbList.firstOrNull()?.fileName ?: "No File Found"

        // Synchronized Excel update
        val formatToHumanReadable = formatToHumanReadable(any?.rdt.toString())
        synchronized(row.sheet.workbook) {
            setCellValue(row, 14, dataStatus)
            setCellValue(row, 15, if(dataStatus=="YES") formatToHumanReadable ?:"" else "")
            setCellValue(row, 16, fileName)
        }

        println("$rowIdx dataStatus:: $dataStatus $formatToHumanReadable $fileName")
    }

    private fun getCellStringValue(cell: Cell?, evaluator: FormulaEvaluator): String? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(cell.dateCellValue)
            } else cell.numericCellValue.toLong().toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> getCellStringValue(evaluator.evaluateInCell(cell), evaluator)
            else -> null
        }
    }

    private fun getCellNumericValue(cell: Cell?, evaluator: FormulaEvaluator): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            CellType.FORMULA -> getCellNumericValue(evaluator.evaluateInCell(cell), evaluator)
            else -> null
        }
    }

    private fun parseDateStartOfDay(input: String) = parseFlexibleDate(input).atStartOfDay()

    private fun parseDateEndOfDay(input: String) = parseFlexibleDate(input).atTime(23, 59, 59)

    private fun parseFlexibleDate(input: String): LocalDate {
        val patterns = listOf(
            "dd/MM/yyyy", "dd/MM/yyyy HH:mm:ss",
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss",
            "MM/dd/yyyy", "MM/dd/yyyy HH:mm:ss"
        )
        patterns.forEach { pattern ->
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern)
                return LocalDate.parse(input.substringBefore(" "), formatter)
            } catch (_: Exception) {}
        }
        throw IllegalArgumentException("Unrecognized date format: $input")
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun setCellValue(row: Row, index: Int, value: String) {
        val cell = row.getCell(index) ?: row.createCell(index)
        cell.setCellValue(value)
    }
}