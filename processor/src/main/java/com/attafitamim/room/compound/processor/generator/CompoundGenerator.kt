package com.attafitamim.room.compound.processor.generator

import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.data.info.PropertyInfo
import com.attafitamim.room.compound.processor.generator.syntax.*
import com.attafitamim.room.compound.processor.utils.*
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class CompoundGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) {

    private val isSuspendDao: Boolean get() =
        options["suspendDao"] == "true"

    private val useDaoPrefix: Boolean get() =
        options["useDaoPrefix"] == "true"

    private val useDaoPostfix: Boolean get() =
        options["useDaoPostfix"] == "true"

    fun generateDao(compoundData: EntityData.MainCompound) {
        val fileName = buildString {
            if (useDaoPrefix) append(DAO_PREFIX)
            append(compoundData.typeInfo.className)
            if (useDaoPostfix) append(DAO_POSTFIX)
        }

        val compoundClassName = ClassName(
            compoundData.typeInfo.packageName,
            compoundData.typeInfo.className
        )

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

        val fileSpec = FileSpec.builder(compoundData.typeInfo.packageName, fileName)
            .addType(interfaceSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            compoundData.typeInfo.packageName,
            fileName
        )

        fileSpec.writeToFile(outputFile)
    }

    private fun createEntityInsertFunction(
        compoundData: EntityData.MainCompound
    ): FunSpec {
        val insertAnnotation = createInsertAnnotationSpec()
        val functionBuilder = FunSpec.builder(INSERT_ENTITIES_METHOD_NAME)
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(insertAnnotation)

        if (isSuspendDao) functionBuilder.addModifiers(KModifier.SUSPEND)

        val presentEntities = HashSet<String>()
        fun addInsertParameter(packageName: String, className: String) {
            val entityName = titleToCamelCase(className)
            if (presentEntities.contains(entityName)) return
            presentEntities.add(entityName)

            val entityClassName = ClassName(packageName, className)

            val entityListName = Collection::class.asClassName()
                .parameterizedBy(entityClassName)

            val parameter = ParameterSpec.builder(entityName, entityListName)
                .build()

            functionBuilder.addParameter(parameter)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.MainCompound -> entityData.entities.forEach { childEntity ->
                    handleEntity(childEntity)
                }

                is EntityData.Nested -> {
                    entityData.junction?.let { junction ->
                        addInsertParameter(junction.packageName, junction.className)
                    }

                    when (entityData) {
                        is EntityData.Compound -> entityData.entities.forEach { childEntity ->
                            handleEntity(childEntity)
                        }

                        is EntityData.Entity -> addInsertParameter(
                            entityData.typeInfo.packageName,
                            entityData.typeInfo.className
                        )
                    }
                }
            }
        }

        handleEntity(compoundData)
        return functionBuilder.build()
    }

    private fun createListMappingBlock(
        compoundData: EntityData.MainCompound
    ): CodeBlock {
        val codeBlockBuilder = CodeBlock.builder()

        fun addForEachStatement(
            parameter: String,
            itemName: String,
            isNullable: Boolean = false
        ) {
            val forEachSyntax = createForEachSyntax(parameter, itemName, isNullable)
            codeBlockBuilder.beginControlFlow(forEachSyntax)
        }

        val itemName = createListItemName(compoundData.typeInfo.className)
        addForEachStatement(COMPOUND_LIST_PARAMETER_NAME, itemName)

        fun handleJunction(
            entityData: EntityData.Nested,
            parents: List<EntityData>,
            accessProperties: List<PropertyInfo>
        ) {
            val junction = entityData.junction
            if (junction != null) {
                val listItemName = createListItemName(entityData.typeInfo.className)

                val junctionClassName = ClassName(junction.packageName, junction.className)
                val junctionListName = titleToCamelCase(junction.className)

                val entityAccessSyntax = createPropertyAccessSyntax(
                    accessProperties,
                    entityData.propertyInfo
                )

                addForEachStatement(
                    entityAccessSyntax.chain,
                    listItemName,
                    entityAccessSyntax.handleNullability
                )

                codeBlockBuilder.addStatement(buildString {
                    append(junctionListName, ".add(")
                    append("%T(")
                    append(junction.junctionParentColumn, " = ", "\"\"", ", ")
                    append(junction.junctionEntityColumn, " = ", "\"\"", ")")
                    append(")")
                }, junctionClassName)

                codeBlockBuilder.endControlFlow()
            }
        }

        fun handleEntity(
            entityData: EntityData,
            parents: List<EntityData> = listOf(),
            accessProperties: List<PropertyInfo> = listOf()
        ) {
            when (entityData) {
                is EntityData.MainCompound -> entityData.entities.forEach { compoundEntityData ->
                    handleEntity(
                        compoundEntityData,
                        parents + entityData,
                        accessProperties + entityData.propertyInfo
                    )
                }

                is EntityData.Nested -> when(entityData) {
                    is EntityData.Compound -> {
                        if (entityData.propertyInfo.isCollection) {
                            val propertyAccessSyntax = createPropertyAccessSyntax(
                                accessProperties,
                                entityData.propertyInfo
                            )

                            val listItemName = createListItemName(entityData.typeInfo.className)
                            addForEachStatement(
                                propertyAccessSyntax.chain,
                                listItemName,
                                propertyAccessSyntax.handleNullability
                            )

                            val listPropertyInfo = PropertyInfo(
                                listItemName,
                                isNullable = false,
                                isCollection = false
                            )

                            entityData.entities.forEach { compoundEntityData ->
                                handleEntity(
                                    compoundEntityData,
                                    parents + entityData,
                                    listOf(listPropertyInfo)
                                )
                            }

                            codeBlockBuilder.endControlFlow()
                        } else entityData.entities.forEach { compoundEntityData ->
                            handleEntity(
                                compoundEntityData,
                                parents + entityData,
                                accessProperties + entityData.propertyInfo
                            )
                        }
                    }

                    is EntityData.Entity -> {
                        handleJunction(entityData, parents, accessProperties)

                        val parameterName = titleToCamelCase(entityData.typeInfo.className)

                        val propertyAccessSyntax = createPropertyAccessSyntax(
                            accessProperties,
                            entityData.propertyInfo
                        )

                        val listAdditionSyntax = createListAdditionSyntax(
                            parameterName,
                            propertyAccessSyntax,
                            entityData.propertyInfo.isCollection
                        )

                        codeBlockBuilder.addStatement(listAdditionSyntax)
                    }
                }
            }
        }

        handleEntity(compoundData)
        return codeBlockBuilder.endControlFlow().build()
    }

    private fun createListInsertBlock(
        compoundData: EntityData.MainCompound
    ): CodeBlock {
        val presentEntities = HashSet<String>()
        val codeBlockBuilder = CodeBlock.builder()

        val insertMethodCall = buildString {
            append(INSERT_ENTITIES_METHOD_NAME, PARAMETER_OPEN_PARENTHESIS)
        }

        codeBlockBuilder.addStatement(insertMethodCall)

        fun addInsertParameter(className: String) {
            val entityName = titleToCamelCase(className)
            if (presentEntities.contains(entityName)) return
            presentEntities.add(entityName)

            val parameterWithSeparator = buildString {
                append(entityName, PARAMETER_SEPARATOR)
            }

            codeBlockBuilder.addStatement(parameterWithSeparator)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.MainCompound -> entityData.entities.forEach(::handleEntity)


                is EntityData.Nested -> {
                    entityData.junction?.let { junction ->
                        addInsertParameter(junction.className)
                    }

                    when (entityData) {
                        is EntityData.Compound -> entityData.entities.forEach(::handleEntity)

                        is EntityData.Entity -> {
                            addInsertParameter(entityData.typeInfo.className)
                        }
                    }
                }
            }
        }

        handleEntity(compoundData)
        return codeBlockBuilder.addStatement(PARAMETER_CLOSE_PARENTHESIS).build()
    }

    private fun createListItemName(
        className: String,
    ): String {
        val camelClassName = titleToCamelCase(className)
        return buildString { append(camelClassName, LIST_ITEM_POSTFIX) }
    }

    private fun createListInitializationBlock(
        compoundData: EntityData.MainCompound
    ): CodeBlock {
        val presentEntityLists = HashSet<String>()
        val codeBlockBuilder = CodeBlock.builder()

        fun addListInitializationStatement(packageName: String, className: String) {
            val listName = titleToCamelCase(className)
            if (presentEntityLists.contains(listName)) return
            presentEntityLists.add(listName)

            val entityClassName = ClassName(packageName, className)
            val parentSetName = HashSet::class.asClassName().parameterizedBy(entityClassName)

            val initializationSyntax = createInitializationSyntax(listName)
            codeBlockBuilder.addStatement(initializationSyntax, parentSetName)
        }

        fun handleEntity(entityData: EntityData) {
            when (entityData) {
                is EntityData.MainCompound -> entityData.entities.forEach(::handleEntity)

                is EntityData.Nested -> {
                    entityData.junction?.let { junction ->
                        addListInitializationStatement(junction.packageName, junction.className)
                    }

                    when (entityData) {
                        is EntityData.Compound -> entityData.entities.forEach(::handleEntity)

                        is EntityData.Entity -> addListInitializationStatement(
                            entityData.typeInfo.packageName,
                            entityData.typeInfo.className
                        )
                    }
                }
            }
        }

        handleEntity(compoundData)
        return codeBlockBuilder.build()
    }

    private fun createPropertyAccessSyntax(
        accessProperties: List<PropertyInfo>,
        property: PropertyInfo
    ): PropertyAccessSyntax {
        var handleNullability = false
        val propertyAccess = buildString {
            accessProperties.forEach { accessProperty ->
                val name = if (accessProperty.isCollection) createListItemName(accessProperty.name)
                else accessProperty.name

                append(name)

                handleNullability = handleNullability || accessProperty.isNullable
                if (handleNullability) append(NULLABLE_SIGN)
                append(INSTANCE_ACCESS_KEY)
            }

            append(property.name)
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