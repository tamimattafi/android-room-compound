package com.attafitamim.room.compound.sample

import androidx.room.Entity

@Entity
data class SecondaryEntity(
    val name: String,
    val date: String
)