package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class SecondCompound(
    @Embedded
    val secondaryEntity: SecondEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val thirdCompound: ThirdCompound
)