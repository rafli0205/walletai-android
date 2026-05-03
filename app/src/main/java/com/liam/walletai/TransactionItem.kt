package com.liam.walletai

data class TransactionItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isIncome: Boolean,
    val date: String,
    val category: String,
    val account: String,
    val type: String,
    val notes: String,
    val receiptImageUri: String?
)
