package com.liam.walletai

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var etProfileName: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton

    private lateinit var userPrefs: UserPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        toolbar = findViewById(R.id.toolbarProfile)
        etProfileName = findViewById(R.id.etProfileName)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        userPrefs = UserPrefs(this)

        // load nama tersimpan
        val savedName = userPrefs.getUserName()
        if (!savedName.isNullOrBlank()) {
            etProfileName.setText(savedName)
        }

        btnSaveProfile.setOnClickListener {
            val name = etProfileName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                etProfileName.error = "Name wajib diisi"
                return@setOnClickListener
            }
            val prefs = UserPrefs(this)
            prefs.setUserName(name)
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
