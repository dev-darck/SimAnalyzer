package com.project.analyzer.ac.telemetry.impl.trackmap

import com.project.analyzer.game.api.AC_KEY
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.utils.AppDirectories
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TrackMapStoreRepositoryTest {

    @Test
    fun `load resolves exact layout before falling back to layoutless map`() = runTest {
        val root = createTempDirectory("track-map-store").toFile()
        try {
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(),
            )
            val baseMap = map(layoutId = null, createdAt = 100L, pointOffset = 0f)
            val indyMap = map(layoutId = "indy", createdAt = 200L, pointOffset = 10f)

            repository.save(baseMap)
            repository.save(indyMap)

            val resolvedIndy = repository.load(gameId = "ace", trackId = "brands_hatch", layoutId = "indy")
            val resolvedMissingLayout = repository.load(gameId = "ace", trackId = "brands_hatch", layoutId = "gp")
            val resolvedDefault = repository.load(gameId = "ace", trackId = "brands_hatch")

            assertNotNull(resolvedIndy)
            assertEquals("indy", resolvedIndy.layoutId)
            assertEquals(10f, resolvedIndy.points.first().x)

            assertNotNull(resolvedMissingLayout)
            assertEquals("", resolvedMissingLayout.layoutId)
            assertEquals(0f, resolvedMissingLayout.points.first().x)

            assertNotNull(resolvedDefault)
            assertEquals("", resolvedDefault.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load without layout returns latest layout map when layoutless map is absent`() = runTest {
        val root = createTempDirectory("track-map-store-latest").toFile()
        try {
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(),
            )
            val gpMap = map(layoutId = "gp", createdAt = 100L, pointOffset = 1f)
            val indyMap = map(layoutId = "indy", createdAt = 200L, pointOffset = 2f)

            repository.save(gpMap)
            repository.save(indyMap)

            val resolved = repository.load(gameId = "ace", trackId = "brands_hatch")

            assertNotNull(resolved)
            assertEquals("indy", resolved.layoutId)
            assertEquals(2f, resolved.points.first().x)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load falls back to imported game map when user store misses track`() = runTest {
        val root = createTempDirectory("track-map-store-fallback").toFile()
        try {
            val gameMap = map(trackId = "brands_hatch_indy", layoutId = "indy", createdAt = 300L, pointOffset = 9f)
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ace",
                trackId = "brands_hatch",
                layoutId = "indy",
            )

            assertNotNull(resolved)
            assertEquals(9f, resolved.points.first().x)
            assertEquals(AC_KEY, resolved.gameId)
            assertEquals("indy", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load keeps imported svg path for game map`() = runTest {
        val root = createTempDirectory("track-map-store-svg").toFile()
        try {
            val gameMap = map(
                trackId = "suzuka_gp",
                layoutId = "gp",
                createdAt = 300L,
                pointOffset = 9f,
                svgPath = root.resolve("user/trackmaps/suzuka-gp.svg").absolutePath,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ace",
                trackId = "suzuka_gp",
                layoutId = "gp",
            )

            assertNotNull(resolved)
            assertEquals(gameMap.svgPath, resolved.svgPath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves imported alias map for runtime track id variant`() = runTest {
        val root = createTempDirectory("track-map-store-red-bull-alias").toFile()
        try {
            val gameMap = map(
                trackId = "redbull_ring_gp",
                layoutId = "gp",
                createdAt = 300L,
                pointOffset = 9f,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ac",
                trackId = "red_bull_ring_gp",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("redbull_ring_gp", resolved.trackId)
            assertEquals("gp", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves imported alias map for watkins glen common inner loop`() = runTest {
        val root = createTempDirectory("track-map-store-watkins-alias").toFile()
        try {
            val gameMap = map(
                trackId = "watkins_glen_gp_inner_loop",
                layoutId = "gp_inner_loop",
                createdAt = 300L,
                pointOffset = 9f,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ac",
                trackId = "watkins_glen_common_inner_loop",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("watkins_glen_gp_inner_loop", resolved.trackId)
            assertEquals("gp_inner_loop", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves imported alias map for truncated watkins glen no inner loop`() = runTest {
        val root = createTempDirectory("track-map-store-watkins-truncated-alias").toFile()
        try {
            val gameMap = map(
                trackId = "watkins_glen_gp",
                layoutId = "gp",
                createdAt = 300L,
                pointOffset = 9f,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ac",
                trackId = "watkins_glen_common_no_inner_loo",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("watkins_glen_gp", resolved.trackId)
            assertEquals("gp", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves imported alias map for spa camera sequence legacy id`() = runTest {
        val root = createTempDirectory("track-map-store-spa-camera-alias").toFile()
        try {
            val gameMap = map(
                trackId = "spa_gp",
                layoutId = "gp",
                createdAt = 300L,
                pointOffset = 9f,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )

            val resolved = repository.load(
                gameId = "ac",
                trackId = "spa_camera_sequence_practice",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("spa_gp", resolved.trackId)
            assertEquals("gp", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `load resolves imported alias map for brands hatch camera sequence legacy id`() = runTest {
        val root = createTempDirectory("track-map-store-brands-camera-alias").toFile()
        try {
            val gpMap = map(
                trackId = "brands_hatch_gp",
                layoutId = "gp",
                createdAt = 300L,
                pointOffset = 9f,
            )
            val indyMap = map(
                trackId = "brands_hatch_indy",
                layoutId = "indy",
                createdAt = 200L,
                pointOffset = 4f,
            )
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gpMap, indyMap)),
            )

            val resolved = repository.load(
                gameId = "ac",
                trackId = "brands_hatch_camera_sequence_pra",
                layoutId = null,
            )

            assertNotNull(resolved)
            assertEquals("brands_hatch_gp", resolved.trackId)
            assertEquals("gp", resolved.layoutId)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `loadAll keeps user map as override over imported game map`() = runTest {
        val root = createTempDirectory("track-map-store-override").toFile()
        try {
            val gameMap = map(layoutId = "indy", createdAt = 100L, pointOffset = 1f)
            val userMap = map(layoutId = "indy", createdAt = 200L, pointOffset = 7f)
            val repository = TrackMapStoreRepository(
                appDirectories = root.asAppDirectories(),
                gameProvider = FakeTrackMapGameProvider(maps = listOf(gameMap)),
            )
            repository.save(userMap)

            val allMaps = repository.loadAll("ace")

            assertEquals(1, allMaps.size)
            assertEquals(7f, allMaps.single().points.first().x)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun map(
        trackId: String = "brands_hatch",
        layoutId: String?,
        createdAt: Long,
        pointOffset: Float,
        svgPath: String? = null,
    ): TrackMap = TrackMap(
        gameId = "ace",
        trackId = trackId,
        trackName = "Brands Hatch",
        layoutId = layoutId,
        createdAtEpochMs = createdAt,
        points = listOf(
            TrackMapPoint(x = pointOffset, y = 0f),
            TrackMapPoint(x = pointOffset + 1f, y = 1f),
        ),
        svgPath = svgPath,
    )

    private fun File.asAppDirectories(): AppDirectories = object : AppDirectories {
        override val dataDir = this@asAppDirectories
        override val preferencesDir = resolve("preferences").apply { mkdirs() }
        override val cacheDir = resolve("cache").apply { mkdirs() }
        override val logsDir = resolve("logs").apply { mkdirs() }
        override val userDataDir = resolve("user").apply { mkdirs() }
        override val runtimeDir = resolve("runtime").apply { mkdirs() }
        override val lockFile = runtimeDir.resolve("app.lock")
    }

    private class FakeTrackMapGameProvider(
        private val maps: List<TrackMap> = emptyList(),
    ) : TrackMapGameProvider {

        override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? {
            val normalizedGameId = normalizeGameId(gameId)
            val normalizedLayoutId = layoutId?.trim().orEmpty()
            val candidates = maps.filter { normalizeGameId(it.gameId) == normalizedGameId && it.trackId == trackId }
            return if (normalizedLayoutId.isBlank()) {
                candidates.firstOrNull()
            } else {
                candidates.firstOrNull { it.layoutId.orEmpty() == normalizedLayoutId }
            }
        }

        override suspend fun loadAll(gameId: String?): List<TrackMap> = if (gameId.isNullOrBlank()) {
            maps
        } else {
            val normalizedGameId = normalizeGameId(gameId)
            maps.filter { normalizeGameId(it.gameId) == normalizedGameId }
        }

        private fun normalizeGameId(gameId: String): String = when (gameId.trim().lowercase()) {
            "ac", "ace", AC_KEY -> AC_KEY
            else -> gameId.trim().lowercase()
        }
    }
}
