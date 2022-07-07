package com.attafitamim.room.compound.processor.generator

import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.generator.syntax.COMPOUND_LIST_PARAMETER_NAME
import com.attafitamim.room.compound.processor.generator.syntax.DAO_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.DAO_POSTFIX
import com.attafitamim.room.compound.processor.generator.syntax.DAO_PREFIX
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_METHOD_NAME
import com.attafitamim.room.compound.processor.generator.syntax.IT_KEYWORD
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_CLOSE_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_OPEN_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_SEPARATOR
import com.attafitamim.room.compound.processor.generator.syntax.ROOM_PACKAGE
import com.attafitamim.room.compound.processor.generator.utils.createEntityParameterName
import com.attafitamim.room.compound.processor.generator.utils.createForEachSyntax
import com.attafitamim.room.compound.processor.generator.utils.createInitializationSyntax
import com.attafitamim.room.compound.processor.generator.utils.createInsertAnnotationSpec
import com.attafitamim.room.compound.processor.generator.utils.createListAdditionSyntax
import com.attafitamim.room.compound.processor.generator.utils.createPropertyAccessSyntax
import com.attafitamim.room.compound.processor.generator.utils.createSingleInsertFunction
import com.attafitamim.room.compound.processor.generator.utils.writeToFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class CompoundGenerator(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>
) {

    private val isSuspendDao: Boolean get() =
        options["suspendDao"] == "true"

    private val useDaoPrefix: Boolean get() =
        options["useDaoPrefix"] == "true"

    private val useDaoPostfix: Boolean get() =
        options["useDaoPostfix"] == "true"

    fun generateDao(compoundData: CompoundData) {
        val fileName = buildString {
            if (useDaoPrefix) append(DAO_PREFIX)
            append(compoundData.className)
            if (useDaoPostfix) append(DAO_POSTFIX)
        }

        val compoundClassName = ClassName(compoundData.packageName, compoundData.className)

        val compoundListName = Collection::class.asClassName()
            .parameterizedBy(compoundClassName)

        val listInsertFunctionBuilder = FunSpec.builder(INSERT_METHOD_NAME)
            .addParameter(COMPOUND_LIST_PARAMETER_NAME, compoundListName)

        if (isSuspendDao) listInsertFunctionBuilder.addModifiers(KModifier.SUSPEND)

        val listInitializationBlock = CodeBlock.builder()
        val listMappingBlock = CodeBlock.builder()
        val insertBlock = CodeBlock.builder()
        val insertMethodCall = buildString {
            append(
                INSERT_METHOD_NAME,
                PARAMETER_OPEN_PARENTHESIS
            )
        }

        insertBlock.addStatement(insertMethodCall)

        fun addForEachStatement(parameter: String) {
            val forEachSyntax = createForEachSyntax(parameter)
            listMappingBlock.beginControlFlow(forEachSyntax)
        }

        fun addSetInitializeStatement(entityData: EntityData.Entity, parameterName: String) {
            val entityClassName = ClassName(entityData.packageName, entityData.className)
            val parentSetName = HashSet::class.asClassName().parameterizedBy(entityClassName)

            val initializationSyntax = createInitializationSyntax(parameterName)
            listInitializationBlock.addStatement(initializationSyntax, parentSetName)
        }

        addForEachStatement(COMPOUND_LIST_PARAMETER_NAME)

        val insertAnnotation = createInsertAnnotationSpec()
        val entityInsertFunctionBuilder = FunSpec.builder(INSERT_METHOD_NAME)
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(insertAnnotation)

        if (isSuspendDao) entityInsertFunctionBuilder.addModifiers(KModifier.SUSPEND)

        fun handleEntity(
            entityData: EntityData,
            parents: List<EntityData> = listOf(),
            accessParents: List<EntityData> = listOf()
        ) {
            when (entityData) {
                is EntityData.Compound -> {
                    if (entityData.isCollection) {
                        val propertyAccessSyntax = createPropertyAccessSyntax(
                            IT_KEYWORD,
                            parents,
                            entityData.propertyName
                        )

                        addForEachStatement(propertyAccessSyntax.properAccess)

                        entityData.entities.forEach { compoundEntityData ->
                            handleEntity(
                                compoundEntityData,
                                parents + entityData,
                                listOf()
                            )
                        }

                        listMappingBlock.endControlFlow()
                    } else entityData.entities.forEach { compoundEntityData ->
                        handleEntity(
                            compoundEntityData,
                            parents + entityData,
                            accessParents + entityData
                        )
                    }
                }

                is EntityData.Entity -> {
                    val parameterName = createEntityParameterName(entityData, parents)
                    addSetInitializeStatement(entityData, parameterName)
                    val parameterWithSeparator = buildString {
                        append(
                            parameterName,
                            PARAMETER_SEPARATOR
                        )
                    }

                    insertBlock.addStatement(parameterWithSeparator)
                    val propertyAccessSyntax = createPropertyAccessSyntax(
                        IT_KEYWORD,
                        accessParents,
                        entityData.propertyName
                    )

                    val listAdditionSyntax = createListAdditionSyntax(
                        parameterName,
                        propertyAccessSyntax,
                        entityData.isCollection
                    )

                    listMappingBlock.addStatement(listAdditionSyntax)

                    val entityClassName = ClassName(entityData.packageName, entityData.className)

                    val parentListName = Collection::class.asClassName()
                        .parameterizedBy(entityClassName)

                    entityInsertFunctionBuilder.addParameter(parameterName, parentListName)
                }
            }
        }

        compoundData.entities.forEach { entityData ->
            handleEntity(entityData)
        }

        insertBlock.addStatement(PARAMETER_CLOSE_PARENTHESIS)

        listInsertFunctionBuilder
            .addCode(listInitializationBlock.build())
            .addCode(listMappingBlock.endControlFlow().build())
            .addCode(insertBlock.build())

        val listInsertFunction = listInsertFunctionBuilder.build()

        val singleInsertFunction = createSingleInsertFunction(
            compoundClassName,
            listInsertFunction,
            isSuspendDao
        )

        val daoAnnotation = ClassName(
            ROOM_PACKAGE,
            DAO_ANNOTATION
        )

        val interfaceSpec = TypeSpec.interfaceBuilder(fileName)
            .addAnnotation(daoAnnotation)
            .addFunction(singleInsertFunction)
            .addFunction(listInsertFunction)
            .addFunction(entityInsertFunctionBuilder.build())
            .build()

        val fileSpec = FileSpec.builder(compoundData.packageName, fileName)
            .addType(interfaceSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            compoundData.packageName,
            fileName
        )

        fileSpec.writeToFile(outputFile)
    }
}