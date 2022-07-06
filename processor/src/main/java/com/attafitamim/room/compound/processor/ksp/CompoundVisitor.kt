package com.attafitamim.room.compound.processor.ksp

import com.attafitamim.room.compound.annotations.Compound
import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.generator.CompoundGenerator
import com.attafitamim.room.compound.processor.generator.syntax.EMBEDDED_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.ENTITY_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.RELATION_ANNOTATION
import com.attafitamim.room.compound.processor.generator.utils.throwException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class CompoundVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    private val compoundGenerator by lazy {
        CompoundGenerator(codeGenerator)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        try {
            val compoundData = getCompoundData(classDeclaration)
            compoundGenerator.generateDao(compoundData)
        } catch (e: Exception) {
        }
    }

    private fun getCompoundData(classDeclaration: KSClassDeclaration): CompoundData {
        if (classDeclaration.classKind != ClassKind.CLASS) logger.throwException(
            "Only classes can be annotated with ${Compound::class}",
            classDeclaration
        )

        val properties = classDeclaration.getAllProperties().iterator()
        if (!properties.hasNext()) logger.throwException(
            "A compound must at least have one embedded and one relation",
            classDeclaration
        )

        val childEntities = properties.toChildEntities()
        if (childEntities.isEmpty()) logger.throwException(
            "A compound must have at least one child with Relation annotation",
            classDeclaration
        )

        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()

        return CompoundData(
            packageName,
            className,
            childEntities
        )
    }

    private fun getCompound(
        classDeclaration: KSClassDeclaration,
        propertyDeclaration: KSPropertyDeclaration? = null
    ): EntityData.Compound {
        if (classDeclaration.classKind != ClassKind.CLASS) logger.throwException(
            "Only classes can be annotated with ${Compound::class}",
            classDeclaration
        )

        val properties = classDeclaration.getAllProperties().iterator()
        if (!properties.hasNext()) logger.throwException(
            "A compound must at least have one embedded and one relation",
            classDeclaration
        )

        val childEntities = properties.toChildEntities()
        if (childEntities.isEmpty()) logger.throwException(
            "A compound must have at least one child with Relation annotation",
            classDeclaration
        )

        val propertyName = propertyDeclaration?.simpleName?.getShortName().orEmpty()
        val isNullable = propertyDeclaration?.type?.resolve()?.isMarkedNullable ?: false

        return EntityData.Compound(
            propertyName,
            isNullable,
            isCollection = false,
            childEntities
        )
    }

    private fun getEntity(propertyDeclaration: KSPropertyDeclaration): EntityData.Entity {
        val propertyType = propertyDeclaration.type.resolve()

        val packageName = propertyType.declaration.packageName.asString()
        val className = propertyType.declaration.simpleName.asString()

        return EntityData.Entity(
            propertyDeclaration.simpleName.getShortName(),
            propertyType.isMarkedNullable,
            isCollection = false,
            packageName,
            className,
        )
    }

    private fun Iterator<KSPropertyDeclaration>.toChildEntities(): List<EntityData> {
        val childEntities = ArrayList<EntityData>()
        forEach { property ->
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

                childEntities.add(childEntity)
            }
        }

        return childEntities
    }
}
