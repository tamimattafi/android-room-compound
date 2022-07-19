package com.attafitamim.room.compound.processor.ksp

import com.attafitamim.room.compound.annotations.Compound
import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.data.EntityJunction
import com.attafitamim.room.compound.processor.generator.CompoundGenerator
import com.attafitamim.room.compound.processor.generator.syntax.EMBEDDED_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.ENTITY_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.JUNCTION_CLASS_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.JUNCTION_ENTITY_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.JUNCTION_PARENT_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.RELATION_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.RELATION_ENTITY_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.RELATION_JUNCTION_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.RELATION_PARENT_PARAMETER
import com.attafitamim.room.compound.processor.utils.stringValue
import com.attafitamim.room.compound.processor.utils.throwException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid

class CompoundVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : KSVisitorVoid() {

    private val compoundGenerator by lazy {
        CompoundGenerator(codeGenerator, options)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val compoundData = getCompoundData(classDeclaration)
        compoundGenerator.generateDao(compoundData)
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
        typeClassDeclaration: KSClassDeclaration,
        propertyDeclaration: KSPropertyDeclaration? = null,
        isEmbedded: Boolean,
        isCollection: Boolean = false,
        junction: EntityJunction? = null
    ): EntityData.Compound {
        if (typeClassDeclaration.classKind != ClassKind.CLASS) logger.throwException(
            "Only classes can be annotated with ${Compound::class}",
            typeClassDeclaration
        )

        val properties = typeClassDeclaration.getAllProperties().iterator()
        if (!properties.hasNext()) logger.throwException(
            "A compound must at least have one embedded and one relation",
            typeClassDeclaration
        )

        val childEntities = properties.toChildEntities()
        if (childEntities.isEmpty()) logger.throwException(
            "A compound must have at least one child with Relation annotation",
            typeClassDeclaration
        )

        val propertyName = propertyDeclaration?.simpleName?.getShortName().orEmpty()
        val isNullable = propertyDeclaration?.type?.resolve()?.isMarkedNullable ?: false

        return EntityData.Compound(
            propertyName,
            isNullable,
            isEmbedded,
            isCollection,
            junction,
            childEntities
        )
    }

    private fun getEntity(
        typeClassDeclaration: KSClassDeclaration,
        propertyDeclaration: KSPropertyDeclaration,
        isEmbedded: Boolean,
        isCollection: Boolean = false,
        junction: EntityJunction? = null
    ): EntityData.Entity {
        val packageName = typeClassDeclaration.packageName.asString()
        val className = typeClassDeclaration.simpleName.asString()

        return EntityData.Entity(
            propertyDeclaration.simpleName.getShortName(),
            propertyDeclaration.type.resolve().isMarkedNullable,
            isEmbedded,
            isCollection,
            junction,
            packageName,
            className,
        )
    }

    private fun Iterator<KSPropertyDeclaration>.toChildEntities(): List<EntityData> {
        val childEntities = ArrayList<EntityData>()
        forEach { property ->
            val propertyType = property.type.resolve().declaration as KSClassDeclaration

            val isEmbedded = property.annotations.any { annotation ->
                val shortName = annotation.shortName.getShortName()
                shortName == EMBEDDED_ANNOTATION
            }

            val isRelation = property.annotations.any { annotation ->
                val shortName = annotation.shortName.getShortName()
                shortName == RELATION_ANNOTATION
            }

            val isValidEntity = isEmbedded || isRelation

            if (isValidEntity) {
                val collectionName = Collection::class.simpleName
                val isCollection = propertyType.simpleName.getShortName() == collectionName ||
                    propertyType.superTypes.any { superType ->
                        superType.resolve().declaration.simpleName.getShortName() == collectionName
                    }

                val childEntity = when {
                    isCollection -> {
                        val junction = if (isRelation) property.getEntityJunction()
                        else null

                        val elementType = property.type.resolve()
                            .arguments
                            .first()
                            .type.let(::requireNotNull)
                            .resolve()
                            .declaration as KSClassDeclaration

                        val isElementEntity = elementType.annotations.any { annotation ->
                            annotation.shortName.getShortName() == ENTITY_ANNOTATION
                        }

                        if (!isElementEntity) getCompound(
                            elementType,
                            property,
                            isEmbedded,
                            isCollection,
                            junction
                        ) else getEntity(
                            elementType,
                            property,
                            isEmbedded,
                            isCollection,
                            junction
                        )
                    }

                    else -> {
                        val isEntity = propertyType.annotations.any { annotation ->
                            annotation.shortName.getShortName() == ENTITY_ANNOTATION
                        }

                        if (!isEntity) getCompound(propertyType, property, isEmbedded)
                        else getEntity(propertyType, property, isEmbedded)
                    }
                }

                childEntities.add(childEntity)
            }
        }

        return childEntities
    }

    private fun KSPropertyDeclaration.getEntityJunction(): EntityJunction? {
        val relationAnnotation = annotations.firstOrNull { annotation ->
            val shortName = annotation.shortName.getShortName()
            shortName == RELATION_ANNOTATION
        } ?: return null

        val relationArguments = relationAnnotation.arguments.associateBy { valueArgument ->
            valueArgument.name?.getShortName()
        }

        val junctionAnnotation = relationArguments[RELATION_JUNCTION_PARAMETER]?.value
                as? KSAnnotation ?: return null

        val junctionArguments = junctionAnnotation.arguments.associateBy { valueArgument ->
            valueArgument.name?.getShortName()
        }

        val junctionType = junctionArguments[JUNCTION_CLASS_PARAMETER]?.value
                as? KSType ?: return null

        val junctionClass = junctionType.declaration as? KSClassDeclaration ?: return null

        if (junctionType.declaration.simpleName.getShortName() == Object::class.java.simpleName)
            return null

        val packageName = junctionClass.packageName.asString()
        val className = junctionClass.simpleName.asString()

        return EntityJunction(
            packageName,
            className,
            relationArguments.getValue(RELATION_PARENT_PARAMETER).stringValue,
            relationArguments.getValue(RELATION_ENTITY_PARAMETER).stringValue,
            junctionArguments.getValue(JUNCTION_PARENT_PARAMETER).stringValue,
            junctionArguments.getValue(JUNCTION_ENTITY_PARAMETER).stringValue
        )
    }
}
