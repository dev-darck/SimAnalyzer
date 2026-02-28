package com.project.analyzer.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryIdentityFormatterTest {

    @Test
    fun `formatTrackName appends layout to readable track name`() {
        val value = TelemetryIdentityFormatter.formatTrackName(
            trackName = "Brands Hatch",
            trackId = "brands_hatch_indy",
            layoutId = "indy",
        )

        assertEquals("Brands Hatch Indy", value)
    }

    @Test
    fun `formatTrackName humanizes slug fallback`() {
        val value = TelemetryIdentityFormatter.formatTrackName(
            trackName = "brands_hatch",
            trackId = "brands_hatch_indy",
            layoutId = "indy",
        )

        assertEquals("Brands Hatch Indy", value)
    }

    @Test
    fun `formatCarName humanizes car model id`() {
        val value = TelemetryIdentityFormatter.formatCarName(
            carModel = "ks_bmw_m4_gt3",
        )

        assertEquals("BMW M4 GT3", value)
    }

    @Test
    fun `stableCarId is deterministic for normalized car model`() {
        val first = TelemetryIdentityIds.stableCarId(" ks_bmw_m4_gt3 ")
        val second = TelemetryIdentityIds.stableCarId("KS_BMW_M4_GT3")

        assertEquals(first, second)
    }
}
