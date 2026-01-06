package com.project.analyzer.hud.api

import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(HudScope::class)
public interface HudGraph {

    @Multibinds(allowEmpty = true)
    public val hudPanels: Set<HudPanel>
}
