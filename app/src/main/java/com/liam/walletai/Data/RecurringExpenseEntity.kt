package com.liam.walletai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Nama template, misal "Gaji", "Sewa kos", "Netflix"
    val title: String,

    val category: String,
    val accountName: String,

    // Pakai Double supaya konsisten dengan TransactionEntity.amount
    val amount: Double,

    // "Income" atau "Expense"
    val type: String,

    val note: String = "",

    // Tanggal dalam bulan (1..28/30/31)
    val dayOfMonth: Int,

    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
