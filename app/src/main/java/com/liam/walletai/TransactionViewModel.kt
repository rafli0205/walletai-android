package com.liam.walletai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liam.walletai.data.AppRepository
import com.liam.walletai.data.SummaryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    private val _summary = MutableStateFlow<SummaryResult?>(null)
    val summary: StateFlow<SummaryResult?> = _summary.asStateFlow()

    fun loadForRange(from: Long, to: Long) {
        viewModelScope.launch {
            val list = repository.getTransactionsInRange(from, to)
            val sum = repository.getSummaryInRange(from, to)
            _transactions.value = list
            _summary.value = sum
        }
    }

    fun refreshForToday() {
        val (start, end) = DateUtils.getTodayRangeMillis()
        loadForRange(start, end)
    }
}
