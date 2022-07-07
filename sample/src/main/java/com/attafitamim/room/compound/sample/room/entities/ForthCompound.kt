package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class ForthCompound(
    @Embedded
    val forthEntity: ForthEntity,

    @Embedded
    val second: ForthEntity,
)