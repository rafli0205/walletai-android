package com.liam.walletai.ai

data class TransactionBrief(
    val id: Long,
    val title: String?,
    val category: String,
    val amount: Double,
    val type: String,
    val dateMillis: Long
)