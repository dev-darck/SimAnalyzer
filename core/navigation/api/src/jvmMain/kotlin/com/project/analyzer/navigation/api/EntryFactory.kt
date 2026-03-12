package com.project.analyzer.navigation.api

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavEntry

@Immutable
public interface EntryFactory {
    public fun create(): (Route) -> NavEntry<Route>
}
