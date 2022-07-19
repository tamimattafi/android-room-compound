package com.attafitamim.room.compound.processor.data

data class EntityJunction(
    val packageName: String,
    val className: String,
    val parentColumn: String,
    val entityColumn: String,
    val junctionParentColumn: String,
    val junctionEntityColumn: String
)