package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginConfigWriter(private val json: Json, @param:IO private val ioDispatcher: CoroutineDispatcher) {

    private val logger = logger()

    suspend fun isReady(configPath: Path): Boolean = withContext(ioDispatcher) {
        val root = readExistingConfigurationRoot(configPath) ?: return@withContext false
        val pluginConfig = root[LMU_PLUGIN_DLL_NAME]?.jsonObject ?: return@withContext false
        pluginConfig[LMU_PLUGIN_ENABLED_KEY]?.jsonPrimitive?.intOrNull == 1
    }

    suspend fun write(configPath: Path) = withContext(ioDispatcher) {
        Files.createDirectories(configPath.parent)
        val root = readExistingConfigurationRoot(configPath) ?: buildJsonObject {}
        val pluginConfig = linkedMapOf<String, JsonElement>().apply {
            root[LMU_PLUGIN_DLL_NAME]?.jsonObject?.forEach { (key, value) ->
                put(key, value)
            }
            LMU_PLUGIN_REQUIRED_SETTINGS.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        }

        val updatedRoot = buildJsonObject {
            root.forEach { (key, value) ->
                if (key != LMU_PLUGIN_DLL_NAME) put(key, value)
            }
            put(LMU_PLUGIN_DLL_NAME, JsonObject(pluginConfig))
        }

        Files.writeString(
            configPath,
            json.encodeToString(JsonObject.serializer(), updatedRoot),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
    }

    private fun readExistingConfigurationRoot(configPath: Path): JsonObject? {
        if (!Files.isRegularFile(configPath)) return null
        val raw = Files.readString(configPath, StandardCharsets.UTF_8)
        return runCatching {
            json.parseToJsonElement(raw).jsonObject
        }.getOrElse { error ->
            logger.warn(error) { "LMU plugin config was invalid, recreating file: ${configPath.toAbsolutePath()}" }
            backupInvalidConfig(configPath)
            null
        }
    }

    private fun backupInvalidConfig(configPath: Path) {
        val backupPath = configPath.resolveSibling("${configPath.fileName}.simanalyzer.bak")
        runCatching {
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
