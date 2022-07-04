package com.attafitamim.room.compound.sample

import androidx.room.Entity

@Entity
data class MainEntity(
    val name: String,
    val date: String
)