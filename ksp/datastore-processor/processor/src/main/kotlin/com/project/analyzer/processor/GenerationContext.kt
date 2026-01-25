package com.project.analyzer.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName

internal data class GenerationContext(
    val packageName: String,
    val modelClassName: ClassName,
    val simpleName: String,
    val serializerName: String,
    val factoryFunName: String,
    val defaultFileName: String,

    val serializerInterface: TypeName,
    val inputStream: ClassName,
    val outputStream: ClassName,
    val fileClass: ClassName,
    val protoBuf: ClassName,
    val serializationException: ClassName,
    val corruptionException: ClassName,
    val dataStoreFactory: ClassName,
    val dataStoreType: TypeName,

    val sourceFqName: String,
) {

    companion object {

        fun from(clazz: KSClassDeclaration): GenerationContext {
            val packageName = clazz.packageName.asString()
            val modelClassName = clazz.toClassName()
            val simpleName = clazz.simpleName.asString()
            val serializerName = "${simpleName}DataStoreSerializer"
            val factoryFunName = "create${simpleName}DataStore"
            val defaultFileName = "${simpleName.toSnakeCase()}.pb"

            val serializerInterface = cn("androidx.datastore.core", "Serializer").parameterizedBy(modelClassName)

            val inputStream = cn("java.io", "InputStream")
            val outputStream = cn("java.io", "OutputStream")
            val fileClass = cn("java.io", "File")

            val protoBuf = cn("kotlinx.serialization.protobuf", "ProtoBuf")
            val serializationException = cn("kotlinx.serialization", "SerializationException")
            val corruptionException = cn("androidx.datastore.core", "CorruptionException")

            val dataStoreFactory = cn("androidx.datastore.core", "DataStoreFactory")
            val dataStoreType = cn("androidx.datastore.core", "DataStore").parameterizedBy(modelClassName)

            val sourceFqName = clazz.qualifiedName?.asString() ?: simpleName

            return GenerationContext(
                packageName = packageName,
                modelClassName = modelClassName,
                simpleName = simpleName,
                serializerName = serializerName,
                factoryFunName = factoryFunName,
                defaultFileName = defaultFileName,
                serializerInterface = serializerInterface,
                inputStream = inputStream,
                outputStream = outputStream,
                fileClass = fileClass,
                protoBuf = protoBuf,
                serializationException = serializationException,
                corruptionException = corruptionException,
                dataStoreFactory = dataStoreFactory,
                dataStoreType = dataStoreType,
                sourceFqName = sourceFqName,
            )
        }

        private fun cn(packageName: String, vararg simpleNames: String): ClassName =
            ClassName(packageName, simpleNames.asList())

        private val WORD_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")

        private fun String.toSnakeCase(): String =
            trim()
                .replace('-', '_')
                .replace(' ', '_')
                .split(WORD_BOUNDARY)
                .filter { it.isNotBlank() }
                .joinToString("_") { it.lowercase() }
    }
}
