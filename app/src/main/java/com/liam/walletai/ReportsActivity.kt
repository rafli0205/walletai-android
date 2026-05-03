package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.liam.walletai.ai.AiInsight
import com.liam.walletai.ai.AiInsightAdapter
import com.liam.walletai.ai.AiInsightEngine
import com.liam.walletai.ai.AiInsightType
import com.liam.walletai.ai.CategoryMonthlySummary
import com.liam.walletai.ai.MonthlySummary
import com.liam.walletai.ai.TransactionBrief
import com.liam.walletai.data.CategoryExpenseSummary
import com.liam.walletai.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

class ReportsActivity : AppCompatActivity() {

    private lateinit var toolbarReports: MaterialToolbar
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var tvSelectedMonth: TextView

    private lateinit var spCategory: AutoCompleteTextView
    private lateinit var spAccount: AutoCompleteTextView

    private lateinit var tvReportSummaryTitle: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvBudgetInfo: TextView
    private lateinit var tvInsight: TextView
    private lateinit var tvAiInsightsTitle: TextView
    private lateinit var tvChartTitle: TextView
    private lateinit var tvPieTitle: TextView
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var tvExportReport: MaterialButton

    private lateinit var rvAiInsights: RecyclerView
    private lateinit var aiInsightAdapter: AiInsightAdapter
    private val aiInsightEngine = AiInsightEngine()

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    private val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val categories = listOf(
        "All", "Food", "Transport", "Shopping", "Bills", "Salary", "Other"
    )

    private val accounts = listOf(
        "All", "Cash", "BCA", "Mandiri", "GoPay", "OVO", "Dana"
    )

    private var selectedMonthOffset = 0

    private var lastIncome: Double = 0.0
    private var lastExpense: Double = 0.0
    private var lastFrom: Long = 0L
    private var lastTo: Long = 0L
    private var lastCategory: String = "All"
    private var lastAccount: String = "All"
    private var lastYear: Int = 0
    private var lastMonthIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        toolbarReports = findViewById(R.id.toolbarReports)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)

        spCategory = findViewById(R.id.spCategory)
        spAccount = findViewById(R.id.spAccount)

        tvReportSummaryTitle = findViewById(R.id.tvReportSummaryTitle)
        tvIncome = findViewById(R.id.tvTotalIncome)
        tvExpense = findViewById(R.id.tvTotalExpense)
        tvBalance = findViewById(R.id.tvBalance)
        tvBudgetInfo = findViewById(R.id.tvBudgetInfo)
        tvInsight = findViewById(R.id.tvInsight)
        tvAiInsightsTitle = findViewById(R.id.tvAiInsightsTitle)
        tvChartTitle = findViewById(R.id.tvChartTitle)
        tvPieTitle = findViewById(R.id.tvPieTitle)
        barChart = findViewById(R.id.barChartMonthly)
        pieChart = findViewById(R.id.pieChartCategories)
        tvExportReport = findViewById(R.id.tvExportReport)

        rvAiInsights = findViewById(R.id.rvAiInsights)
        aiInsightAdapter = AiInsightAdapter(emptyList()) { insight ->
            handleInsightClick(insight)
        }
        rvAiInsights.layoutManager = LinearLayoutManager(this)
        rvAiInsights.adapter = aiInsightAdapter

        setupToolbar()
        setupDropdowns()
        setupMonthNavigator()
        setupBarChart()
        setupPieChart()
        loadSelectedMonthReport()

        tvExportReport.setOnClickListener {
            exportCurrentReport()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbarReports)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val typeface = ResourcesCompat.getFont(this, R.font.poppins_semibold)
        for (i in 0 until toolbarReports.childCount) {
            val child = toolbarReports.getChildAt(i)
            if (child is TextView && child.text == toolbarReports.title) {
                child.typeface = typeface
                child.textSize = 20f
                break
            }
        }

        toolbarReports.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupDropdowns() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        spCategory.setAdapter(categoryAdapter)
        spCategory.setText(categories.first(), false)

        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts)
        spAccount.setAdapter(accountAdapter)
        spAccount.setText(accounts.first(), false)

        spCategory.setOnItemClickListener { _, _, _, _ ->
            loadSelectedMonthReport()
        }

        spAccount.setOnItemClickListener { _, _, _, _ ->
            loadSelectedMonthReport()
        }
    }

    private fun setupMonthNavigator() {
        updateMonthNavigatorUi()

        btnPrevMonth.setOnClickListener {
            if (selectedMonthOffset < 12) {
                selectedMonthOffset++
                updateMonthNavigatorUi()
                loadSelectedMonthReport()
            }
        }

        btnNextMonth.setOnClickListener {
            if (selectedMonthOffset > 0) {
                selectedMonthOffset--
                updateMonthNavigatorUi()
                loadSelectedMonthReport()
            }
        }
    }

    private fun updateMonthNavigatorUi() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -selectedMonthOffset)

        val sdf = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
        val monthLabel = sdf.format(cal.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("in", "ID")) else it.toString()
        }

        tvSelectedMonth.text = monthLabel
        btnNextMonth.isEnabled = selectedMonthOffset > 0
        btnNextMonth.alpha = if (selectedMonthOffset > 0) 1f else 0.35f

        btnPrevMonth.isEnabled = selectedMonthOffset < 12
        btnPrevMonth.alpha = if (selectedMonthOffset < 12) 1f else 0.35f
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)
        barChart.setScaleEnabled(false)
        barChart.axisRight.isEnabled = false

        barChart.legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        }

        barChart.axisLeft.apply {
            granularity = 1f
            textColor = getColorCompat(R.color.md_theme_onSurfaceVariant)
            gridColor = getColorCompat(R.color.outline)
        }

        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            textColor = getColorCompat(R.color.md_theme_onSurfaceVariant)
            setDrawGridLines(false)
        }
    }

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.isRotationEnabled = true
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 56f
        pieChart.setHoleColor(android.graphics.Color.TRANSPARENT)
        pieChart.transparentCircleRadius = 60f
        pieChart.setTransparentCircleColor(android.graphics.Color.TRANSPARENT)

        pieChart.legend.apply {
            isEnabled = true
            textColor = getColorCompat(R.color.md_theme_onSurfaceVariant)
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            isWordWrapEnabled = true
        }

        pieChart.setCenterTextSize(14f)
        pieChart.setCenterTextColor(getColorCompat(R.color.md_theme_onSurface))

        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val pieEntry = e as? PieEntry ?: return
                val label = pieEntry.label ?: return
                openCategoryTransactions(label)
            }

            override fun onNothingSelected() = Unit
        })
    }

    private data class RangeResult(
        val currentStart: Long,
        val currentEnd: Long,
        val lastStart: Long,
        val lastEnd: Long,
        val currentYear: Int,
        val currentMonth: Int,
        val lastYear: Int,
        val lastMonth: Int
    )

    private fun calculateRangesFromUi(): RangeResult {
        val currentCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -selectedMonthOffset)
        }

        val currentYear = currentCal.get(Calendar.YEAR)
        val currentMonthZeroBased = currentCal.get(Calendar.MONTH)
        val currentMonthOneBased = currentMonthZeroBased + 1
        val (currentStart, currentEnd) = getMonthRangeMillis(currentYear, currentMonthZeroBased)

        val lastCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -selectedMonthOffset)
            add(Calendar.MONTH, -1)
        }

        val lastYear = lastCal.get(Calendar.YEAR)
        val lastMonthZeroBased = lastCal.get(Calendar.MONTH)
        val lastMonthOneBased = lastMonthZeroBased + 1
        val (lastStart, lastEnd) = getMonthRangeMillis(lastYear, lastMonthZeroBased)

        return RangeResult(
            currentStart = currentStart,
            currentEnd = currentEnd,
            lastStart = lastStart,
            lastEnd = lastEnd,
            currentYear = currentYear,
            currentMonth = currentMonthOneBased,
            lastYear = lastYear,
            lastMonth = lastMonthOneBased
        )
    }

    private fun loadSelectedMonthReport() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -selectedMonthOffset)

        val monthIndex = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val (from, to) = getMonthRangeMillis(year, monthIndex)

        val selectedCategory = spCategory.text?.toString()?.ifBlank { "All" } ?: "All"
        val selectedAccount = spAccount.text?.toString()?.ifBlank { "All" } ?: "All"

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.transactionDao()
            val budgetDao = db.budgetDao()

            val income = dao.getTotalIncomeInRangeFiltered(from, to, selectedCategory, selectedAccount)
            val expense = dao.getTotalExpenseInRangeFiltered(from, to, selectedCategory, selectedAccount)
            val balance = income - expense

            val byCategory = dao.getExpenseByCategoryInRange(from, to, selectedAccount)

            val budgetForCategory = if (selectedCategory == "All") {
                null
            } else {
                budgetDao.getByCategory(selectedCategory)
            }

            lastIncome = income
            lastExpense = expense
            lastFrom = from
            lastTo = to
            lastCategory = selectedCategory
            lastAccount = selectedAccount
            lastYear = year
            lastMonthIndex = monthIndex

            val rangeResult = calculateRangesFromUi()

            val currentSummaryResult = dao.getSummaryForRange(
                rangeResult.currentStart,
                rangeResult.currentEnd
            )

            val lastSummaryResult = dao.getSummaryForRange(
                rangeResult.lastStart,
                rangeResult.lastEnd
            )

            val currentSummary = MonthlySummary(
                year = rangeResult.currentYear,
                month = rangeResult.currentMonth,
                totalIncome = currentSummaryResult?.totalIncome ?: 0.0,
                totalExpense = (currentSummaryResult?.totalExpense ?: 0.0).absoluteValue
            )

            val lastSummary = lastSummaryResult?.let {
                MonthlySummary(
                    year = rangeResult.lastYear,
                    month = rangeResult.lastMonth,
                    totalIncome = it.totalIncome ?: 0.0,
                    totalExpense = (it.totalExpense ?: 0.0).absoluteValue
                )
            }

            val currentCatAgg = dao.getExpenseByCategoryForRange(
                rangeResult.currentStart,
                rangeResult.currentEnd
            )

            val lastCatAgg = dao.getExpenseByCategoryForRange(
                rangeResult.lastStart,
                rangeResult.lastEnd
            )

            val currentCategories = currentCatAgg.map {
                CategoryMonthlySummary(
                    year = rangeResult.currentYear,
                    month = rangeResult.currentMonth,
                    category = it.category,
                    totalExpense = it.totalExpense
                )
            }

            val lastCategories = if (lastCatAgg.isNotEmpty()) {
                lastCatAgg.map {
                    CategoryMonthlySummary(
                        year = rangeResult.lastYear,
                        month = rangeResult.lastMonth,
                        category = it.category,
                        totalExpense = it.totalExpense
                    )
                }
            } else {
                null
            }

            val currentTxRows = dao.getTransactionsBriefForRange(
                rangeResult.currentStart,
                rangeResult.currentEnd
            )

            val currentTransactions = currentTxRows.map {
                TransactionBrief(
                    id = it.id,
                    title = it.title,
                    category = it.category,
                    amount = it.amount,
                    type = it.type,
                    dateMillis = it.dateMillis
                )
            }

            val insights: List<AiInsight> = aiInsightEngine.generateInsights(
                currentMonth = currentSummary,
                lastMonth = lastSummary,
                currentCategories = currentCategories,
                lastCategories = lastCategories,
                currentTransactions = currentTransactions,
                largeTxThreshold = 1_000_000.0,
                spikePercentThreshold = 30.0
            )

            withContext(Dispatchers.Main) {
                val monthName = monthNames[monthIndex]
                val categoryLabel = if (selectedCategory == "All") "All Categories" else selectedCategory
                val accountLabel = if (selectedAccount == "All") "All Accounts" else selectedAccount

                tvReportSummaryTitle.text = "Summary – $monthName $year"
                tvChartTitle.text = "Income vs Expense – $monthName $year\n$categoryLabel • $accountLabel"
                tvPieTitle.text = "Expenses by Category – $monthName $year\n$accountLabel"

                tvIncome.text = formatRupiah(income)
                tvExpense.text = formatRupiah(expense)
                tvBalance.text = formatRupiah(balance)

                if (selectedCategory == "All" || budgetForCategory == null) {
                    tvBudgetInfo.text = "Budget is not set for this category yet."
                } else {
                    val budgetAmount = budgetForCategory.amount.toDouble()
                    val diff = budgetAmount - expense
                    val status = when {
                        expense == 0.0 -> "No expense recorded for this category yet."
                        diff > 0 -> "You are under budget by ${formatRupiah(diff)}."
                        diff == 0.0 -> "You have exactly reached the budget."
                        else -> "You are over budget by ${formatRupiah(-diff)}."
                    }
                    tvBudgetInfo.text =
                        "Budget for $selectedCategory: ${formatRupiah(budgetAmount)}\n$status"
                }

                tvInsight.text = when {
                    income == 0.0 && expense == 0.0 ->
                        "No transactions in this period yet."
                    income >= expense && expense > 0.0 ->
                        "Nice, your income is still higher than your spending this month."
                    income > 0.0 && expense == 0.0 ->
                        "You only recorded income in this period."
                    income == 0.0 && expense > 0.0 ->
                        "Only expenses were recorded in this period."
                    else ->
                        "Your spending is higher than your income in this period."
                }

                applyBarChartData(income.toFloat(), expense.toFloat())
                applyPieChartData(byCategory)
                updateAiInsightsSection(insights)
            }
        }
    }

    private fun updateAiInsightsSection(insights: List<AiInsight>) {
        if (insights.isEmpty()) {
            tvAiInsightsTitle.visibility = View.GONE
            rvAiInsights.visibility = View.GONE

            if (tvInsight.text.isNullOrBlank() || tvInsight.text == "Insight text will appear here.") {
                tvInsight.text = "Belum ada pola khusus yang bisa disorot untuk periode ini."
            }
            return
        }

        tvAiInsightsTitle.visibility = View.VISIBLE
        rvAiInsights.visibility = View.VISIBLE
        aiInsightAdapter.submitList(insights)
    }

    private fun applyBarChartData(income: Float, expense: Float) {
        val entries = listOf(
            BarEntry(0f, income),
            BarEntry(1f, expense)
        )

        val labels = listOf("Income", "Expense")

        val dataSet = BarDataSet(entries, "Monthly Summary").apply {
            colors = listOf(
                getColorCompat(R.color.md_theme_secondary),
                getColorCompat(R.color.md_theme_error)
            )
            valueTextColor = getColorCompat(R.color.md_theme_onBackground)
            valueTextSize = 12f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        barChart.data = barData
        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            axisMinimum = -0.5f
            axisMaximum = labels.size - 0.5f
            granularity = 1f
        }
        barChart.invalidate()
        barChart.animateY(700, Easing.EaseInOutQuad)
    }

    private fun applyPieChartData(data: List<CategoryExpenseSummary>) {
        if (data.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No expense data"
            return
        }

        val entries = data.map { item ->
            PieEntry(item.total.toFloat(), item.category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                getColorCompat(R.color.md_theme_primary),
                getColorCompat(R.color.md_theme_secondary),
                getColorCompat(R.color.md_theme_tertiary),
                getColorCompat(R.color.md_theme_error),
                getColorCompat(R.color.md_theme_primary),
                getColorCompat(R.color.md_theme_secondary)
            )
            valueTextSize = 12f
            valueTextColor = getColorCompat(R.color.md_theme_onPrimary)
            sliceSpace = 3f
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }

        pieChart.data = pieData
        pieChart.centerText = ""
        pieChart.invalidate()
        pieChart.animateY(800, Easing.EaseInOutQuad)
    }

    private fun openCategoryTransactions(category: String) {
        val selectedAccount = spAccount.text?.toString()?.ifBlank { "All" } ?: "All"

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -selectedMonthOffset)
        val monthIndex = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val (from, to) = getMonthRangeMillis(year, monthIndex)

        val monthName = monthNames[monthIndex]
        val accountLabel = if (selectedAccount == "All") "All Accounts" else selectedAccount
        val label = "$monthName $year • $accountLabel"

        val intent = Intent(this, CategoryTransactionsActivity::class.java).apply {
            putExtra("category", category)
            putExtra("account", selectedAccount)
            putExtra("from", from)
            putExtra("to", to)
            putExtra("label", label)
        }
        startActivity(intent)
    }

    private fun handleInsightClick(insight: AiInsight) {
        when (insight.type) {
            AiInsightType.TOP_SPENDING_CATEGORY,
            AiInsightType.CATEGORY_SPIKE,
            AiInsightType.RECURRING_SPENDING -> {
                val category = insight.category ?: return
                openCategoryTransactions(category)
            }

            AiInsightType.UNUSUAL_LARGE_TRANSACTIONS,
            AiInsightType.MONTHLY_SPENDING_TREND,
            AiInsightType.POTENTIAL_SAVINGS -> {
                // v1: belum ada action khusus
            }
        }
    }

    private fun exportCurrentReport() {
        val monthName = monthNames.getOrNull(lastMonthIndex) ?: "-"
        val categoryLabel = if (lastCategory == "All") "All Categories" else lastCategory
        val accountLabel = if (lastAccount == "All") "All Accounts" else lastAccount
        val balance = lastIncome - lastExpense

        val sb = StringBuilder()
        sb.appendLine("Report: $monthName $lastYear")
        sb.appendLine("Filter: $categoryLabel • $accountLabel")
        sb.appendLine()
        sb.appendLine("Income: ${formatRupiah(lastIncome)}")
        sb.appendLine("Expense: ${formatRupiah(lastExpense)}")
        sb.appendLine("Balance: ${formatRupiah(balance)}")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share report"))
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

    private fun getColorCompat(resId: Int): Int =
        ContextCompat.getColor(this, resId)
}