package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class ThirdCompound(
    @Embedded
    val thirdEntity: ThirdEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val secondEntity: List<SecondEntity>?,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val forthEntity: List<ForthEntity>?,
)