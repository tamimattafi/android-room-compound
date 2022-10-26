package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MainEntity::class,
            parentColumns = ["name"],
            childColumns = ["name"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]
)
data class SecondEntity(
    @PrimaryKey
    val name: String,
    val date: String
)