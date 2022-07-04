package com.attafitamim.room.compound.sample

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class SecondaryCompound(
    @Embedded
    val name: SecondaryEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val name2: ThirdCompound
)