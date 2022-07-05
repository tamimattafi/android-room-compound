package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ForthEntity(
    @PrimaryKey
    val name: String,
    val date: String
)