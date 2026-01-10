package com.project.analyzer.hud.api

import dev.zacsweers.metro.Multibinds

public interface HudGraph {

    @Multibinds(allowEmpty = true)
    public val hudPanels: Set<HudPanel>
}
