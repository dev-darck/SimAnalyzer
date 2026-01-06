package com.project.analyzer.preference.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.project.analyzer.preference.api.Key
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.utils.AppDirectories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

class PreferenceImpl(
    private val directories: AppDirectories,
    preferenceName: String
) : Preference {

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
        ) {
            File(directories.preferencesDir, preferenceName)
        }
    }

    override suspend fun <T> put(entry: Pair<Key<T>, T>) {
        val (key, value) = entry
        val prefsKey = key.toPreferencesKey()
        dataStore.edit { preferences ->
            preferences[prefsKey] = value
        }
    }

    override suspend fun <T> get(key: Key<T>, default: T): T = getOrNull(key) ?: default

    override suspend fun <T> getOrNull(key: Key<T>): T? {
        val prefsKey = key.toPreferencesKey()
        return dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .first()[prefsKey]
    }

    override fun <T> observe(key: Key<T>): Flow<T?> {
        val prefsKey = key.toPreferencesKey()
        return dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { preferences -> preferences[prefsKey] }
    }

    override fun <T> observe(
        key: Key<T>,
        default: T
    ): Flow<T> = observe(key).map { it ?: default }

    override suspend fun <T> contains(key: Key<T>): Boolean {
        val prefsKey = key.toPreferencesKey()
        return dataStore.data.first().contains(prefsKey)
    }

    override suspend fun <T> remove(key: Key<T>) {
        val prefsKey = key.toPreferencesKey()
        dataStore.edit { preferences ->
            preferences.remove(prefsKey)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
