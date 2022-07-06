package com.attafitamim.room.compound.processor.generator.utils

import com.attafitamim.room.compound.processor.data.EntityData
import com.attafitamim.room.compound.processor.generator.syntax.CLASS_ACCESS_KEY
import com.attafitamim.room.compound.processor.generator.syntax.COMPOUND_PARAMETER_NAME
import com.attafitamim.room.compound.processor.generator.syntax.FOR_EACH_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.IMMUTABLE_KEYWORD
import com.attafitamim.room.compound.processor.generator.syntax.INITIALIZATION_SIGN
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_METHOD_NAME
import com.attafitamim.room.compound.processor.generator.syntax.INSTANCE_ACCESS_KEY
import com.attafitamim.room.compound.processor.generator.syntax.KEYWORD_SEPARATOR
import com.attafitamim.room.compound.processor.generator.syntax.LET_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.LIST_MULTI_ADD_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.LIST_OF_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.LIST_SINGLE_ADD_METHOD
import com.attafitamim.room.compound.processor.generator.syntax.NULLABLE_SIGN
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_CLOSE_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_NAME_SEPARATOR
import com.attafitamim.room.compound.processor.generator.syntax.PARAMETER_OPEN_PARENTHESIS
import com.attafitamim.room.compound.processor.generator.syntax.PropertyAccessSyntax
import com.attafitamim.room.compound.processor.generator.syntax.TYPE_CONCAT_INDICATOR
import com.attafitamim.room.compound.processor.generator.syntax.CONFLICT_STRATEGY
import com.attafitamim.room.compound.processor.generator.syntax.CONFLICT_STRATEGY_REPLACE
import com.attafitamim.room.compound.processor.generator.syntax.INSERT_ANNOTATION
import com.attafitamim.room.compound.processor.generator.syntax.ON_CONFLICT_PARAMETER
import com.attafitamim.room.compound.processor.generator.syntax.ROOM_PACKAGE
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import java.io.OutputStream


fun createSingleInsertFunction(
    compoundClassName: ClassName,
    listInsertFunction: FunSpec
): FunSpec {
    val listCreationMethodCall = createMethodCallSyntax(
        LIST_OF_METHOD,
        COMPOUND_PARAMETER_NAME
    )

    val insertMethodCall = createMethodCallSyntax(
        INSERT_METHOD_NAME,
        listCreationMethodCall
    )

    return FunSpec.builder(INSERT_METHOD_NAME)
        .addParameter(COMPOUND_PARAMETER_NAME, compoundClassName)
        .addStatement(insertMethodCall, listInsertFunction)
        .addModifiers(KModifier.SUSPEND)
        .build()
}

fun createInsertAnnotationSpec(): AnnotationSpec {
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

fun createListAdditionSyntax(
    listName: String,
    propertyAccessSyntax: PropertyAccessSyntax,
    isCollection: Boolean
) = buildString {
    val additionMethod = if (isCollection) LIST_MULTI_ADD_METHOD else LIST_SINGLE_ADD_METHOD
    if (propertyAccessSyntax.handleNullability) {
        val letMethodParameter = buildString {
            append(
                listName,
                CLASS_ACCESS_KEY,
                additionMethod,
            )
        }

        val letMethodCall = createMethodCallSyntax(
            LET_METHOD,
            letMethodParameter
        )

        append(
            propertyAccessSyntax.properAccess,
            NULLABLE_SIGN,
            INSTANCE_ACCESS_KEY,
            letMethodCall
        )
    } else {
        val listAddMethodCall = createMethodCallSyntax(
            additionMethod,
            propertyAccessSyntax.properAccess
        )
        append(listName, INSTANCE_ACCESS_KEY, listAddMethodCall)
    }
}

fun createEntityParameterName(
    entityData: EntityData,
    parents: List<EntityData>
) = buildString {
    parents.forEach { entityData ->
        append(entityData.propertyName, PARAMETER_NAME_SEPARATOR)
    }

    append(entityData.propertyName)
}

fun createPropertyAccessSyntax(
    parent: String,
    parents: List<EntityData>,
    propertyName: String
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

        append(propertyName)
    }

    return PropertyAccessSyntax(propertyAccess, handleNullability)
}

fun createForEachSyntax(collectionName: String) = buildString {
    append(collectionName, INSTANCE_ACCESS_KEY, FOR_EACH_METHOD)
}

fun createAssignSyntax(variableName: String) = buildString {
    append(
        variableName,
        INITIALIZATION_SIGN,
        TYPE_CONCAT_INDICATOR
    )
}

fun createInitializationSyntax(variableName: String) = buildString {
    val assignSyntax = createAssignSyntax(variableName)
    append(
        IMMUTABLE_KEYWORD,
        KEYWORD_SEPARATOR,
        assignSyntax,
        PARAMETER_OPEN_PARENTHESIS,
        PARAMETER_CLOSE_PARENTHESIS
    )
}

fun createMethodCallSyntax(methodName: String, parameters: String) = buildString {
    append(
        methodName,
        PARAMETER_OPEN_PARENTHESIS,
        parameters,
        PARAMETER_CLOSE_PARENTHESIS
    )
}

fun FileSpec.writeToFile(outputStream: OutputStream) = outputStream.use { stream ->
    stream.writer().use(::writeTo)
}