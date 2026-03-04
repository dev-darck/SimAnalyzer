package com.project.analyzer.preference.impl

import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.SessionPref
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

private const val USER_PREF_NAME = "app_preferences.preferences_pb"
private const val SESSION_PREF_NAME = "session_preferences.preferences_pb"

@BindingContainer
@ContributesTo(AppScope::class)
interface PreferenceBindings {
    companion object {

        @Provides
        @UserPref
        @SingleIn(AppScope::class)
        private fun provideUserPref(directories: AppDirectories): Preference = PreferenceImpl(
            directories = directories,
            preferenceName = USER_PREF_NAME,
        )

        @Provides
        @SessionPref
        @SingleIn(AppScope::class)
        private fun provideSessionPref(directories: AppDirectories): Preference = PreferenceImpl(
            directories = directories,
            preferenceName = SESSION_PREF_NAME,
        )
    }
}
