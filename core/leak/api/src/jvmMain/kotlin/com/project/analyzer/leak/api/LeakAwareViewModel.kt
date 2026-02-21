package com.project.analyzer.leak.api

import androidx.lifecycle.ViewModel

public abstract class LeakAwareViewModel : ViewModel() {

    init {
        addCloseable {
            val name = this::class.qualifiedName ?: "ViewModel"
            LeakCanaryRuntime.watch(this, name)
        }
    }
}
