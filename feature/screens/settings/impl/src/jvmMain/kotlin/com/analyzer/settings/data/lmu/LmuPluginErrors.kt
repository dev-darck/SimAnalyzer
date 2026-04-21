package com.analyzer.settings.data.lmu

import java.net.http.HttpTimeoutException
import java.nio.file.AccessDeniedException

internal fun Throwable.toLmuPluginUserMessage(): String = when (this) {
    is AccessDeniedException -> "Windows denied access to the LMU files. Close Le Mans Ultimate and retry."

    is HttpTimeoutException -> "Timed out while contacting the official plugin source. Check your network and retry."

    else -> message?.trim().orEmpty()
        .takeIf(String::isNotBlank)
        ?: "Unable to complete the LMU shared memory plugin setup."
}
