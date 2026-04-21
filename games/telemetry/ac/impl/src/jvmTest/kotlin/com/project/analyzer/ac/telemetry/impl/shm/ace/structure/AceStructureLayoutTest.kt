package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.sun.jna.Structure
import kotlin.test.Test
import kotlin.test.assertEquals

class AceStructureLayoutTest {

    @Test
    fun `graphics page matches documented size and key offsets`() {
        val structure = AcEvoGraphicsPageView()

        assertEquals(AcEvoGraphicsPageView.SIZE_BYTES, structure.size())
        assertEquals(40, structure.fieldOffset("rpmRaw"))
        assertEquals(58, structure.fieldOffset("displaySpeedKmhRaw"))
        assertEquals(220, structure.fieldOffset("tyreLf"))
        assertEquals(1260, structure.fieldOffset("carDamage"))
        assertEquals(3020, structure.fieldOffset("driverNameRaw"))
        assertEquals(3119, structure.fieldOffset("isInPitBoxRaw"))
    }

    @Test
    fun `embedded page structures keep documented fixed sizes`() {
        assertEquals(AcEvoTyreStateView.SIZE_BYTES, AcEvoTyreStateView().size())
        assertEquals(AcEvoDamageStateView.SIZE_BYTES, AcEvoDamageStateView().size())
        assertEquals(AcEvoPitInfoView.SIZE_BYTES, AcEvoPitInfoView().size())
        assertEquals(AcEvoElectronicsView.SIZE_BYTES, AcEvoElectronicsView().size())
        assertEquals(AcEvoInstrumentationView.SIZE_BYTES, AcEvoInstrumentationView().size())
        assertEquals(AcEvoSessionStateView.SIZE_BYTES, AcEvoSessionStateView().size())
        assertEquals(AcEvoTimingStateView.SIZE_BYTES, AcEvoTimingStateView().size())
        assertEquals(AcEvoAssistsStateView.SIZE_BYTES, AcEvoAssistsStateView().size())
        assertEquals(AcEvoStaticPageView.SIZE_BYTES, AcEvoStaticPageView().size())
    }

    @Test
    fun `static page matches documented size and key offsets`() {
        val structure = AcEvoStaticPageView()

        assertEquals(AcEvoStaticPageView.SIZE_BYTES, structure.size())
        assertEquals(32, structure.fieldOffset("sessionRaw"))
        assertEquals(36, structure.fieldOffset("sessionNameRaw"))
        assertEquals(72, structure.fieldOffset("startingGripRaw"))
        assertEquals(136, structure.fieldOffset("trackRaw"))
        assertEquals(204, structure.fieldOffset("trackLengthMRaw"))
    }

    private fun Structure.fieldOffset(name: String): Int {
        val method = Structure::class.java.getDeclaredMethod("fieldOffset", String::class.java)
        method.isAccessible = true
        return method.invoke(this, name) as Int
    }
}
