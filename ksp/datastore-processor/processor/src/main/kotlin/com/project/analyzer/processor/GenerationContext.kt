package com.project.analyzer.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName

internal data class GenerationContext(
    val packageName: String,
    val modelClassName: ClassName,
    val simpleName: String,
    val serializerName: String,
    val factoryFunName: String,
    val defaultFileName: String,

    val serializerInterface: com.squareup.kotlinpoet.TypeName,
    val inputStream: ClassName,
    val outputStream: ClassName,
    val fileClass: ClassName,
    val protoBuf: ClassName,
    val serializationException: ClassName,
    val corruptionException: ClassName,
    val dataStoreFactory: ClassName,
    val dataStoreType: com.squareup.kotlinpoet.TypeName,

    val sourceFqName: String,
) {

    companion object {

        fun from(clazz: KSClassDeclaration): GenerationContext {
            val packageName = clazz.packageName.asString()
            val modelClassName = clazz.toClassName()
            val simpleName = clazz.simpleName.asString()
            val serializerName = "${simpleName}DataStoreSerializer"
            val factoryFunName = "create${simpleName}DataStore"
            val defaultFileName = "$simpleName.pb"

            val serializerInterface =
                ClassName("androidx.datastore.core", "Serializer").parameterizedBy(modelClassName)

            val inputStream = ClassName("java.io", "InputStream")
            val outputStream = ClassName("java.io", "OutputStream")
            val fileClass = ClassName("java.io", "File")

            val protoBuf = ClassName("kotlinx.serialization.protobuf", "ProtoBuf")
            val serializationException = ClassName("kotlinx.serialization", "SerializationException")
            val corruptionException = ClassName("androidx.datastore.core", "CorruptionException")

            val dataStoreFactory = ClassName("androidx.datastore.core", "DataStoreFactory")
            val dataStoreType =
                ClassName("androidx.datastore.core", "DataStore").parameterizedBy(modelClassName)

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
    }
}
