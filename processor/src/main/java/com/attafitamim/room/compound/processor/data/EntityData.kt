package com.attafitamim.room.compound.processor.data

sealed interface EntityData {
    val packageName: String
    val className: String

    data class Compound(
        override val packageName: String,
        override val className: String,
        val parentEntity: EntityData,
        val childEntities: List<EntityData>
    ) : EntityData

    data class Entity(
        override val packageName: String,
        override val className: String,
        val isNullable: Boolean
    ) : EntityData
}
