package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Entity

@Entity(primaryKeys = ["secondId", "thirdId"])
data class SecondThirdJunction(
    val secondId: String,
    val thirdId: String
)