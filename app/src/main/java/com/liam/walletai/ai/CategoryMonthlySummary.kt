package com.liam.walletai.ai

data class CategoryMonthlySummary(
    val year: Int,
    val month: Int,
    val category: String,
    val totalExpense: Double
)