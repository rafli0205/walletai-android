package com.liam.walletai

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.liam.walletai.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetSettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    // Misal kamu pakai 4 kategori utama dulu; sesuaikan dengan yang ada di app
    private lateinit var etFoodBudget: EditText
    private lateinit var etTransportBudget: EditText
    private lateinit var etShoppingBudget: EditText
    private lateinit var etBillsBudget: EditText

    private val db by lazy { DatabaseProvider.getDatabase(this) }
    private val prefs by lazy { UserPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_settings)

        toolbar = findViewById(R.id.toolbarBudgetSettings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Category budgets"
        toolbar.setNavigationOnClickListener { finish() }

        etFoodBudget = findViewById(R.id.etFoodBudget)
        etTransportBudget = findViewById(R.id.etTransportBudget)
        etShoppingBudget = findViewById(R.id.etShoppingBudget)
        etBillsBudget = findViewById(R.id.etBillsBudget)

        // Load nilai awal budget (sementara pakai SharedPreferences sederhana lewat UserPrefs)
        loadBudgetsFromPrefs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_budget, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_budgets -> {
                saveBudgets()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadBudgetsFromPrefs() {
        // Sementara simpan budget di SharedPreferences via UserPrefs (bisa kamu pindah ke Room nanti)
        etFoodBudget.setText(prefs.getString("budget_food", "") ?: "")
        etTransportBudget.setText(prefs.getString("budget_transport", "") ?: "")
        etShoppingBudget.setText(prefs.getString("budget_shopping", "") ?: "")
        etBillsBudget.setText(prefs.getString("budget_bills", "") ?: "")
    }

    private fun saveBudgets() {
        val food = etFoodBudget.text.toString().trim()
        val transport = etTransportBudget.text.toString().trim()
        val shopping = etShoppingBudget.text.toString().trim()
        val bills = etBillsBudget.text.toString().trim()

        // Validasi ringan
        if (!isValidNumberOrEmpty(food) ||
            !isValidNumberOrEmpty(transport) ||
            !isValidNumberOrEmpty(shopping) ||
            !isValidNumberOrEmpty(bills)
        ) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            return
        }

        // Simpan ke prefs (bisa kamu ganti ke tabel Budget di Room kalau sudah siap)
        prefs.setString("budget_food", food)
        prefs.setString("budget_transport", transport)
        prefs.setString("budget_shopping", shopping)
        prefs.setString("budget_bills", bills)

        Toast.makeText(this, "Budgets saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isValidNumberOrEmpty(text: String): Boolean {
        if (text.isEmpty()) return true
        return text.toDoubleOrNull() != null
    }
}
