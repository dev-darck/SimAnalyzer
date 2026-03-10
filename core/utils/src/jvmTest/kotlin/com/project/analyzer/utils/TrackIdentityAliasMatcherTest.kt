package com.project.analyzer.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackIdentityAliasMatcherTest {

    @Test
    fun `areEquivalent matches telemetry alias with inserted family token`() {
        assertTrue(
            TrackIdentityAliasMatcher.areEquivalent(
                trackId = "watkins_glen_short_inner_loop",
                layoutId = "short_inner_loop",
                otherTrackId = "watkins_glen_international_short_inner_loop",
                otherLayoutId = null,
            )
        )
    }

    @Test
    fun `areEquivalent matches underscore and compact variants`() {
        assertTrue(
            TrackIdentityAliasMatcher.areEquivalent(
                trackId = "red_bull_ring_gp",
                layoutId = "gp",
                otherTrackId = "redbull_ring_gp",
                otherLayoutId = "gp",
            )
        )
    }

    @Test
    fun `areEquivalent does not merge different layouts`() {
        assertFalse(
            TrackIdentityAliasMatcher.areEquivalent(
                trackId = "circuit_of_the_americas_gp",
                layoutId = "gp",
                otherTrackId = "circuit_of_the_americas_national",
                otherLayoutId = "national",
            )
        )
    }
}
