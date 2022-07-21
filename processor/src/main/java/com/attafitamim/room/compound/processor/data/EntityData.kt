package com.attafitamim.room.compound.processor.data

import com.attafitamim.room.compound.processor.data.info.TypeInfo
import com.attafitamim.room.compound.processor.data.info.PropertyInfo
import com.attafitamim.room.compound.processor.data.utility.EntityJunction

sealed interface EntityData {
    val typeInfo: TypeInfo
    val propertyInfo: PropertyInfo

    sealed interface Nested : EntityData {
        val isEmbedded: Boolean
        val junction: EntityJunction?
    }

    data class MainCompound(
        override val typeInfo: TypeInfo,
        override val propertyInfo: PropertyInfo,
        val entities: List<Nested>
    ) : EntityData

    data class Compound(
        override val typeInfo: TypeInfo,
        override val propertyInfo: PropertyInfo,
        override val isEmbedded: Boolean,
        override val junction: EntityJunction?,
        val entities: List<Nested>
    ) : Nested

    data class Entity(
        override val typeInfo: TypeInfo,
        override val propertyInfo: PropertyInfo,
        override val isEmbedded: Boolean,
        override val junction: EntityJunction?
    ) : Nested
}
