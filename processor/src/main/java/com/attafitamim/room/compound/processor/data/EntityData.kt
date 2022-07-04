package com.attafitamim.room.compound.processor.data

sealed interface EntityData {
    val propertyName: String
    val isNullable: Boolean

    data class Compound(
        override val propertyName: String,
        override val isNullable: Boolean,
        val entities: List<EntityData>
    ) : EntityData

    data class Entity(
        val packageName: String,
        val className: String,
        override val propertyName: String,
        override val isNullable: Boolean
    ) : EntityData
}
