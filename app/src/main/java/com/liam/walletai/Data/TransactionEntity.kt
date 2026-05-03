package com.liam.walletai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val amount: Double,

    val category: String,

    val account: String,

    // "Income" atau "Expense"
    val type: String,

    // Teks tanggal buat ditampilin di UI (misal "12 Mar 2026")
    val date: String,

    // Timestamp (millis) dari MaterialDatePicker, dipakai buat filter/range
    val dateMillis: Long,

    val notes: String,

    // URI foto struk (bisa null kalau transaksi tidak lewat scan)
    val receiptImageUri: String? = null
)
