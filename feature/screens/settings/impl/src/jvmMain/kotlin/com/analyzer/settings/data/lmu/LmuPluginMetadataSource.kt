package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginMetadataSource(@param:IO private val ioDispatcher: CoroutineDispatcher) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun resolveLatest(): LmuPluginMetadata = withContext(ioDispatcher) {
        var lastError: Throwable? = null
        LMU_PLUGIN_README_URLS.forEach { readmeUrl ->
            val readme = runCatching { httpGetText(readmeUrl) }.getOrElse { error ->
                if (error is CancellationException) throw error
                lastError = error
                return@forEach
            }

            val match = README_DOWNLOAD_PAGE_REGEX.find(readme)
            if (match != null) {
                return@withContext LmuPluginMetadata(
                    version = match.groupValues[1],
                    downloadPageUrl = match.value,
                )
            }
            lastError = IllegalStateException("Official README did not expose a supported plugin archive link.")
        }

        throw IllegalStateException(
            "Unable to resolve the latest LMU shared memory plugin from the official repository.",
            lastError,
        )
    }

    private fun httpGetText(url: String): String {
        validateOfficialReadmeUrl(url)
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", LMU_PLUGIN_USER_AGENT)
            .header("Accept", "text/plain,text/markdown,*/*")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        check(response.statusCode() in 200..299) {
            "Request failed for $url with HTTP ${response.statusCode()}."
        }
        return response.body()
    }

    private fun validateOfficialReadmeUrl(url: String) {
        val uri = URI.create(url)
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty()
        check(host == "raw.githubusercontent.com" || host == "github.com") {
            "Unsupported plugin metadata host: $host."
        }
        check(path.contains("/TheIronWolfModding/rF2SharedMemoryMapPlugin/")) {
            "Unsupported plugin metadata repository: $url."
        }
    }

    private companion object {
        val README_DOWNLOAD_PAGE_REGEX: Regex =
            Regex("https://www\\.mediafire\\.com/file/[^\\s)]+/rf2_sm_tools_([0-9.]+)\\.zip/file")
    }
}
