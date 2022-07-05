package com.attafitamim.room.compound.processor.ksp

import com.attafitamim.room.compound.annotations.Compound
import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.generator.CompoundGenerator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import java.io.OutputStream

class CompoundVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    val compoundGenerator by lazy {
        CompoundGenerator(codeGenerator)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val compoundData = getCompoundData(classDeclaration) ?: kotlin.run {
            logger.error("Error parsing compound data", classDeclaration)
            return
        }

        compoundGenerator.generateDao(compoundData)
    }

    private fun getCompoundData(classDeclaration: KSClassDeclaration): CompoundData? {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.error(
                "Only classes can be annotated with ${Compound::class}",
                classDeclaration
            )

            return null
        }

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        val properties = classDeclaration.getAllProperties()
            .iterator()

        if (!properties.hasNext()) {
            logger.error(
                "A compound must at least have one embedded and one relation",
                classDeclaration
            )

            return null
        }

        val childEntities = ArrayList<EntityData>()

        properties.forEach { property ->
            val propertyType = property.type.resolve().declaration as KSClassDeclaration

            val isValidEntity = property.annotations.any { annotation ->
                val shortName = annotation.shortName.getShortName()
                shortName == RELATION_ANNOTATION || shortName == EMBEDDED_ANNOTATION
            }

            if (isValidEntity) {
                val isEntity = propertyType.annotations.any { annotation ->
                    annotation.shortName.getShortName() == ENTITY_ANNOTATION
                }

                val childEntity = if (!isEntity) getCompound(propertyType, property)
                else getEntity(property)

                if (childEntity == null) return null

                childEntities.add(childEntity)
                return@forEach
            }
        }

        if (childEntities.isEmpty()) {
            logger.error(
                "A compound must have at least one child with Relation annotation",
                classDeclaration
            )

            return null
        }


        return CompoundData(
            packageName,
            className,
            childEntities
        )
    }

    private fun getCompound(
        classDeclaration: KSClassDeclaration,
        propertyDeclaration: KSPropertyDeclaration? = null
    ): EntityData.Compound? {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.error(
                "Only classes can be annotated with ${Compound::class}",
                classDeclaration
            )

            return null
        }

        val properties = classDeclaration.getAllProperties()
            .iterator()

        if (!properties.hasNext()) {
            logger.error(
                "A compound must at least have one embedded and one relation",
                classDeclaration
            )

            return null
        }

        val childEntities = ArrayList<EntityData>()

        properties.forEach { property ->
            val propertyType = property.type.resolve().declaration as KSClassDeclaration

            val isValidEntity = property.annotations.any { annotation ->
                val shortName = annotation.shortName.getShortName()
                shortName == RELATION_ANNOTATION || shortName == EMBEDDED_ANNOTATION
            }

            if (isValidEntity) {
                val isEntity = propertyType.annotations.any { annotation ->
                    annotation.shortName.getShortName() == ENTITY_ANNOTATION
                }

                val childEntity = if (!isEntity) getCompound(propertyType, property)
                else getEntity(property)

                if (childEntity == null) return null

                childEntities.add(childEntity)
                return@forEach
            }
        }

        if (childEntities.isEmpty()) {
            logger.error(
                "A compound must have at least one child with Relation annotation",
                classDeclaration
            )

            return null
        }

        val propertyName = propertyDeclaration?.simpleName?.getShortName().orEmpty()
        val isNullable = propertyDeclaration?.type?.resolve()?.isMarkedNullable ?: false
        return EntityData.Compound(
            propertyName,
            isNullable,
            childEntities
        )
    }

    private fun getEntity(propertyDeclaration: KSPropertyDeclaration): EntityData.Entity {
        val propertyType = propertyDeclaration.type.resolve()

        val packageName = propertyType.declaration.packageName.asString()
        val className = propertyType.declaration.simpleName.asString()

        return EntityData.Entity(
            packageName,
            className,
            propertyDeclaration.simpleName.getShortName(),
            propertyType.isMarkedNullable
        )
    }

    companion object {
        const val EMBEDDED_ANNOTATION = "Embedded"
        const val RELATION_ANNOTATION = "Relation"
        const val ENTITY_ANNOTATION = "Entity"
        const val DAO_ANNOTATION = "Dao"
        const val INSERT_ANNOTATION = "Insert"

        const val CONFLICT_STRATEGY = "OnConflictStrategy"
        const val CONFLICT_STRATEGY_REPLACE = "REPLACE"

        const val ROOM_PACKAGE = "androidx.room"
    }
}
