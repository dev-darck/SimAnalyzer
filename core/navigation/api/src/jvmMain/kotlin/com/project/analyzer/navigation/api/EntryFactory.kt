package com.project.analyzer.navigation.api

import androidx.navigation3.runtime.NavEntry

public interface EntryFactory {
    public fun create(): (Route) -> NavEntry<Route>
}
