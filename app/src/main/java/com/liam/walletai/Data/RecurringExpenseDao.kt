package com.liam.walletai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecurringExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecurringExpenseEntity): Long

    @Query("SELECT * FROM recurring_expenses ORDER BY dayOfMonth ASC")
    suspend fun getAll(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY dayOfMonth ASC")
    suspend fun getAllActive(): List<RecurringExpenseEntity>
}
