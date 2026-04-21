package com.project.analyzer.ac.telemetry.impl.shm

import kotlin.test.Test
import kotlin.test.assertEquals

class AcShmNamesTest {

    @Test
    fun `defaults prefer AC Evo 0_6 shared memory names and keep legacy fallback`() {
        val names = AcShmNames()

        assertEquals(
            AcShmLayoutNames(
                physics = "Local\\acevo_pmf_physics",
                graphics = "Local\\acevo_pmf_graphics",
                statics = "Local\\acevo_pmf_static",
            ),
            names.evo,
        )
        assertEquals(
            AcShmLayoutNames(
                physics = "Local\\acpmf_physics",
                graphics = "Local\\acpmf_graphics",
                statics = "Local\\acpmf_static",
            ),
            names.legacy,
        )
    }
}
