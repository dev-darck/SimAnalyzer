package com.project.analyzer.ac.telemetry.impl.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackIdNormalizerTest {

    @Test
    fun `normalizes embedded layout aliases inside track id`() {
        assertEquals(
            "spa_gp",
            TrackIdNormalizer.normalize(track = "circuit_de_spa_francorchamps_gp", layout = null),
        )
        assertEquals(
            "donington_park_gp",
            TrackIdNormalizer.normalize(track = "donington_grand_prix", layout = null),
        )
        assertEquals(
            "fuji_gp_short",
            TrackIdNormalizer.normalize(track = "fuji_speedway_gp_short", layout = null),
        )
        assertEquals(
            "redbull_ring_gp",
            TrackIdNormalizer.normalize(track = "red_bull_ring_gp", layout = null),
        )
        assertEquals(
            "sebring_gp",
            TrackIdNormalizer.normalize(track = "sebring_international_raceway_gp", layout = null),
        )
    }

    @Test
    fun `normalizes base aliases when layout arrives separately`() {
        assertEquals(
            "donington_park_gp",
            TrackIdNormalizer.normalize(track = "donington", layout = "grand_prix"),
        )
        assertEquals(
            "fuji_gp",
            TrackIdNormalizer.normalize(track = "fuji_speedway", layout = "gp"),
        )
        assertEquals(
            "circuit_of_the_americas_gp",
            TrackIdNormalizer.normalize(track = "cota", layout = "gp"),
        )
        assertEquals(
            "sebring_gp",
            TrackIdNormalizer.normalize(track = "sebring_international_raceway", layout = "gp"),
        )
    }

    @Test
    fun `normalizes evo default layouts that use track name instead of gp`() {
        assertEquals(
            "gp",
            TrackIdNormalizer.normalizeLayoutId("imola"),
        )
        assertEquals(
            "gp",
            TrackIdNormalizer.normalizeLayoutId("laguna_seca"),
        )
        assertEquals(
            "gp",
            TrackIdNormalizer.normalizeLayoutId("track_layout"),
        )
        assertEquals(
            "imola_gp",
            TrackIdNormalizer.normalize(track = "imola", layout = "imola"),
        )
        assertEquals(
            "laguna_seca_gp",
            TrackIdNormalizer.normalize(track = "laguna_seca", layout = "laguna_seca"),
        )
        assertEquals(
            "mount_panorama_gp",
            TrackIdNormalizer.normalize(track = "mount_panorama", layout = "track_layout"),
        )
        assertEquals(
            "mount_panorama_gp",
            TrackIdNormalizer.normalize(track = "mount_panorama_track_layout", layout = null),
        )
    }
}
