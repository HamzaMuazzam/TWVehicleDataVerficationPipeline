package com.logicaldevs.twdatapipeline.module.importexcel.repo

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationHistoryRepo: JpaRepository<LocationHistory, Long> {
}