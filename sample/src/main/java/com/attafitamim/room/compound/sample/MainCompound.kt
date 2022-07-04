package com.attafitamim.room.compound.sample

import androidx.room.Embedded
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class MainCompound(
    @Embedded
    val parent: MainEntity,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val relation: SecondaryCompound,
)