package com.attafitamim.room.compound.processor.generator.utils

fun titleToCamelCase(string: String): String {
    require(string.isNotBlank())

    val builder = StringBuilder(string)
    builder[0] = builder[0].toLowerCase()
    return builder.toString()
}