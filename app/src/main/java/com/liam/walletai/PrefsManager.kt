package com.liam.walletai

import android.content.Context

class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("walletai_prefs", Context.MODE_PRIVATE)

    fun isOnboardingDone(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingDone(value: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
    }

    companion object {
        private const val KEY_ONBOARDING_DONE = "key_onboarding_done"
    }
}
