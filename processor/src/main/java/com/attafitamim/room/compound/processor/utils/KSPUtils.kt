package com.attafitamim.room.compound.processor.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueArgument

fun KSPLogger.throwException(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    val exception = Exception(message)
    exception(exception)
    throw exception
}

val KSValueArgument.stringValue: String get() = value.toString()