package com.liam.walletai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.liam.walletai.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

data class MainUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isEmpty: Boolean = true,
    val topCategoryThisPeriod: String? = null,
    val topCategoryChangeText: String? = null
)

class MainViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // range aktif + offset bulan untuk insight perbandingan
    private var currentRange: Pair<Long, Long> = DateUtils.getTodayRangeMillis()
    private var currentMonthOffset: Int? = null

    fun setDateRange(range: Pair<Long, Long>, monthOffset: Int? = null) {
        currentRange = range
        currentMonthOffset = monthOffset
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val from = currentRange.first
            val to = currentRange.second

            val transactions = repository.getTransactionsInRange(from, to)
            val summary = repository.getSummaryInRange(from, to)

            val income = summary.income
            val expense = summary.expense
            val balance = income - expense

            var topCategoryThisPeriod: String? = null
            var topCategoryChangeText: String? = null

            // Insight perbandingan hanya untuk filter berbasis bulan
            if (currentMonthOffset != null) {
                val thisAgg = repository.getExpenseByCategoryInRange(from, to)

                val compareOffset = currentMonthOffset!! + 1
                val prevRange = DateUtils.getMonthRangeMillis(compareOffset)
                val prevAgg = repository.getExpenseByCategoryInRange(
                    prevRange.first,
                    prevRange.second
                )

                val lastMap = prevAgg.associateBy { it.category }

                val top = thisAgg.maxByOrNull { it.totalExpense }
                if (top != null && top.totalExpense > 0) {
                    topCategoryThisPeriod = top.category

                    val thisVal = top.totalExpense
                    val lastVal = lastMap[top.category]?.totalExpense ?: 0.0

                    if (lastVal > 0.0) {
                        val diff = thisVal - lastVal
                        val pct = (diff / lastVal) * 100.0
                        val rounded = String.format(Locale.US, "%.1f", abs(pct))

                        topCategoryChangeText = when {
                            pct > 5.0 ->
                                "You’re spending $rounded% more on ${top.category} than previous month."
                            pct < -5.0 ->
                                "Good job, you cut ${top.category} spending by $rounded% compared to previous month."
                            else ->
                                "Your ${top.category} spending is about the same as previous month."
                        }
                    } else {
                        topCategoryChangeText =
                            "You started spending on ${top.category} in this period."
                    }
                }
            }

            _uiState.value = MainUiState(
                transactions = transactions,
                totalIncome = income,
                totalExpense = expense,
                balance = balance,
                isEmpty = transactions.isEmpty(),
                topCategoryThisPeriod = topCategoryThisPeriod,
                topCategoryChangeText = topCategoryChangeText
            )
        }
    }

    class Factory(
        private val repository: AppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}