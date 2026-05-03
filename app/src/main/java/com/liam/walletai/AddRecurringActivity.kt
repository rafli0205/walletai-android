package com.liam.walletai

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.liam.walletai.data.DatabaseProvider
import com.liam.walletai.data.RecurringExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddRecurringActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var spType: Spinner
    private lateinit var etAmount: EditText
    private lateinit var etCategory: EditText
    private lateinit var etAccount: EditText
    private lateinit var etDayOfMonth: EditText
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recurring)

        etTitle = findViewById(R.id.etTitle)
        spType = findViewById(R.id.spType)
        etAmount = findViewById(R.id.etAmount)
        etCategory = findViewById(R.id.etCategory)
        etAccount = findViewById(R.id.etAccount)
        etDayOfMonth = findViewById(R.id.etDayOfMonth)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Income", "Expense")
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spType.adapter = typeAdapter

        btnSave.setOnClickListener {
            saveRecurring()
        }
    }

    private fun saveRecurring() {
        val title = etTitle.text.toString().trim()
        val type = spType.selectedItem as String
        val amountText = etAmount.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val account = etAccount.text.toString().trim()
        val dayText = etDayOfMonth.text.toString().trim()
        val note = etNote.text.toString().trim()

        if (title.isEmpty() || amountText.isEmpty() || category.isEmpty() || account.isEmpty() || dayText.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        val day = dayText.toIntOrNull()

        if (amount == null || day == null || day !in 1..31) {
            Toast.makeText(this, "Invalid amount or day", Toast.LENGTH_SHORT).show()
            return
        }

        val db = DatabaseProvider.getDatabase(this)
        val dao = db.recurringExpenseDao()

        val entity = RecurringExpenseEntity(
            title = title,
            category = category,
            accountName = account,
            amount = amount,
            type = type,
            note = note,
            dayOfMonth = day,
            isActive = true
        )

        lifecycleScope.launch(Dispatchers.IO) {
            dao.insert(entity)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@AddRecurringActivity, "Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
