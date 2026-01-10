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

private const val UserPrefName = "app_preferences.preferences_pb"
private const val SessionPrefName = "session_preferences.preferences_pb"

@BindingContainer
@ContributesTo(AppScope::class)
object PreferenceBindings {

    @Provides
    @UserPref
    @SingleIn(AppScope::class)
    fun provideUserPref(directories: AppDirectories): Preference = PreferenceImpl(
        directories = directories,
        preferenceName = UserPrefName
    )

    @Provides
    @SessionPref
    @SingleIn(AppScope::class)
    fun provideSessionPref(directories: AppDirectories): Preference = PreferenceImpl(
        directories = directories,
        preferenceName = SessionPrefName
    )
}
