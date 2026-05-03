package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = UserPrefs(this)
        decideNext(prefs)
    }

    private fun decideNext(prefs: UserPrefs) {
        if (!prefs.isOnboardingShown()) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        if (prefs.isLockEnabled()) {
            prefs.setSessionUnlocked(false)
            startActivity(Intent(this, LockActivity::class.java))
            finish()
            return
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}