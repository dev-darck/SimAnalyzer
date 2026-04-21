package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Duration

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginArchiveDownloader(
    private val appDirectories: AppDirectories,
    @param:IO private val ioDispatcher: CoroutineDispatcher,
) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun downloadArchive(metadata: LmuPluginMetadata): LmuPluginArchive = withContext(ioDispatcher) {
        val cacheDir = appDirectories.cacheDir.toPath()
            .resolve(LMU_PLUGIN_CACHE_DIR_NAME)
            .resolve(metadata.version)
        Files.createDirectories(cacheDir)

        val archivePath = cacheDir.resolve("rf2_sm_tools_${metadata.version}.zip")
        if (Files.isRegularFile(archivePath) && Files.size(archivePath) > 0L) {
            return@withContext LmuPluginArchive(path = archivePath, sha256 = archivePath.sha256Hex())
        }

        Files.deleteIfExists(archivePath)
        val directDownloadUrl = resolveDirectDownloadUrl(metadata)
        val request = baseRequestBuilder(directDownloadUrl)
            .header("Accept", "*/*")
            .header("Referer", metadata.downloadPageUrl)
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { input ->
            check(response.statusCode() in 200..299) {
                "Request failed for $directDownloadUrl with HTTP ${response.statusCode()}."
            }
            Files.newOutputStream(
                archivePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                input.copyTo(output)
            }
        }

        LmuPluginArchive(path = archivePath, sha256 = archivePath.sha256Hex())
    }

    private fun resolveDirectDownloadUrl(metadata: LmuPluginMetadata): String {
        validateDownloadPageUrl(metadata.downloadPageUrl, metadata.version)
        val pageHtml = httpGetText(metadata.downloadPageUrl)
        val matchedUrl = DOWNLOAD_BUTTON_REGEX.find(pageHtml)?.groupValues?.getOrNull(1)
            ?: DIRECT_DOWNLOAD_REGEX.find(pageHtml)?.value
            ?: error("Unable to resolve the direct plugin archive link from the official download page.")
        val resolved = matchedUrl
            .replace("\\/", "/")
            .replace("&amp;", "&")
        validateDirectDownloadUrl(resolved, metadata.version)
        return resolved
    }

    private fun httpGetText(url: String): String {
        val request = baseRequestBuilder(url)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        check(response.statusCode() in 200..299) {
            "Request failed for $url with HTTP ${response.statusCode()}."
        }
        return response.body()
    }

    private fun baseRequestBuilder(url: String): HttpRequest.Builder = HttpRequest.newBuilder(URI.create(url))
        .header("User-Agent", LMU_PLUGIN_USER_AGENT)

    private fun validateDownloadPageUrl(url: String, version: String) {
        val uri = URI.create(url)
        check(uri.scheme == "https" && uri.host == "www.mediafire.com") {
            "Unsupported plugin download page host: ${uri.host}."
        }
        check(uri.path.endsWith("/rf2_sm_tools_$version.zip/file")) {
            "Unsupported plugin archive name: ${uri.path}."
        }
    }

    private fun validateDirectDownloadUrl(url: String, version: String) {
        val uri = URI.create(url)
        check(uri.scheme == "https" && uri.host.orEmpty().startsWith("download")) {
            "Unsupported plugin archive host: ${uri.host}."
        }
        check(uri.path.contains("rf2_sm_tools_$version.zip")) {
            "Unsupported plugin archive file: ${uri.path}."
        }
    }

    private companion object {
        val DOWNLOAD_BUTTON_REGEX: Regex = Regex("id=\"downloadButton\"[^>]*href=\"([^\"]+)\"")
        val DIRECT_DOWNLOAD_REGEX: Regex = Regex("https://download[^\"']+rf2_sm_tools_[0-9.]+\\.zip[^\"']*")
    }
}
