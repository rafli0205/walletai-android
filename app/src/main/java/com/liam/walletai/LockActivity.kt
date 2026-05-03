package com.liam.walletai

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LockActivity : AppCompatActivity() {

    private lateinit var tvLockSubtitle: TextView
    private lateinit var tilPin: TextInputLayout
    private lateinit var etPin: TextInputEditText
    private lateinit var btnBiometric: MaterialButton
    private lateinit var btnUnlock: MaterialButton
    private lateinit var btnForgot: MaterialButton

    private lateinit var prefs: UserPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        prefs = UserPrefs(this)

        tvLockSubtitle = findViewById(R.id.tvLockSubtitle)
        tilPin = findViewById(R.id.tilPin)
        etPin = findViewById(R.id.etPin)
        btnBiometric = findViewById(R.id.btnBiometric)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnForgot = findViewById(R.id.btnForgot)

        val biometricAvailable = canUseBiometric()
        val biometricEnabled = prefs.isBiometricEnabled()

        btnBiometric.visibility =
            if (biometricAvailable && biometricEnabled) android.view.View.VISIBLE
            else android.view.View.GONE

        tvLockSubtitle.text =
            if (biometricAvailable && biometricEnabled) {
                "Use biometric or enter your PIN to continue"
            } else {
                "Enter your PIN to continue"
            }

        btnBiometric.setOnClickListener {
            showBiometricPrompt()
        }

        btnUnlock.setOnClickListener {
            unlockWithPin()
        }

        btnForgot.setOnClickListener {
            Toast.makeText(
                this,
                "Lock disabled. Set a new PIN in Settings.",
                Toast.LENGTH_LONG
            ).show()
            prefs.setBiometricEnabled(false)
            prefs.setLockEnabled(false)
            prefs.setPin(null)
            prefs.setSessionUnlocked(true)
            finish()
        }

        if (biometricAvailable && biometricEnabled) {
            btnBiometric.post {
                showBiometricPrompt()
            }
        }
    }

    private fun unlockWithPin() {
        val input = etPin.text?.toString()?.trim().orEmpty()
        val savedPin = prefs.getPin().orEmpty()

        tilPin.error = null

        if (savedPin.isEmpty()) {
            prefs.setSessionUnlocked(true)
            finish()
            return
        }

        if (input.isEmpty()) {
            tilPin.error = "PIN wajib diisi"
            return
        }

        if (input == savedPin) {
            prefs.setSessionUnlocked(true)
            Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            tilPin.error = "PIN salah"
            etPin.setText("")
        }
    }

    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    prefs.setSessionUnlocked(true)
                    Toast.makeText(this@LockActivity, "Unlocked", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(
                            this@LockActivity,
                            errString,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@LockActivity,
                        "Biometric not recognized",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock WalletAI")
            .setSubtitle("Authenticate to continue")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}