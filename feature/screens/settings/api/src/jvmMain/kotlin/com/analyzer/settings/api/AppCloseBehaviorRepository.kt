package com.analyzer.settings.api

import kotlinx.coroutines.flow.Flow

public interface AppCloseBehaviorRepository {

    public suspend fun loadCloseBehavior(): AppCloseBehavior
    public fun observeCloseBehavior(): Flow<AppCloseBehavior>
    public suspend fun setCloseBehavior(behavior: AppCloseBehavior)
}
