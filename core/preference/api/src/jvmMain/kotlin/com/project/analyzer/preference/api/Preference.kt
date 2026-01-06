package com.project.analyzer.preference.api

import kotlinx.coroutines.flow.Flow

public interface Preference {

    public suspend fun <T> put(entry: Pair<Key<T>, T>)
    public suspend fun <T> get(key: Key<T>, default: T): T
    public suspend fun <T> getOrNull(key: Key<T>): T?
    public fun <T> observe(key: Key<T>): Flow<T?>
    public fun <T> observe(key: Key<T>, default: T): Flow<T>
    public suspend fun <T> contains(key: Key<T>): Boolean
    public suspend fun <T> remove(key: Key<T>)
    public suspend fun clear()
}
