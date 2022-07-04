package com.attafitamim.room.compound.processor.ksp

import com.attafitamim.room.compound.annotations.Compound
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class CompoundProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val compoundVisitor by lazy {
        CompoundVisitor(codeGenerator, logger)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(Compound::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val symbolsIterator = symbols.iterator()
        if (!symbolsIterator.hasNext()) return emptyList()

        symbolsIterator.forEach { classDeclaration ->
            classDeclaration.accept(compoundVisitor, Unit)
        }

        return emptyList()
    }
}
