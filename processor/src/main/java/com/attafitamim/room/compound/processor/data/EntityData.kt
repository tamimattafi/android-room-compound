package com.attafitamim.room.compound.processor.data

sealed interface EntityData {
    val propertyName: String
    val isNullable: Boolean
    val isCollection: Boolean

    data class Compound(
        override val propertyName: String,
        override val isNullable: Boolean,
        override val isCollection: Boolean,
        val entities: List<EntityData>
    ) : EntityData

    data class Entity(
        override val propertyName: String,
        override val isNullable: Boolean,
        override val isCollection: Boolean,
        val packageName: String,
        val className: String
    ) : EntityData
}
