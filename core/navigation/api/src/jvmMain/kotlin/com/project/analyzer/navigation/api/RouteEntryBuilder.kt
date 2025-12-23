package com.project.analyzer.navigation.api

import androidx.navigation3.runtime.EntryProviderScope

public fun interface RouteEntryBuilder {
    public fun EntryProviderScope<Route>.build()
}
