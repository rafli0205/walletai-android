package com.liam.walletai

import android.content.Context

class UserPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("wallet_ai_prefs", Context.MODE_PRIVATE)

    fun getUserName(): String? = prefs.getString("user_name", null)

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
    }

    fun isOnboardingShown(): Boolean =
        prefs.getBoolean("onboarding_shown", false)

    fun setOnboardingShown(shown: Boolean) {
        prefs.edit().putBoolean("onboarding_shown", shown).apply()
    }

    fun isRecurringReminderEnabled(): Boolean =
        prefs.getBoolean("recurring_reminder_enabled", true)

    fun setRecurringReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("recurring_reminder_enabled", enabled).apply()
    }

    fun isBudgetAlertEnabled(): Boolean =
        prefs.getBoolean("budget_alert_enabled", true)

    fun setBudgetAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("budget_alert_enabled", enabled).apply()
    }

    fun getString(key: String, default: String? = null): String? =
        prefs.getString(key, default)

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getPin(): String? = prefs.getString("lock_pin", null)

    fun setPin(pin: String?) {
        prefs.edit().putString("lock_pin", pin).apply()
    }

    fun isLockEnabled(): Boolean =
        prefs.getBoolean("lock_enabled", false) && !getPin().isNullOrEmpty()

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("lock_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean("biometric_enabled", false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isSessionUnlocked(): Boolean =
        prefs.getBoolean("lock_session_unlocked", false)

    fun setSessionUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean("lock_session_unlocked", unlocked).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}