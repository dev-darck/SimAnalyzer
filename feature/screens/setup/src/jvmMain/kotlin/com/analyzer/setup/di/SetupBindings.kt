package com.analyzer.setup.di

import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo

@ContributesTo(ScreenScope::class)
@BindingContainer
interface SetupBindings
