package com.attafitamim.room.compound.processor.generator

import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.data.info.PropertyInfo
import com.attafitamim.room.compound.processor.data.utility.EntityJunction
import com.attafitamim.room.compound.processor.generator.syntax.CONSTANT_NAME_SEPARATOR
import com.attafitamim.room.compound.processor.generator.syntax.DAO_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.DAO_POSTFIX
import com.attafitamim.room.compound.processor.generator.syntax.DAO_PREFIX
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_ENTITIES_METHOD_NAME
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_METHOD_NAME
import com.attafitamim.room.compound.processor.generator.syntax.INSTANCE_ACCESS_KEY
import com.attafitamim.room.compound.processor.generator.syntax.LIST_ITEM_POSTFIX
import com.attafitamim.room.compound.processor.generator.syntax.LIST_OF_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.NULLABLE_SIGN
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_CLOSE_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_OPEN_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_SEPARATOR
import com.attafitamim.room.compound.processor.generator.syntax.PropertyAccessSyntax
import com.attafitamim.room.compound.processor.generator.syntax.ROOM_PACKAGE
import com.attafitamim.room.compound.processor.generator.syntax.UPDATE_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.UPDATE_ENTITIES_METHOD_NAME
import com.attafitamim.room.compound.processor.generator.syntax.UPDATE_METHOD_NAME
import com.attafitamim.room.compound.processor.utils.createForEachSyntax
import com.attafitamim.room.compound.processor.utils.createInitializationSyntax
import com.attafitamim.room.compound.processor.utils.createListAdditionSyntax
import com.attafitamim.room.compound.processor.utils.createMethodCallSyntax
import com.attafitamim.room.compound.processor.utils.titleToCamelCase
import com.attafitamim.room.compound.processor.utils.writeToFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

// TODO(Generator): Refactor generator logic and structure
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

        val insertOrUpdateFunctionBuilder = FunSpec.builder(INSERT_METHOD_NAME)
            .addParameter(compoundData.propertyInfo.name, compoundListName)

        val updateFunctionBuilder = FunSpec.builder(UPDATE_METHOD_NAME)
            .addParameter(compoundData.propertyInfo.name, compoundListName)

        if (isSuspendDao) {
            insertOrUpdateFunctionBuilder.addModifiers(KModifier.SUSPEND)
            updateFunctionBuilder.addModifiers(KModifier.SUSPEND)
        }

        val listInitializationBlock = createListInitializationBlock(compoundData)
        val listMappingBlock = createListMappingBlock(compoundData)

        val insertListBlock = createListActionBlock(
            compoundData,
            INSERT_ENTITIES_METHOD_NAME
        )

        val updateListBlock = createListActionBlock(
            compoundData,
            UPDATE_ENTITIES_METHOD_NAME
        )

        val insertEntityFunctionBuilder = createEntityActionFunction(
            compoundData,
            INSERT_ENTITIES_METHOD_NAME,
            INSERT_ANNOTATION
        )

        val updateEntityFunctionBuilder = createEntityActionFunction(
            compoundData,
            UPDATE_ENTITIES_METHOD_NAME,
            UPDATE_ANNOTATION
        )

        insertOrUpdateFunctionBuilder
            .addCode(listInitializationBlock)
            .addCode(listMappingBlock)
            .addCode(insertListBlock)

        updateFunctionBuilder
            .addCode(listInitializationBlock)
            .addCode(listMappingBlock)
            .addCode(updateListBlock)

        val listInsertFunction = insertOrUpdateFunctionBuilder.build()
        val listUpdateFunction = updateFunctionBuilder.build()

        val singleInsertFunction = createSingleActionFunction(
            compoundClassName,
            compoundData.propertyInfo.name,
            listInsertFunction,
            isSuspendDao,
            INSERT_METHOD_NAME
        )

        val singleUpdateFunction = createSingleActionFunction(
            compoundClassName,
            compoundData.propertyInfo.name,
            listUpdateFunction,
            isSuspendDao,
            UPDATE_METHOD_NAME
        )

        val daoAnnotation = ClassName(
            ROOM_PACKAGE,
            DAO_ANNOTATION
        )

        val interfaceSpec = TypeSpec.interfaceBuilder(fileName)
            .addAnnotation(daoAnnotation)
            .addFunction(singleInsertFunction)
            .addFunction(listInsertFunction)
            .addFunction(insertEntityFunctionBuilder)
            .addFunction(singleUpdateFunction)
            .addFunction(listUpdateFunction)
            .addFunction(updateEntityFunctionBuilder)
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

    private fun createEntityActionFunction(
        compoundData: EntityData.MainCompound,
        actionName: String,
        actionAnnotationName: String
    ): FunSpec {
        val actionAnnotationClassName = ClassName(
            ROOM_PACKAGE,
            actionAnnotationName
        )

        val actionAnnotation = AnnotationSpec.builder(actionAnnotationClassName).build()

        val functionBuilder = FunSpec.builder(actionName)
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(actionAnnotation)

        if (isSuspendDao) functionBuilder.addModifiers(KModifier.SUSPEND)

        val presentEntities = HashSet<String>()
        fun addActionParameter(packageName: String, className: String) {
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
                        addActionParameter(junction.packageName, junction.className)
                    }

                    when (entityData) {
                        is EntityData.Compound -> entityData.entities.forEach { childEntity ->
                            handleEntity(childEntity)
                        }

                        is EntityData.Entity -> addActionParameter(
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

        val itemName = createListItemName(compoundData.propertyInfo.name)
        addForEachStatement(compoundData.propertyInfo.name, itemName)

        fun handleJunction(
            entityData: EntityData.Nested,
            parents: List<EntityData>,
            accessProperties: List<PropertyInfo>,
            insideCollection: Boolean = false
        ) {
            val junction = entityData.junction ?: return
            val listItemName = createListItemName(entityData.propertyInfo.name)

            val junctionClassName = ClassName(junction.packageName, junction.className)
            val junctionListName = titleToCamelCase(junction.className)

            val entityAccessSyntax = createPropertyAccessSyntax(
                accessProperties,
                entityData.propertyInfo
            )

            val listItemPropertyInfo = PropertyInfo(
                listItemName,
                isNullable = false,
                isCollection = false
            )

            val embeddedParentAccessSyntax = createPreviousEmbeddedEntityAccessSyntax(
                parents,
                junction.parentColumn
            )

            val embeddedEntityAccessSyntax = createNextEmbeddedEntityAccessSyntax(
                listOf(listItemPropertyInfo),
                entityData,
                junction.entityColumn
            )

            val junctionCreationStatement = createEntityJunctionStatement(
                junctionListName,
                junction,
                embeddedParentAccessSyntax,
                embeddedEntityAccessSyntax
            )

            if (entityData.propertyInfo.isCollection && !insideCollection) {
                addForEachStatement(
                    entityAccessSyntax.chain,
                    listItemName,
                    entityAccessSyntax.handleNullability
                )

                codeBlockBuilder.addStatement(junctionCreationStatement, junctionClassName)
                    .endControlFlow()
            } else {
                codeBlockBuilder.addStatement(junctionCreationStatement, junctionClassName)
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

                            val listItemName = createListItemName(entityData.propertyInfo.name)
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

                            handleJunction(
                                entityData,
                                parents,
                                accessProperties,
                                insideCollection = true
                            )

                            entityData.entities.forEach { compoundEntityData ->
                                handleEntity(
                                    compoundEntityData,
                                    parents + entityData,
                                    listOf(listPropertyInfo)
                                )
                            }

                            codeBlockBuilder.endControlFlow()
                        } else {
                            handleJunction(
                                entityData,
                                parents,
                                accessProperties,
                                insideCollection = true
                            )

                            entityData.entities.forEach { compoundEntityData ->
                                handleEntity(
                                    compoundEntityData,
                                    parents + entityData,
                                    accessProperties + entityData.propertyInfo
                                )
                            }
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

    private fun createPreviousEmbeddedEntityAccessSyntax(
        parents: List<EntityData>,
        columnName: String
    ): PropertyAccessSyntax {
        val accessParents = parents.map(EntityData::propertyInfo)

        val collectionIndex = accessParents.indexOfLast { propertyInfo ->
            propertyInfo.isCollection
        }.takeIf { index ->
            index in 1..parents.size
        }

        val newAccessParents = if (collectionIndex != null) {
            accessParents.subList(collectionIndex, accessParents.size).toMutableList()
        } else {
            accessParents.toMutableList()
        }

        when (val parent = parents.last()) {
            is EntityData.MainCompound -> {
                newAccessParents.add(parent.entities.first(EntityData.Nested::isEmbedded).propertyInfo)
            }

            is EntityData.Compound -> {
                newAccessParents.add(parent.entities.first(EntityData.Nested::isEmbedded).propertyInfo)
            }

            is EntityData.Entity -> {
                // Do nothing, same entity
            }
        }

        val columnPropertyInfo = PropertyInfo(
            columnName,
            isNullable = false,
            isCollection = false
        )

        return createPropertyAccessSyntax(newAccessParents, columnPropertyInfo)
    }

    private fun createNextEmbeddedEntityAccessSyntax(
        accessParents: List<PropertyInfo>,
        entityData: EntityData,
        columnName: String
    ): PropertyAccessSyntax {
        val collectionIndex = accessParents.indexOfLast { propertyInfo ->
            propertyInfo.isCollection
        }.takeIf { index ->
            index in 0..accessParents.lastIndex
        }

        val newAccessParents = if (collectionIndex != null) {
            accessParents.toMutableList()
            //accessParents.subList(collectionIndex, accessParents.size).toMutableList()
        } else {
            accessParents.toMutableList()
        }

        when (entityData) {
            is EntityData.MainCompound -> {
                newAccessParents.add(entityData.entities.first(EntityData.Nested::isEmbedded).propertyInfo)
            }

            is EntityData.Compound -> {
                newAccessParents.add(entityData.entities.first(EntityData.Nested::isEmbedded).propertyInfo)
            }

            is EntityData.Entity -> {
                // Do nothing, same entity
            }
        }

        val columnInfo = PropertyInfo(
            columnName,
            isNullable = false,
            isCollection = false
        )

        return createPropertyAccessSyntax(newAccessParents, columnInfo)
    }

    private fun createListActionBlock(
        compoundData: EntityData.MainCompound,
        actionMethodName: String
    ): CodeBlock {
        val presentEntities = HashSet<String>()
        val codeBlockBuilder = CodeBlock.builder()

        val actionMethodCall = buildString {
            append(actionMethodName, PARAMETER_OPEN_PARENTHESIS)
        }

        codeBlockBuilder.addStatement(actionMethodCall)

        fun addActionParameter(className: String) {
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
                        addActionParameter(junction.className)
                    }

                    when (entityData) {
                        is EntityData.Compound -> entityData.entities.forEach(::handleEntity)

                        is EntityData.Entity -> {
                            addActionParameter(entityData.typeInfo.className)
                        }
                    }
                }
            }
        }

        handleEntity(compoundData)
        return codeBlockBuilder.addStatement(PARAMETER_CLOSE_PARENTHESIS).build()
    }

    private fun createListItemName(
        listName: String,
    ): String {
        val camelClassName = titleToCamelCase(listName)
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
        val propertyNameBuilder = StringBuilder()
        val propertyAccessBuilder = StringBuilder()

        accessProperties.forEach { accessProperty ->
            val name = if (accessProperty.isCollection) createListItemName(accessProperty.name)
            else accessProperty.name

            propertyAccessBuilder.append(name)
            propertyNameBuilder.append(name)

            handleNullability = handleNullability || accessProperty.isNullable
            if (handleNullability) propertyAccessBuilder.append(NULLABLE_SIGN)
            propertyAccessBuilder.append(INSTANCE_ACCESS_KEY)
            propertyNameBuilder.append(CONSTANT_NAME_SEPARATOR)
        }

        propertyAccessBuilder.append(property.name)
        propertyNameBuilder.append(property.name)

        handleNullability = handleNullability || property.isNullable

        return PropertyAccessSyntax(
            propertyAccessBuilder.toString(),
            propertyNameBuilder.toString(),
            handleNullability
        )
    }

    private fun createSingleActionFunction(
        compoundClassName: ClassName,
        parameterName: String,
        listActionFunction: FunSpec,
        isSuspend: Boolean,
        actionName: String
    ): FunSpec {
        val listCreationMethodCall = createMethodCallSyntax(
            LIST_OF_METHOD,
            parameterName
        )

        val actionMethodCall = createMethodCallSyntax(
            actionName,
            listCreationMethodCall
        )

        val functionBuilder = FunSpec.builder(actionName)
            .addParameter(parameterName, compoundClassName)
            .addStatement(actionMethodCall, listActionFunction)

        if (isSuspend) functionBuilder.addModifiers(KModifier.SUSPEND)

        return functionBuilder.build()
    }

    private fun createEntityJunctionStatement(
        junctionListName: String,
        junction: EntityJunction,
        embeddedParentAccessSyntax: PropertyAccessSyntax,
        embeddedEntityAccessSyntax: PropertyAccessSyntax
    ) = buildString {
        val handleNullability = embeddedParentAccessSyntax.handleNullability ||
                embeddedEntityAccessSyntax.handleNullability

        if (handleNullability) {
            append("if (")
            if (embeddedParentAccessSyntax.handleNullability) {
                append("${embeddedParentAccessSyntax.chain} != null")
                if (embeddedEntityAccessSyntax.handleNullability) append(" && ")
            }

            if (embeddedEntityAccessSyntax.handleNullability) {
                append("${embeddedEntityAccessSyntax.chain} != null")
            }

            append(")\n")
        }

        append(junctionListName, ".add(\n")
        append("%T(\n")
        append(junction.junctionParentColumn, " = ", embeddedParentAccessSyntax.chain, ",\n")
        append(junction.junctionEntityColumn, " = ", embeddedEntityAccessSyntax.chain, "\n)")
        append("\n)")
    }
}