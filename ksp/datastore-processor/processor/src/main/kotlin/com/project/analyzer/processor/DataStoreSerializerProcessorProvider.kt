package com.project.analyzer.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class DataStoreSerializerProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataStoreSerializerProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
