package com.attafitamim.room.compound.processor.data

sealed interface EntityData {
    val propertyName: String
    val isNullable: Boolean
    val isEmbedded: Boolean
    val isCollection: Boolean
    val junction: EntityJunction?

    data class Compound(
        override val propertyName: String,
        override val isNullable: Boolean,
        override val isEmbedded: Boolean,
        override val isCollection: Boolean,
        override val junction: EntityJunction?,
        val entities: List<EntityData>
    ) : EntityData

    data class Entity(
        override val propertyName: String,
        override val isNullable: Boolean,
        override val isEmbedded: Boolean,
        override val isCollection: Boolean,
        override val junction: EntityJunction?,
        val packageName: String,
        val className: String
    ) : EntityData
}
