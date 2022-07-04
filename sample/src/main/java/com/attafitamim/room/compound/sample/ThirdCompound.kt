package com.attafitamim.room.compound.sample

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class ThirdCompound(
    @Embedded
    val name: ThirdEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val name2: ForthEntity
)