package com.attafitamim.room.compound.processor

import com.attafitamim.room.compound.annotations.Compound
import com.attafitamim.room.compound.processor.data.EntityData
import java.lang.IllegalStateException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class CompoundProcessor : AbstractProcessor() {

    override fun process(
        annotations: MutableSet<out TypeElement>?,
        roundEnv: RoundEnvironment
    ): Boolean {
        val compounds = roundEnv.getElementsAnnotatedWith(Compound::class.java)
            .filter { element ->
                element.kind == ElementKind.CLASS
            }.map { element ->
                println("Compound: Parsing $element")
                getCompoundData(element)
            }

        println("Compound: Parsed entities: $compounds")
        return true
    }

    private fun getCompoundData(element: Element): EntityData.Compound {
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
        val modelName = element.simpleName.toString()
        println("Compound: Parsing Compound $modelName")

        var parentEntity: EntityData? = null
        val childEntities = ArrayList<EntityData>()

        element.enclosedElements.forEach { property ->
            if (property.kind != ElementKind.FIELD) return@forEach

            println("Compound: Parsing Compound property $property")

            property.annotationMirrors.forEach { annotationMirror ->
                println("Compound: Property annotation $annotationMirror")
            }

            val parentAnnotation = property.annotationMirrors.find { annotationMirror ->
                annotationMirror.toString().contains(EMBEDDED_ANNOTATION)
            }

            if (parentAnnotation != null) {
                println("Compound: Found Compound parent $property")
                if (parentEntity != null) throw IllegalStateException("A compound can't have multiple parents")
                val isCompound = property.getAnnotation(Compound::class.java) != null
                parentEntity = if (isCompound) getCompoundData(property)
                else getEntityData(property)
                return@forEach
            }

            val childAnnotation = property.annotationMirrors.find { annotationMirror ->
                annotationMirror.toString().contains(RELATION_ANNOTATION)
            }

            if (childAnnotation != null) {
                println("Compound: Found Compound child $property")
                val isCompound = property.getAnnotation(Compound::class.java) != null
                val childEntity = if (isCompound) getCompoundData(property)
                else getEntityData(property)
                childEntities.add(childEntity)
                return@forEach
            }
        }

        val actualParent = parentEntity ?: throw IllegalStateException("A compound must have at least on parent")
        if (childEntities.isEmpty()) throw IllegalStateException("A compound must have at least on child")

        return EntityData.Compound(
            packageName,
            modelName,
            actualParent,
            childEntities
        )
    }

    private fun getEntityData(element: Element): EntityData.Entity {
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
        val modelName = element.
        val nullableAnnotation = element.getAnnotation(org.jetbrains.annotations.Nullable::class.java)

        return EntityData.Entity(packageName, modelName, nullableAnnotation != null)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String>
        = mutableSetOf(Compound::class.java.canonicalName)

    companion object {
        const val EMBEDDED_ANNOTATION = "androidx.room.Embedded"
        const val RELATION_ANNOTATION = "androidx.room.Relation"
    }
}
