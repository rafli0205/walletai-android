package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.liam.walletai.data.DatabaseProvider
import com.liam.walletai.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private lateinit var tvTotalBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvInsight: TextView
    private lateinit var tvEmptyState: TextView

    private lateinit var cardAddTransaction: MaterialCardView
    private lateinit var cardScanReceipt: MaterialCardView
    private lateinit var cardEmptyState: MaterialCardView

    private lateinit var recyclerTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var userPrefs: UserPrefs

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    private val selectedCalendar: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPrefs = UserPrefs(this)
        setContentView(R.layout.activity_main)

        bindViews()
        setupToolbar()
        setupGreeting()
        setupRecyclerView()
        setupQuickActions()
        setupBottomNavigation()
        setupMonthNavigator()

        loadDashboard()
    }

    override fun onResume() {
        super.onResume()

        if (shouldOpenLockScreen()) {
            startActivity(Intent(this, LockActivity::class.java))
            return
        }

        loadDashboard()
    }

    override fun onStop() {
        super.onStop()
        if (userPrefs.isLockEnabled()) {
            userPrefs.setSessionUnlocked(false)
        }
    }

    private fun shouldOpenLockScreen(): Boolean {
        return userPrefs.isLockEnabled() && !userPrefs.isSessionUnlocked()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvUserName = findViewById(R.id.tvUserName)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)
        tvInsight = findViewById(R.id.tvInsight)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        cardAddTransaction = findViewById(R.id.cardAddTransaction)
        cardScanReceipt = findViewById(R.id.cardScanReceipt)
        cardEmptyState = findViewById(R.id.cardEmptyState)

        recyclerTransactions = findViewById(R.id.recyclerTransactions)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..10 -> "Good morning"
            in 11..14 -> "Good afternoon"
            in 15..17 -> "Good evening"
            else -> "Good night"
        }
        tvGreeting.text = greeting
        tvUserName.text = "Smart insights for your wallet"
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(emptyList())
        recyclerTransactions.layoutManager = LinearLayoutManager(this)
        recyclerTransactions.adapter = transactionAdapter
    }

    private fun setupMonthNavigator() {
        updateSelectedMonthLabel()
        updateNextMonthButtonState()

        btnPrevMonth.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, -1)
            updateSelectedMonthLabel()
            updateNextMonthButtonState()
            loadDashboard()
        }

        btnNextMonth.setOnClickListener {
            if (canGoToNextMonth()) {
                selectedCalendar.add(Calendar.MONTH, 1)
                updateSelectedMonthLabel()
                updateNextMonthButtonState()
                loadDashboard()
            }
        }
    }

    private fun updateSelectedMonthLabel() {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        tvSelectedMonth.text = formatter.format(selectedCalendar.time)
    }

    private fun canGoToNextMonth(): Boolean {
        val currentMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextMonth = selectedCalendar.clone() as Calendar
        nextMonth.add(Calendar.MONTH, 1)

        return nextMonth.timeInMillis <= currentMonth.timeInMillis
    }

    private fun updateNextMonthButtonState() {
        val enabled = canGoToNextMonth()
        btnNextMonth.isEnabled = enabled
        btnNextMonth.alpha = if (enabled) 1f else 0.35f
    }

    private fun setupQuickActions() {
        cardAddTransaction.setOnClickListener {
            openActivityByName(
                "com.liam.walletai.AddTransactionActivity",
                "Add Transaction page belum tersedia"
            )
        }

        cardScanReceipt.setOnClickListener {
            openActivityByName(
                "com.liam.walletai.ScanReceiptActivity",
                "Scan Receipt page belum tersedia atau belum didaftarkan di AndroidManifest"
            )
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.menu_home

        bottomNavigation.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_home -> true

                R.id.menu_reports -> {
                    openActivityByName(
                        "com.liam.walletai.ReportsActivity",
                        "Reports page belum tersedia"
                    )
                    false
                }

                R.id.menu_settings -> {
                    openActivityByName(
                        "com.liam.walletai.SettingsActivity",
                        "Settings page belum tersedia"
                    )
                    false
                }

                else -> false
            }
        }
    }

    private fun loadDashboard() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.transactionDao()

            val (from, to) = getMonthRangeMillis(
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH)
            )

            val income = dao.getTotalIncomeInRange(from, to)
            val expense = dao.getTotalExpenseInRange(from, to)
            val transactions = dao.getTransactionsInRange(from, to)

            withContext(Dispatchers.Main) {
                renderDashboard(
                    income = income,
                    expense = expense,
                    transactions = transactions
                )
            }
        }
    }

    private fun renderDashboard(
        income: Double,
        expense: Double,
        transactions: List<TransactionEntity>
    ) {
        val balance = income - expense

        tvTotalBalance.text = formatRupiah(balance)
        tvIncome.text = formatRupiah(income)
        tvExpense.text = formatRupiah(expense)

        if (transactions.isEmpty()) {
            recyclerTransactions.visibility = View.GONE
            cardEmptyState.visibility = View.VISIBLE
            transactionAdapter.submitList(emptyList())
            tvEmptyState.text =
                "Your wallet is ready.\nAdd your first transaction or scan a receipt to get insights."
            tvInsight.text = "Your wallet is ready. Start adding transactions to see insights."
        } else {
            recyclerTransactions.visibility = View.VISIBLE
            cardEmptyState.visibility = View.GONE

            val items = transactions.take(5).map { entity ->
                TransactionItem(
                    id = entity.id,
                    title = entity.title,
                    subtitle = "${entity.category} • ${entity.account} • ${entity.date}",
                    amount = formatSignedAmount(entity.amount, entity.type),
                    isIncome = entity.type.equals("Income", ignoreCase = true),
                    date = entity.date,
                    category = entity.category,
                    account = entity.account,
                    type = entity.type,
                    notes = entity.notes,
                    receiptImageUri = entity.receiptImageUri
                )
            }

            transactionAdapter.submitList(items)
            tvInsight.text = buildInsightText(income, expense, balance, transactions.size)
        }
    }

    private fun buildInsightText(
        income: Double,
        expense: Double,
        balance: Double,
        transactionCount: Int
    ): String {
        return when {
            transactionCount == 0 ->
                "Your wallet is ready. Start adding transactions to see insights."

            income == 0.0 && expense > 0.0 ->
                "You are only tracking expenses this month. Try adding income too."

            income > 0.0 && expense == 0.0 ->
                "Nice start. You recorded income but no expenses yet this month."

            balance >= 0 && expense > 0.0 ->
                "Great job. Your balance is still positive this month."

            else ->
                "Careful, your spending is higher than your income this month."
        }
    }

    private fun getMonthRangeMillis(year: Int, monthIndexZeroBased: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()

        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndexZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return start to end
    }

    private fun formatRupiah(amount: Double): String {
        val localeID = Locale("in", "ID")
        return NumberFormat.getCurrencyInstance(localeID).format(amount)
    }

    private fun formatSignedAmount(amount: Double, type: String): String {
        val formatted = formatRupiah(amount)
        return if (type.equals("Income", ignoreCase = true)) {
            "+$formatted"
        } else {
            "-$formatted"
        }
    }

    private fun openActivityByName(className: String, fallbackMessage: String) {
        try {
            val target = Class.forName(className)
            startActivity(Intent(this, target))
        } catch (_: ClassNotFoundException) {
            Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
        }
    }
}