package com.logicaldevs.twdatapipeline.module.importexcel.service

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory
import com.logicaldevs.twdatapipeline.module.importexcel.repo.LocationHistoryRepo
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LocationHistoryService(
    private val locationHistoryRepo: LocationHistoryRepo
) {
    fun getByRdtRangeAndGroup(groupName: String, start: LocalDateTime, end: LocalDateTime): List<LocationHistory> {
        return locationHistoryRepo.findByGroupNameAndRdtRange(groupName, start, end)
    }
}
