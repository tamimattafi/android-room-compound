package com.attafitamim.room.compound.processor.utils

import com.attafitamim.room.compound.processor.generator.syntax.*
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream


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
            propertyAccessSyntax.chain,
            NULLABLE_SIGN,
            INSTANCE_ACCESS_KEY,
            letMethodCall
        )
    } else {
        val listAddMethodCall = createMethodCallSyntax(
            additionMethod,
            propertyAccessSyntax.chain
        )
        append(listName, INSTANCE_ACCESS_KEY, listAddMethodCall)
    }
}

fun createForEachSyntax(
    collectionName: String,
    elementName: String,
    isNullable: Boolean
) = buildString {
    append(collectionName)
    if (isNullable) append(NULLABLE_SIGN)
    append(
        INSTANCE_ACCESS_KEY,
        FOR_EACH_METHOD,
        NO_WRAP_KEYWORD_SEPARATOR,
        CURLY_BRACE_OPEN_PARENTHESIS,
        NO_WRAP_KEYWORD_SEPARATOR,
        elementName,
        NO_WRAP_KEYWORD_SEPARATOR,
        LAMBDA_ARROW
    )
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