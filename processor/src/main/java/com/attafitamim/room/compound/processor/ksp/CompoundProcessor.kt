package com.attafitamim.room.compound.processor.ksp

import com.attafitamim.room.compound.annotations.Compound
import com.attafitamim.room.compound.processor.data.EntityData
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class CompoundProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(Compound::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val symbolsIterator = symbols.iterator()

        if (!symbolsIterator.hasNext()) return emptyList()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
            packageName = "com.attafitamim",
            fileName = "GeneratedFunctions"
        ).writer()

        val compounds = ArrayList<EntityData.Compound>()
        symbolsIterator.forEach { classDeclaration ->
            val compoundData = getCompoundData(classDeclaration) ?: kotlin.run {
                logger.error("Error parsing compound data", classDeclaration)
                return emptyList()
            }
            compounds.add(compoundData)
            file.write("$compoundData\n")
        }

        file.apply {
            flush()
            close()
        }

        return emptyList()
    }

    private fun getCompoundData(classDeclaration: KSClassDeclaration): EntityData.Compound? {
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

        var parentEntity: EntityData? = null
        val childEntities = ArrayList<EntityData>()

        properties.forEach { propertyDeclaration ->
            val propertyType = propertyDeclaration.type.resolve().declaration as KSClassDeclaration

            val isParentAnnotation = propertyDeclaration.annotations.any { annotation ->
                annotation.shortName.getShortName() == EMBEDDED_ANNOTATION
            }

            if (isParentAnnotation) {
                if (parentEntity != null) {
                    logger.error(
                        "A compound can't have multiple embedded parents",
                        propertyDeclaration
                    )
                }

                val isEntity = propertyType.annotations.any { annotation ->
                    annotation.shortName.getShortName() == ENTITY_ANNOTATION
                }

                parentEntity = if (!isEntity) getCompoundData(propertyType)
                else getEntityData(propertyDeclaration)

                if (parentEntity == null) return null
                return@forEach
            }

            val isChildAnnotation = propertyDeclaration.annotations.any { annotation ->
                annotation.shortName.getShortName() == RELATION_ANNOTATION
            }

            if (isChildAnnotation) {
                val isEntity = propertyType.annotations.any { annotation ->
                    annotation.shortName.getShortName() == ENTITY_ANNOTATION
                }

                val childEntity = if (!isEntity) getCompoundData(propertyType)
                else getEntityData(propertyDeclaration)

                if (childEntity == null) return null

                childEntities.add(childEntity)
                return@forEach
            }
        }

        val actualParent = parentEntity ?: kotlin.run {
            logger.error(
                "A compound must have at least one parent with Embedded annotation",
                classDeclaration
            )

            return null
        }

        if (childEntities.isEmpty()) {
            logger.error(
                "A compound must have at least one child with Relation annotation",
                classDeclaration
            )

            return null
        }

        return EntityData.Compound(
            packageName,
            className,
            actualParent,
            childEntities
        )
    }

    private fun getEntityData(propertyDeclaration: KSPropertyDeclaration): EntityData.Entity {
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
    }
}
