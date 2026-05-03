package com.liam.walletai

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.liam.walletai.data.DatabaseProvider
import com.liam.walletai.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilAmount: TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var tilAccount: TextInputLayout
    private lateinit var tilType: TextInputLayout
    private lateinit var tilDate: TextInputLayout
    private lateinit var tilNotes: TextInputLayout

    private lateinit var etTitle: TextInputEditText
    private lateinit var etAmount: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var actAccount: AutoCompleteTextView
    private lateinit var actType: AutoCompleteTextView
    private lateinit var btnSave: MaterialButton

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    private var selectedDateMillis: Long? = null
    private var receiptImageUri: String? = null
    private var editTransactionId: Int? = null

    private val categories = listOf("Food", "Transport", "Shopping", "Bills", "Salary", "Other")
    private val accounts = listOf("Cash", "BCA", "Mandiri", "GoPay", "OVO", "Dana")
    private val types = listOf("Expense", "Income")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        bindViews()
        setupToolbar()
        setupDropdowns()
        setupAmountFormatting()
        setupDatePicker()

        editTransactionId = intent.getIntExtra("transaction_id", 0).takeIf { it != 0 }

        if (editTransactionId != null) {
            loadTransactionForEdit(editTransactionId!!)
        } else {
            prefillFromScan()
            prefillDefaultDateIfNeeded()
        }

        setupSaveButton()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbarAddTransaction)

        tilTitle = findViewById(R.id.tilTitle)
        tilAmount = findViewById(R.id.tilAmount)
        tilCategory = findViewById(R.id.tilCategory)
        tilAccount = findViewById(R.id.tilAccount)
        tilType = findViewById(R.id.tilType)
        tilDate = findViewById(R.id.tilDate)
        tilNotes = findViewById(R.id.tilNotes)

        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        etDate = findViewById(R.id.etDate)
        etNotes = findViewById(R.id.etNotes)
        actCategory = findViewById(R.id.actCategory)
        actAccount = findViewById(R.id.actAccount)
        actType = findViewById(R.id.actType)
        btnSave = findViewById(R.id.btnSaveTransaction)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Transaction"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            categories
        )
        val accountAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            accounts
        )
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            types
        )

        actCategory.setAdapter(categoryAdapter)
        actAccount.setAdapter(accountAdapter)
        actType.setAdapter(typeAdapter)

        actCategory.setText(categories.first(), false)
        actAccount.setText(accounts.first(), false)
        actType.setText(types.first(), false)

        actCategory.setOnClickListener { actCategory.showDropDown() }
        actAccount.setOnClickListener { actAccount.showDropDown() }
        actType.setOnClickListener { actType.showDropDown() }

        actCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) actCategory.showDropDown()
        }
        actAccount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) actAccount.showDropDown()
        }
        actType.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) actType.showDropDown()
        }
    }

    private fun setupAmountFormatting() {
        etAmount.imeOptions = EditorInfo.IME_ACTION_NEXT

        etAmount.doAfterTextChanged {
            tilAmount.error = null
        }

        etTitle.doAfterTextChanged {
            tilTitle.error = null
        }

        etNotes.doAfterTextChanged {
            tilNotes.error = null
        }

        actCategory.doAfterTextChanged {
            tilCategory.error = null
        }

        actAccount.doAfterTextChanged {
            tilAccount.error = null
        }

        actType.doAfterTextChanged {
            tilType.error = null
        }

        etDate.doAfterTextChanged {
            tilDate.error = null
        }
    }

    private fun setupDatePicker() {
        fun openDatePicker() {
            val builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select transaction date")

            selectedDateMillis?.let { builder.setSelection(it) }

            val datePicker = builder.build()

            datePicker.addOnPositiveButtonClickListener { millis ->
                selectedDateMillis = millis
                etDate.setText(formatDisplayDate(millis))
                tilDate.error = null
            }

            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        etDate.setOnClickListener { openDatePicker() }
        tilDate.setEndIconOnClickListener { openDatePicker() }
    }

    private fun prefillDefaultDateIfNeeded() {
        if (selectedDateMillis == null) {
            val now = System.currentTimeMillis()
            selectedDateMillis = now
            etDate.setText(formatDisplayDate(now))
        }
    }

    private fun prefillFromScan() {
        val amountExtra = intent.getDoubleExtra("prefill_amount", -1.0)
        if (amountExtra > 0) {
            etAmount.setText(formatAmountForInput(amountExtra))
        }

        val notesExtra = intent.getStringExtra("prefill_notes")
        if (!notesExtra.isNullOrBlank()) {
            etNotes.setText(notesExtra.trim())
        }

        val prefillCategory = intent.getStringExtra("prefill_category")
        if (!prefillCategory.isNullOrBlank()) {
            val adapter = actCategory.adapter
            val index = (0 until adapter.count).firstOrNull { i ->
                (adapter.getItem(i) as? String).equals(prefillCategory, ignoreCase = true)
            }
            if (index != null) {
                actCategory.setText(adapter.getItem(index) as String, false)
            }
        }

        receiptImageUri = intent.getStringExtra("receipt_image_uri")
    }

    private fun loadTransactionForEdit(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entity = db.transactionDao().getTransactionById(id)
            entity?.let { t ->
                selectedDateMillis = t.dateMillis
                receiptImageUri = t.receiptImageUri

                val catIndex = categories.indexOf(t.category).takeIf { it >= 0 } ?: 0
                val accIndex = accounts.indexOf(t.account).takeIf { it >= 0 } ?: 0
                val typeIndex = types.indexOf(t.type).takeIf { it >= 0 } ?: 0

                withContext(Dispatchers.Main) {
                    etTitle.setText(t.title)
                    etAmount.setText(formatAmountForInput(t.amount))
                    etDate.setText(t.date)
                    etNotes.setText(t.notes)

                    actCategory.setText(categories[catIndex], false)
                    actAccount.setText(accounts[accIndex], false)
                    actType.setText(types[typeIndex], false)

                    supportActionBar?.title = "Edit Transaction"
                    btnSave.text = "Update Transaction"
                }
            }
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            clearErrors()

            val title = etTitle.text?.toString()?.trim().orEmpty()
            val amountText = etAmount.text?.toString()?.trim().orEmpty()
            val category = actCategory.text?.toString()?.trim().orEmpty()
            val account = actAccount.text?.toString()?.trim().orEmpty()
            val type = actType.text?.toString()?.trim().orEmpty()
            val date = etDate.text?.toString()?.trim().orEmpty()
            val notes = etNotes.text?.toString()?.trim().orEmpty()

            var isValid = true

            if (title.isEmpty()) {
                tilTitle.error = "Title wajib diisi"
                isValid = false
            }

            if (amountText.isEmpty()) {
                tilAmount.error = "Amount wajib diisi"
                isValid = false
            }

            val normalizedAmountText = amountText.replace(".", "").replace(",", ".")
            val amount = normalizedAmountText.toDoubleOrNull()
            if (amountText.isNotEmpty() && amount == null) {
                tilAmount.error = "Amount tidak valid"
                isValid = false
            }

            if (amount != null && amount <= 0.0) {
                tilAmount.error = "Amount harus lebih dari 0"
                isValid = false
            }

            if (category.isEmpty()) {
                tilCategory.error = "Category wajib dipilih"
                isValid = false
            }

            if (account.isEmpty()) {
                tilAccount.error = "Account wajib dipilih"
                isValid = false
            }

            if (type.isEmpty()) {
                tilType.error = "Type wajib dipilih"
                isValid = false
            }

            if (selectedDateMillis == null || date.isEmpty()) {
                tilDate.error = "Date wajib diisi"
                isValid = false
            }

            if (!isValid || amount == null) return@setOnClickListener

            val transaction = TransactionEntity(
                id = editTransactionId ?: 0,
                title = title,
                amount = amount,
                category = category,
                account = account,
                type = type,
                date = date,
                dateMillis = selectedDateMillis ?: 0L,
                notes = notes,
                receiptImageUri = receiptImageUri
            )

            lifecycleScope.launch(Dispatchers.IO) {
                if (editTransactionId == null) {
                    db.transactionDao().insertTransaction(transaction)
                } else {
                    db.transactionDao().updateTransaction(transaction)
                }

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@AddTransactionActivity,
                        if (editTransactionId == null) "Transaction saved" else "Transaction updated",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun clearErrors() {
        tilTitle.error = null
        tilAmount.error = null
        tilCategory.error = null
        tilAccount.error = null
        tilType.error = null
        tilDate.error = null
        tilNotes.error = null
    }

    private fun formatDisplayDate(millis: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale("in", "ID")).format(Date(millis))
    }

    private fun formatAmountForInput(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            val nf = NumberFormat.getNumberInstance(Locale.US)
            nf.minimumFractionDigits = 0
            nf.maximumFractionDigits = 2
            nf.format(amount)
        }
    }
}