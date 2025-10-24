package com.logicaldevs.twdatapipeline.module.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebController {

    @GetMapping("/")
    fun index(): String {
        return "index"
    }
}