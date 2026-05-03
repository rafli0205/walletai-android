package com.liam.walletai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.liam.walletai.data.DatabaseProvider
import com.liam.walletai.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rowBudgetSettings: LinearLayout
    private lateinit var rowExportCsv: LinearLayout
    private lateinit var rowImportCsv: LinearLayout
    private lateinit var switchRecurringReminder: Switch
    private lateinit var switchBudgetAlert: Switch
    private lateinit var tvVersionInfo: TextView

    private lateinit var rowAppLock: LinearLayout
    private lateinit var switchAppLock: Switch
    private lateinit var rowBiometric: LinearLayout
    private lateinit var switchBiometric: Switch
    private lateinit var userPrefs: UserPrefs

    private var isUpdatingLockSwitch = false
    private var isUpdatingBiometricSwitch = false

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    private val importCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                importFromCsv(uri)
            } else {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        userPrefs = UserPrefs(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvVersionInfo.text = "Version 1.0.0 • made with WalletAI Team"

        bindPreferenceState()
        setupPreferenceListeners()
        setupActionRows()
        setupAppLock()
        setupBiometric()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbarSettings)
        rowBudgetSettings = findViewById(R.id.rowBudgetSettings)
        rowExportCsv = findViewById(R.id.rowExportCsv)
        rowImportCsv = findViewById(R.id.rowImportCsv)
        switchRecurringReminder = findViewById(R.id.switchRecurringReminder)
        switchBudgetAlert = findViewById(R.id.switchBudgetAlert)
        tvVersionInfo = findViewById(R.id.tvVersionInfo)

        rowAppLock = findViewById(R.id.rowAppLock)
        switchAppLock = findViewById(R.id.switchAppLock)

        rowBiometric = findViewById(R.id.rowBiometric)
        switchBiometric = findViewById(R.id.switchBiometric)
    }

    private fun bindPreferenceState() {
        switchRecurringReminder.isChecked = userPrefs.isRecurringReminderEnabled()
        switchBudgetAlert.isChecked = userPrefs.isBudgetAlertEnabled()
        switchAppLock.isChecked = userPrefs.isLockEnabled()
        switchBiometric.isChecked = userPrefs.isBiometricEnabled()

        val lockEnabled = userPrefs.isLockEnabled()
        rowBiometric.alpha = if (lockEnabled) 1f else 0.5f
        switchBiometric.isEnabled = lockEnabled
        rowBiometric.isEnabled = lockEnabled
    }

    private fun setupPreferenceListeners() {
        switchRecurringReminder.setOnCheckedChangeListener { _, isChecked ->
            userPrefs.setRecurringReminderEnabled(isChecked)
        }

        switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            userPrefs.setBudgetAlertEnabled(isChecked)
        }
    }

    private fun setupActionRows() {
        rowBudgetSettings.setOnClickListener {
            startActivity(Intent(this, BudgetSettingsActivity::class.java))
        }

        rowExportCsv.setOnClickListener {
            showExportOptionsDialog()
        }

        rowImportCsv.setOnClickListener {
            startImportPicker()
        }
    }

    private fun setupAppLock() {
        rowAppLock.setOnClickListener {
            switchAppLock.performClick()
        }

        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingLockSwitch) return@setOnCheckedChangeListener

            if (isChecked) {
                if (userPrefs.getPin().isNullOrEmpty()) {
                    showSetPinDialog(enableLockAfter = true)
                } else {
                    userPrefs.setLockEnabled(true)
                    userPrefs.setSessionUnlocked(false)
                    updateBiometricAvailability(true)
                    Toast.makeText(this, "App lock enabled", Toast.LENGTH_SHORT).show()
                }
            } else {
                userPrefs.setLockEnabled(false)
                userPrefs.setBiometricEnabled(false)
                userPrefs.setSessionUnlocked(true)
                updateBiometricAvailability(false)
                Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBiometric() {
        rowBiometric.setOnClickListener {
            if (switchBiometric.isEnabled) {
                switchBiometric.performClick()
            }
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingBiometricSwitch) return@setOnCheckedChangeListener

            if (!userPrefs.isLockEnabled()) {
                setBiometricSwitchCheckedSafely(false)
                Toast.makeText(this, "Enable app lock first", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                if (canUseBiometric()) {
                    userPrefs.setBiometricEnabled(true)
                    Toast.makeText(this, "Biometric unlock enabled", Toast.LENGTH_SHORT).show()
                } else {
                    setBiometricSwitchCheckedSafely(false)
                    Toast.makeText(
                        this,
                        "Biometric is not available on this device",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                userPrefs.setBiometricEnabled(false)
                Toast.makeText(this, "Biometric unlock disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBiometricAvailability(lockEnabled: Boolean) {
        rowBiometric.alpha = if (lockEnabled) 1f else 0.5f
        switchBiometric.isEnabled = lockEnabled
        rowBiometric.isEnabled = lockEnabled

        if (!lockEnabled) {
            setBiometricSwitchCheckedSafely(false)
        }
    }

    private fun setLockSwitchCheckedSafely(checked: Boolean) {
        isUpdatingLockSwitch = true
        switchAppLock.isChecked = checked
        isUpdatingLockSwitch = false
    }

    private fun setBiometricSwitchCheckedSafely(checked: Boolean) {
        isUpdatingBiometricSwitch = true
        switchBiometric.isChecked = checked
        isUpdatingBiometricSwitch = false
    }

    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showSetPinDialog(enableLockAfter: Boolean) {
        val view = layoutInflater.inflate(R.layout.dialog_set_pin, null)
        val etPin1 = view.findViewById<EditText>(R.id.etPin1)
        val etPin2 = view.findViewById<EditText>(R.id.etPin2)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set PIN")
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                etPin1.error = null
                etPin2.error = null

                val p1 = etPin1.text.toString().trim()
                val p2 = etPin2.text.toString().trim()

                when {
                    p1.length < 4 -> {
                        etPin1.error = "PIN minimal 4 digit"
                    }

                    p2.isEmpty() -> {
                        etPin2.error = "Konfirmasi PIN wajib diisi"
                    }

                    p1 != p2 -> {
                        etPin2.error = "PIN tidak sama"
                    }

                    else -> {
                        userPrefs.setPin(p1)

                        if (enableLockAfter) {
                            userPrefs.setLockEnabled(true)
                            userPrefs.setSessionUnlocked(false)
                            setLockSwitchCheckedSafely(true)
                            updateBiometricAvailability(true)
                            Toast.makeText(
                                this,
                                "PIN saved, app lock enabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(this, "PIN saved", Toast.LENGTH_SHORT).show()
                        }

                        dialog.dismiss()
                    }
                }
            }

            negativeButton.setOnClickListener {
                if (enableLockAfter) {
                    setLockSwitchCheckedSafely(false)
                    updateBiometricAvailability(false)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("All time", "This month", "Custom range")
        AlertDialog.Builder(this)
            .setTitle("Export period")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportAllTime()
                    1 -> exportThisMonth()
                    2 -> exportCustomRange()
                }
            }
            .show()
    }

    private fun exportAllTime() {
        val from = Long.MIN_VALUE
        val to = Long.MAX_VALUE
        ExportCsvHelper(this, db).exportTransactionsCsv(from, to, "all")
    }

    private fun exportThisMonth() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis

        val to = System.currentTimeMillis()

        val label = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(from)
        ExportCsvHelper(this, db).exportTransactionsCsv(from, to, label)
    }

    private fun exportCustomRange() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select export range")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { range ->
            val from = range.first ?: return@addOnPositiveButtonClickListener
            val to = range.second ?: return@addOnPositiveButtonClickListener

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val label = "${sdf.format(from)}_${sdf.format(to)}"

            ExportCsvHelper(this, db).exportTransactionsCsv(from, to, label)
        }

        dateRangePicker.show(supportFragmentManager, "EXPORT_DATE_RANGE")
    }

    private fun startImportPicker() {
        importCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel"))
    }

    private fun importFromCsv(uri: Uri) {
        val contentResolver = contentResolver
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot open file")
                val reader = BufferedReader(InputStreamReader(inputStream))

                val lines = reader.readLines()
                if (lines.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Empty CSV file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val dataLines = lines.drop(1)
                val dao = db.transactionDao()

                var importedCount = 0
                for (line in dataLines) {
                    if (line.isBlank()) continue
                    val cols = parseCsvLine(line)
                    if (cols.size < 10) continue

                    val title = cols[1]
                    val amount = cols[2].toDoubleOrNull() ?: continue
                    val category = cols[3]
                    val account = cols[4]
                    val type = cols[5]
                    val date = cols[6]
                    val dateMillis = cols[7].toLongOrNull() ?: 0L
                    val notes = cols[8]
                    val receiptImageUri = cols[9].ifBlank { null }

                    val entity = TransactionEntity(
                        id = 0,
                        title = title,
                        amount = amount,
                        category = category,
                        account = account,
                        type = type,
                        date = date,
                        dateMillis = dateMillis,
                        notes = notes,
                        receiptImageUri = receiptImageUri
                    )

                    dao.insertTransaction(entity)
                    importedCount++
                }

                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Imported $importedCount transactions",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Import failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}