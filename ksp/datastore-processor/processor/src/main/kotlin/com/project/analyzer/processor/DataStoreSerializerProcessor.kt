package com.project.analyzer.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class DataStoreSerializerProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ANNOTATION_NAME)
        val (valid, invalid) = symbols.partition { it.validate() }

        valid.forEach { symbol ->
            val clazz = symbol as? KSClassDeclaration
            if (clazz == null) {
                logger.error("@$ANNOTATION_NAME can only be applied to classes.", symbol)
                return@forEach
            }

            if (clazz.classKind != com.google.devtools.ksp.symbol.ClassKind.CLASS) {
                logger.error("@$ANNOTATION_NAME can only be applied to CLASS declarations.", clazz)
                return@forEach
            }

            if (clazz.modifiers.contains(Modifier.ABSTRACT)) {
                logger.error(
                    "Class ${clazz.qualifiedName?.asString()} is abstract; cannot generate a DataStore serializer.",
                    clazz
                )
                return@forEach
            }

            val isSerializable = clazz.annotations.any { a ->
                a.annotationType.resolve().declaration.qualifiedName?.asString() == SERIALIZABLE_FQCN
            }
            if (!isSerializable) {
                logger.error("Class ${clazz.qualifiedName?.asString()} must be annotated with @Serializable.", clazz)
                return@forEach
            }

            val primaryCtor = clazz.primaryConstructor
            if (primaryCtor == null) {
                logger.error("Class ${clazz.qualifiedName?.asString()} must have a primary constructor.", clazz)
                return@forEach
            }

            val allHaveDefaults = primaryCtor.parameters.all { it.hasDefault }
            if (!allHaveDefaults) {
                logger.error(
                    "All primary constructor parameters of ${clazz.qualifiedName?.asString()} must have default values " +
                        "so that ${clazz.simpleName.asString()}() can be used as defaultValue.",
                    clazz
                )
                return@forEach
            }

            generateSerializer(clazz)
        }

        return invalid
    }

    private fun generateSerializer(clazz: KSClassDeclaration) {
        val ctx = GenerationContext.from(clazz)

        val formatProp = buildFormatProperty(ctx)
        val defaultValueProp = buildDefaultValueProperty(ctx)

        val serializerObject = buildSerializerObject(
            ctx = ctx,
            formatProp = formatProp,
            defaultValueProp = defaultValueProp,
            readFromFun = buildReadFromFun(ctx),
            writeToFun = buildWriteToFun(ctx),
        )

        val factories = buildFactoryFunctions(ctx)

        val fileSpec = FileSpec.builder(ctx.packageName, ctx.serializerName)
            .addType(serializerObject)
            .apply { factories.forEach(::addFunction) }
            .build()

        val deps = clazz.containingFile?.let { Dependencies(aggregating = false, it) }
            ?: Dependencies(aggregating = false)

        fileSpec.writeTo(codeGenerator, deps)
        logger.info("Generated ${ctx.packageName}.${ctx.serializerName} (+ factories)")
    }

    private fun buildDefaultValueProperty(ctx: GenerationContext): PropertySpec =
        PropertySpec.builder("defaultValue", ctx.modelClassName)
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%T()", ctx.modelClassName)
            .build()

    private fun buildFormatProperty(ctx: GenerationContext): PropertySpec =
        PropertySpec.builder("format", ctx.protoBuf)
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T { encodeDefaults = true }", ctx.protoBuf)
            .build()

    private fun buildReadFromFun(ctx: GenerationContext): FunSpec =
        FunSpec.builder("readFrom")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("input", ctx.inputStream)
            .returns(ctx.modelClassName)
            .addStatement("val bytes = input.readBytes()")
            .beginControlFlow("if (bytes.isEmpty())")
            .addStatement("return defaultValue")
            .endControlFlow()
            .beginControlFlow("return try")
            .addStatement("format.decodeFromByteArray(%T.serializer(), bytes)", ctx.modelClassName)
            .nextControlFlow("catch (e: %T)", ctx.serializationException)
            .addStatement("throw %T(%S, e)", ctx.corruptionException, "Cannot read proto.")
            .endControlFlow()
            .build()

    private fun buildWriteToFun(ctx: GenerationContext): FunSpec =
        FunSpec.builder("writeTo")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("t", ctx.modelClassName)
            .addParameter("output", ctx.outputStream)
            .addStatement("val bytes = format.encodeToByteArray(%T.serializer(), t)", ctx.modelClassName)
            .addStatement("output.write(bytes)")
            .build()

    private fun buildSerializerObject(
        ctx: GenerationContext,
        formatProp: PropertySpec,
        defaultValueProp: PropertySpec,
        readFromFun: FunSpec,
        writeToFun: FunSpec,
    ): TypeSpec =
        TypeSpec.objectBuilder(ctx.serializerName)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc(
                "Generated by DataStoreSerializerProcessor. DO NOT EDIT.\n" +
                    "Source: %L\n",
                ctx.sourceFqName
            )
            .addSuperinterface(ctx.serializerInterface)
            .addProperty(formatProp)
            .addProperty(defaultValueProp)
            .addFunction(readFromFun)
            .addFunction(writeToFun)
            .build()

    private fun buildFactoryFunctions(ctx: GenerationContext): List<FunSpec> {
        val produceFileType = LambdaTypeName.get(
            parameters = emptyList(),
            returnType = ctx.fileClass
        )

        val byProduceFile = FunSpec.builder(ctx.factoryFunName)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc(
                "Generated by DataStoreSerializerProcessor.\n" +
                    "Creates a DataStore for %T using the generated serializer.\n",
                ctx.modelClassName
            )
            .addParameter("produceFile", produceFileType)
            .returns(ctx.dataStoreType)
            .addStatement(
                "return %T.create(serializer = %L, produceFile = produceFile)",
                ctx.dataStoreFactory,
                ctx.serializerName
            )
            .build()

        val byFile = FunSpec.builder(ctx.factoryFunName)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc(
                "Generated by DataStoreSerializerProcessor.\n" +
                    "Creates a DataStore for %T using the given file.\n",
                ctx.modelClassName
            )
            .addParameter("file", ctx.fileClass)
            .returns(ctx.dataStoreType)
            .addStatement("return %L(produceFile = { file })", ctx.factoryFunName)
            .build()

        val byDirectory = FunSpec.builder(ctx.factoryFunName)
            .addModifiers(KModifier.INTERNAL)
            .addKdoc(
                "Generated by DataStoreSerializerProcessor.\n" +
                    "Creates a DataStore for %T in the given directory.\n",
                ctx.modelClassName
            )
            .addParameter("directory", ctx.fileClass)
            .addParameter(
                ParameterSpec.builder("fileName", STRING)
                    .defaultValue("%S", ctx.defaultFileName)
                    .build()
            )
            .returns(ctx.dataStoreType)
            .beginControlFlow("if (!directory.exists())")
            .addStatement("directory.mkdirs()")
            .endControlFlow()
            .addStatement("return %L(produceFile = { %T(directory, fileName) })", ctx.factoryFunName, ctx.fileClass)
            .build()

        return listOf(byProduceFile, byFile, byDirectory)
    }

    private companion object {

        const val ANNOTATION_NAME = "com.project.analyzer.annotations.DataStoreSerializer"
        const val SERIALIZABLE_FQCN = "kotlinx.serialization.Serializable"
    }
}
