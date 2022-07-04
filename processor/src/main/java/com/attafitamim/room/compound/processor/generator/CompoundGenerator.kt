package com.attafitamim.room.compound.processor.generator

import com.attafitamim.room.compound.processor.data.CompoundData
import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.ksp.CompoundVisitor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import java.io.OutputStream

class CompoundGenerator(
    private val codeGenerator: CodeGenerator
) {

    fun generateDao(compoundData: CompoundData) {
        val fileName = buildString {
            append(compoundData.className, "Dao")
        }

        val compoundClassName = ClassName(compoundData.packageName, compoundData.className)

        val transactionAnnotation = ClassName(CompoundVisitor.ROOM_PACKAGE, CompoundVisitor.TRANSACTION_ANNOTATION)
        val insertAnnotationClassName = ClassName(CompoundVisitor.ROOM_PACKAGE, CompoundVisitor.INSERT_ANNOTATION)
        val conflictStrategyReplaceName = ClassName(
            CompoundVisitor.ROOM_PACKAGE, listOf(
                CompoundVisitor.CONFLICT_STRATEGY, CompoundVisitor.CONFLICT_STRATEGY_REPLACE
            ))
        val insertAnnotation = AnnotationSpec.builder(insertAnnotationClassName)
            .addMember("onConflict = %T", conflictStrategyReplaceName)
            .build()

        val compoundListName = Collection::class.asClassName().parameterizedBy(compoundClassName)
        val listInsertFunction = FunSpec.builder("insertOrUpdate")
            .addParameter("compounds", compoundListName)
            .addAnnotation(transactionAnnotation)

        fun addEntitySet(entityData: EntityData, parentName: String? = null) {
            val parameterName = buildString {
                if (parentName != null) append(parentName, "_")
                append(entityData.propertyName)
            }

            when (entityData) {
                is EntityData.Compound -> {
                    entityData.entities.forEach { compoundEntityData ->
                        addEntitySet(compoundEntityData, parameterName)
                    }
                }

                is EntityData.Entity -> {
                    val entityClassName = ClassName(entityData.packageName, entityData.className)
                    val parentSetName = HashSet::class.asClassName().parameterizedBy(entityClassName)
                    listInsertFunction.addStatement("val $parameterName = %T()", parentSetName)
                }
            }
        }

        val singleInsertFunction = FunSpec.builder("insertOrUpdate")
            .addParameter("compound", compoundClassName)
            .addStatement("insertOrUpdate(listOf(compound))", listInsertFunction)
            .addAnnotation(transactionAnnotation)
            .build()

        val entityInsertFunction = FunSpec.builder("insertOrUpdate")
            .addModifiers(KModifier.ABSTRACT)
            .addAnnotation(insertAnnotation)

        fun addParameter(entityData: EntityData, parentName: String? = null) {
            val parameterName = buildString {
                if (parentName != null) append(parentName, "_")
                append(entityData.propertyName)
            }

            when (entityData) {
                is EntityData.Compound -> {
                    entityData.entities.forEach { compoundEntityData ->
                        addParameter(compoundEntityData, parameterName)
                    }
                }

                is EntityData.Entity -> {
                    val entityClassName = ClassName(entityData.packageName, entityData.className)
                    val parentListName = Collection::class.asClassName().parameterizedBy(entityClassName)
                    entityInsertFunction.addParameter(parameterName, parentListName)
                }
            }
        }

        compoundData.entities.forEach { entityData ->
            addParameter(entityData)
            addEntitySet(entityData)
        }

        val daoAnnotation = ClassName(CompoundVisitor.ROOM_PACKAGE, CompoundVisitor.DAO_ANNOTATION)

        val interfaceSpec = TypeSpec.interfaceBuilder(fileName)
            .addAnnotation(daoAnnotation)
            .addFunction(singleInsertFunction)
            .addFunction(listInsertFunction.build())
            .addFunction(entityInsertFunction.build())
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

    private fun FileSpec.writeToFile(outputStream: OutputStream) = outputStream.use { stream ->
        stream.writer().use(::writeTo)
    }
}