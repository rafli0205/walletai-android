package com.liam.walletai

data class HomeUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isEmpty: Boolean = true
)
