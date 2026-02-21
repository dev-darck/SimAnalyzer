package com.project.analyzer.utils.logger

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.slf4j.toKotlinLogging
import org.slf4j.MarkerFactory

public val logger: KLogger = KotlinLogging.logger {}

public inline fun <reified T : Any> T.logger(): KLogger =
    KotlinLogging.logger(T::class.java.name)

public val RATE_LIMITED: Marker = MarkerFactory.getMarker("RATE_LIMITED").toKotlinLogging()
