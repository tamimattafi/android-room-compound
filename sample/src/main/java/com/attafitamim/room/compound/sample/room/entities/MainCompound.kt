package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class MainCompound(
    @Embedded
    val mainEntity: MainEntity,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = MainSecondJunction::class,
            parentColumn = "mainId",
            entityColumn = "secondId"
        )
    )
    val secondaryCompounds: List<SecondCompound>?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = MainSecondJunction::class,
            parentColumn = "mainId",
            entityColumn = "secondId"
        )
    )
    val secondaryEntities: List<SecondEntity>?,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val secondaryCompound: SecondCompound?
)