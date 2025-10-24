package com.logicaldevs.twdatapipeline.module.importexcel.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table
@Entity
data class LocationHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long?=null,

    var lat: Double? = null,

    var lng: Double? = null,

    var rdt: LocalDateTime? = null,

    @Column(length = 1000)
    var landMark: String? = null,

    var speed: Double? = null,

    var direction: Double? = null,

    var distance: Double? = null,

    var travelTime: String? = null,

    var stopTime: String? = null,

    var groupName: String? = null,

    )
