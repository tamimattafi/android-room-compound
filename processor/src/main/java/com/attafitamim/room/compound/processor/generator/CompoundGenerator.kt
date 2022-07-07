package com.attafitamim.room.compound.processor.generator

import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.generator.syntax.*
import com.attafitamim.room.compound.processor.generator.utils.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

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

        val listInitializationBlock = createListInitializationBlock(compoundData)
        val insertBlock = createListInsertBlock(compoundData)
        val listMappingBlock = createListMappingBlock(compoundData)
        val entityInsertFunctionBuilder = createEntityInsertFunction(compoundData)

        listInsertFunctionBuilder
            .addCode(listInitializationBlock)
            .addCode(listMappingBlock)
            .addCode(insertBlock)

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
            .addFunction(entityInsertFunctionBuilder)
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

    private fun createEntityInsertFunction(
        compoundData: CompoundData
    ): FunSpec {
        val insertAnnotation = createInsertAnnotationSpec()
        val functionBuilder = FunSpec.builder(INSERT_ENTITIES_METHOD_NAME)
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(insertAnnotation)

        if (isSuspendDao) functionBuilder.addModifiers(KModifier.SUSPEND)

        val presentEntities = HashSet<String>()
        fun addInsertParameter(entityData: EntityData.Entity) {
            val entityName = titleToCamelCase(entityData.className)
            if (presentEntities.contains(entityName)) return
            presentEntities.add(entityName)

            val entityClassName = ClassName(entityData.packageName, entityData.className)

            val entityListName = Collection::class.asClassName()
                .parameterizedBy(entityClassName)

            val parameter = ParameterSpec.builder(entityName, entityListName)
                .build()

            functionBuilder.addParameter(parameter)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.Compound -> entityData.entities.forEach { childEntity ->
                    handleEntity(childEntity)
                }

                is EntityData.Entity -> addInsertParameter(entityData)
            }
        }

        compoundData.entities.forEach(::handleEntity)
        return functionBuilder.build()
    }

    private fun createListMappingBlock(
        compoundData: CompoundData
    ): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        fun addForEachStatement(parameter: String, isNullable: Boolean = false) {
            val forEachSyntax = createForEachSyntax(parameter, isNullable)
            codeBlockBuilder.beginControlFlow(forEachSyntax)
        }

        addForEachStatement(COMPOUND_LIST_PARAMETER_NAME)

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
                            accessParents,
                            entityData
                        )

                        addForEachStatement(
                            propertyAccessSyntax.properAccess,
                            propertyAccessSyntax.handleNullability
                        )

                        entityData.entities.forEach { compoundEntityData ->
                            handleEntity(
                                compoundEntityData,
                                parents + entityData,
                                listOf()
                            )
                        }

                        codeBlockBuilder.endControlFlow()
                    } else entityData.entities.forEach { compoundEntityData ->
                        handleEntity(
                            compoundEntityData,
                            parents + entityData,
                            accessParents + entityData
                        )
                    }
                }

                is EntityData.Entity -> {
                    val parameterName = titleToCamelCase(entityData.className)

                    val propertyAccessSyntax = createPropertyAccessSyntax(
                        IT_KEYWORD,
                        accessParents,
                        entityData
                    )

                    val listAdditionSyntax = createListAdditionSyntax(
                        parameterName,
                        propertyAccessSyntax,
                        entityData.isCollection
                    )

                    codeBlockBuilder.addStatement(listAdditionSyntax)
                }
            }
        }

        compoundData.entities.forEach(::handleEntity)
        return codeBlockBuilder.endControlFlow().build()
    }

    private fun createListInsertBlock(
        compoundData: CompoundData
    ): CodeBlock {
        val presentEntities = HashSet<String>()
        val codeBlockBuilder = CodeBlock.builder()

        val insertMethodCall = buildString {
            append(INSERT_ENTITIES_METHOD_NAME, PARAMETER_OPEN_PARENTHESIS)
        }

        codeBlockBuilder.addStatement(insertMethodCall)

        fun addInsertParameter(entityData: EntityData.Entity) {
            val entityName = titleToCamelCase(entityData.className)
            if (presentEntities.contains(entityName)) return
            presentEntities.add(entityName)

            val parameterWithSeparator = buildString {
                append(entityName, PARAMETER_SEPARATOR)
            }

            codeBlockBuilder.addStatement(parameterWithSeparator)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.Compound -> entityData.entities.forEach { childEntity ->
                    handleEntity(childEntity)
                }

                is EntityData.Entity -> addInsertParameter(entityData)
            }
        }

        compoundData.entities.forEach(::handleEntity)

        return codeBlockBuilder.addStatement(PARAMETER_CLOSE_PARENTHESIS).build()
    }

    private fun createListInitializationBlock(
        compoundData: CompoundData
    ): CodeBlock {
        val presentEntityLists = HashSet<String>()
        val codeBlockBuilder = CodeBlock.builder()

        fun addListInitializationStatement(entityData: EntityData.Entity) {
            val listName = titleToCamelCase(entityData.className)
            if (presentEntityLists.contains(listName)) return
            presentEntityLists.add(listName)

            val entityClassName = ClassName(entityData.packageName, entityData.className)
            val parentSetName = HashSet::class.asClassName().parameterizedBy(entityClassName)

            val initializationSyntax = createInitializationSyntax(listName)
            codeBlockBuilder.addStatement(initializationSyntax, parentSetName)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.Compound -> entityData.entities.forEach { childEntity ->
                    handleEntity(childEntity)
                }

                is EntityData.Entity -> addListInitializationStatement(entityData)
            }
        }

        compoundData.entities.forEach(::handleEntity)

        return codeBlockBuilder.build()
    }

    private fun createPropertyAccessSyntax(
        parent: String,
        parents: List<EntityData>,
        property: EntityData
    ): PropertyAccessSyntax {
        var handleNullability = false
        val propertyAccess = buildString {
            append(parent, INSTANCE_ACCESS_KEY)

            parents.forEach { entityData ->
                append(entityData.propertyName)
                handleNullability = handleNullability || entityData.isNullable
                if (handleNullability) append(NULLABLE_SIGN)
                append(INSTANCE_ACCESS_KEY)
            }

            append(property.propertyName)
        }

        handleNullability = handleNullability || property.isNullable
        return PropertyAccessSyntax(propertyAccess, handleNullability)
    }

    private fun createSingleInsertFunction(
        compoundClassName: ClassName,
        listInsertFunction: FunSpec,
        isSuspend: Boolean
    ): FunSpec {
        val listCreationMethodCall = createMethodCallSyntax(
            LIST_OF_METHOD,
            COMPOUND_PARAMETER_NAME
        )

        val insertMethodCall = createMethodCallSyntax(
            INSERT_METHOD_NAME,
            listCreationMethodCall
        )

        val functionBuilder = FunSpec.builder(INSERT_METHOD_NAME)
            .addParameter(COMPOUND_PARAMETER_NAME, compoundClassName)
            .addStatement(insertMethodCall, listInsertFunction)

        if (isSuspend) functionBuilder.addModifiers(KModifier.SUSPEND)

        return functionBuilder.build()
    }

    private fun createInsertAnnotationSpec(): AnnotationSpec {
        val insertAnnotationClassName = ClassName(
            ROOM_PACKAGE,
            INSERT_ANNOTATION
        )

        val conflictStrategyReplaceName = ClassName(
            ROOM_PACKAGE,
            listOf(
                CONFLICT_STRATEGY,
                CONFLICT_STRATEGY_REPLACE
            )
        )

        val onConflictAssignment = createAssignSyntax(ON_CONFLICT_PARAMETER)
        return AnnotationSpec.builder(insertAnnotationClassName)
            .addMember(onConflictAssignment, conflictStrategyReplaceName)
            .build()
    }
}