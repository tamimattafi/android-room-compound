package com.attafitamim.room.compound.sample.room.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.attafitamim.room.compound.annotations.Compound

@Compound
data class SecondCompound(
    @Embedded
    val secondaryEntity: SecondEntity?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = SecondThirdJunction::class,
            parentColumn = "secondId",
            entityColumn = "thirdId"
        )
    )
    val thirdCompounds: List<ThirdCompound>?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
        associateBy = Junction(
            value = SecondThirdJunction::class,
            parentColumn = "secondId",
            entityColumn = "thirdId"
        )
    )
    val thirdEntity: List<ThirdEntity>?,

    @Relation(
        parentColumn = "name",
        entityColumn = "name",
    )
    val thirdCompound: ThirdCompound?,

    @Relation(
        parentColumn = "",
        entityColumn = ""
    )
    val forthEntity: ForthEntity?
)