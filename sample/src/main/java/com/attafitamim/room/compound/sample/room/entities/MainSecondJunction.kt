package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Entity

@Entity(primaryKeys = ["mainId", "secondId"])
data class MainSecondJunction(
    val mainId: String,
    val secondId: String
)