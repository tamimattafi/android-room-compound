package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class MainCompound(
    @Embedded
    val mainEntity: MainEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val secondaryCompound: List<SecondCompound>?,
)