package com.logicaldevs.twdatapipeline.module.importexcel.controller

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory
import com.logicaldevs.twdatapipeline.module.importexcel.service.ExcelImportService
import com.logicaldevs.twdatapipeline.module.importexcel.service.LocationHistoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime


@RestController
@RequestMapping("api/getLocationHistory")
@CrossOrigin(origins = ["*"])
@Validated
class LocationHistoryController @Autowired constructor(private val locationHistoryService: LocationHistoryService) {
    @GetMapping("get")
    fun getByRdtRangeAndGroup(
        @RequestParam groupName: String,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") start: LocalDateTime,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") end: LocalDateTime
    ): ResponseEntity<List<LocationHistory>> {
        return ResponseEntity.ok(locationHistoryService.getByRdtRangeAndGroup(groupName, start, end))
    }

}
