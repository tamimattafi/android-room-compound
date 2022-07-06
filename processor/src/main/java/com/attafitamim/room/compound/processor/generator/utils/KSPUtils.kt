package com.attafitamim.room.compound.processor.generator.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

fun KSPLogger.throwException(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    val exception = Exception(message)
    exception(exception)
    throw exception
}