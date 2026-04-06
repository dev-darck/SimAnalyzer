package com.analyzer.session.analysis.presentation.cache

import com.analyzer.session.analysis.presentation.model.SessionAnalysisState

internal class SelectionStateCache(private val maxEntries: Int) :
    LinkedHashMap<SelectionStateKey, SessionAnalysisState>(maxEntries, 0.75f, true) {

    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<SelectionStateKey, SessionAnalysisState>?,
    ): Boolean = size > maxEntries
}
