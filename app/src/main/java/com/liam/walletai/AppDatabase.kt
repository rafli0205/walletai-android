package com.liam.walletai

import androidx.room.Database
import androidx.room.RoomDatabase
import com.liam.walletai.data.TransactionDao
import com.liam.walletai.data.TransactionEntity
import com.liam.walletai.data.AccountEntity
import com.liam.walletai.data.BudgetEntity
import com.liam.walletai.data.CategoryEntity
import com.liam.walletai.data.RecurringExpenseEntity
import com.liam.walletai.data.RecurringExpenseDao
import com.liam.walletai.data.BudgetDao

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun budgetDao(): BudgetDao
}
