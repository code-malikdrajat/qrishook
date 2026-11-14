package com.qrishook.app.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Preferensi non-rahasia (flag onboarding). */
@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()
        }

    companion object {
        private const val FILE_NAME = "qrishook_app"
        private const val KEY_ONBOARDING = "onboarding_done"
    }
}
