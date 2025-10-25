package com.logicaldevs.twdatapipeline.module.importexcel.repo

import com.logicaldevs.twdatapipeline.module.importexcel.model.LocationHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface LocationHistoryRepo: JpaRepository<LocationHistory, Long> {
    @Query(
        value = """
        SELECT * FROM location_history 
        WHERE group_name = :groupName 
          AND rdt BETWEEN :startDate AND :endDate
    """,
        nativeQuery = true
    )
    fun findByGroupNameAndRdtRange(
        @Param("groupName") groupName: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<LocationHistory>

}