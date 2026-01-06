package com.project.analyzer.preference.api

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
public interface PreferenceGraph {

    @UserPref
    public val preference: Preference
}
