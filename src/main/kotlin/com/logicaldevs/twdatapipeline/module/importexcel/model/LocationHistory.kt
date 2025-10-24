package com.logicaldevs.twdatapipeline.module.importexcel.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table
@Entity
data class LocationHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    val lat: Double? = null,

    val lng: Double? = null,

    val rdt: LocalDateTime? = null,

    val landMark: String? = null,

    val speed: Double? = null,

    val direction: Double? = null,

    val distance: Double? = null,

    val travelTime: String? = null,

    val stopTime: String? = null,

    // Many locations belong to one vehicle group
    @ManyToOne(fetch = FetchType.LAZY)
    val vehicleGroup: VehicleGroup? = null

)
