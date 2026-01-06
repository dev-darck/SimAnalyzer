package com.project.analyzer.preference.impl

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.project.analyzer.preference.api.BooleanPrefKey
import com.project.analyzer.preference.api.DoublePrefKey
import com.project.analyzer.preference.api.FloatPrefKey
import com.project.analyzer.preference.api.IntPrefKey
import com.project.analyzer.preference.api.Key
import com.project.analyzer.preference.api.LongPrefKey
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.preference.api.StringSetPrefKey

@Suppress("UNCHECKED_CAST")
internal fun <T> Key<T>.toPreferencesKey(): Preferences.Key<T> = when (this) {
    is StringPrefKey -> stringPreferencesKey(name)
    is IntPrefKey -> intPreferencesKey(name)
    is LongPrefKey -> longPreferencesKey(name)
    is FloatPrefKey -> floatPreferencesKey(name)
    is DoublePrefKey -> doublePreferencesKey(name)
    is BooleanPrefKey -> booleanPreferencesKey(name)
    is StringSetPrefKey -> stringSetPreferencesKey(name)
} as Preferences.Key<T>
