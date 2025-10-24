package com.logicaldevs.twdatapipeline.module.importexcel.controller

import com.logicaldevs.twdatapipeline.module.importexcel.service.ExcelImportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/importexcel")
@CrossOrigin(origins = ["*"])
@Validated
class ExcelImportController @Autowired constructor(private val excelImportService: ExcelImportService) {

}