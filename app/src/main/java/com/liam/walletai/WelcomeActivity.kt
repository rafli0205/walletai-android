package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class WelcomeActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvCountry: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        tvWelcome = findViewById(R.id.tvWelcome)
        tvCountry = findViewById(R.id.tvCountry)

        tvWelcome.text = "Welcome"

        val countryIso = detectCountryIso().uppercase(Locale.ROOT)
        val flag = isoToFlagEmoji(countryIso)
        tvCountry.text = "$flag $countryIso"

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1800)
    }

    private fun detectCountryIso(): String {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val networkIso = try {
            telephonyManager.networkCountryIso
        } catch (e: Exception) {
            ""
        }

        if (!networkIso.isNullOrBlank()) {
            return networkIso
        }

        val localeIso = Locale.getDefault().country
        if (localeIso.isNotBlank()) {
            return localeIso
        }

        return "ID"
    }

    private fun isoToFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🏳️"

        val upper = countryCode.uppercase(Locale.ROOT)
        val first = upper[0].code - 'A'.code + 0x1F1E6
        val second = upper[1].code - 'A'.code + 0x1F1E6

        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
