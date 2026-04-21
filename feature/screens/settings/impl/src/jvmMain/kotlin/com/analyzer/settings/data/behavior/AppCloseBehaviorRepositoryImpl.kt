package com.analyzer.settings.data.behavior

import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.api.AppCloseBehaviorRepository
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.str
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
internal class AppCloseBehaviorRepositoryImpl(
    @param:UserPref
    private val preference: Preference,
) : AppCloseBehaviorRepository {

    override suspend fun loadCloseBehavior(): AppCloseBehavior =
        AppCloseBehavior.fromPreference(preference.get(KEY_APP_CLOSE_BEHAVIOR.str, AppCloseBehavior.AskEveryTime.name))

    override fun observeCloseBehavior(): Flow<AppCloseBehavior> =
        preference.observe(KEY_APP_CLOSE_BEHAVIOR.str, AppCloseBehavior.AskEveryTime.name)
            .map(AppCloseBehavior::fromPreference)

    override suspend fun setCloseBehavior(behavior: AppCloseBehavior) {
        preference.put(KEY_APP_CLOSE_BEHAVIOR.str to behavior.name)
    }

    private companion object {

        const val KEY_APP_CLOSE_BEHAVIOR = "app_close_behavior"
    }
}
