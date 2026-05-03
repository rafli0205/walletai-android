package com.liam.walletai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE dateMillis BETWEEN :from AND :to
        ORDER BY dateMillis DESC
        """
    )
    suspend fun getTransactionsInRange(from: Long, to: Long): List<TransactionEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = 'Income'
        AND dateMillis BETWEEN :from AND :to
        """
    )
    suspend fun getTotalIncomeInRange(from: Long, to: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = 'Expense'
        AND dateMillis BETWEEN :from AND :to
        """
    )
    suspend fun getTotalExpenseInRange(from: Long, to: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = 'Income'
        AND dateMillis BETWEEN :from AND :to
        AND (:category = 'All' OR category = :category)
        AND (:account = 'All' OR account = :account)
        """
    )
    suspend fun getTotalIncomeInRangeFiltered(
        from: Long,
        to: Long,
        category: String,
        account: String
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = 'Expense'
        AND dateMillis BETWEEN :from AND :to
        AND (:category = 'All' OR category = :category)
        AND (:account = 'All' OR account = :account)
        """
    )
    suspend fun getTotalExpenseInRangeFiltered(
        from: Long,
        to: Long,
        category: String,
        account: String
    ): Double

    @Query(
        """
        SELECT category AS category,
               SUM(amount) AS total
        FROM transactions
        WHERE type = 'Expense'
        AND dateMillis BETWEEN :from AND :to
        AND (:account = 'All' OR account = :account)
        GROUP BY category
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    suspend fun getExpenseByCategoryInRange(
        from: Long,
        to: Long,
        account: String
    ): List<CategoryExpenseSummary>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'Expense'
        AND dateMillis BETWEEN :from AND :to
        AND category = :category
        AND (:account = 'All' OR account = :account)
        ORDER BY dateMillis DESC
        """
    )
    suspend fun getExpensesByCategoryAndRange(
        from: Long,
        to: Long,
        category: String,
        account: String
    ): List<TransactionEntity>

    @Query(
        """
        SELECT
            SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END) AS totalIncome,
            SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END) AS totalExpense
        FROM transactions
        WHERE dateMillis BETWEEN :start AND :end
        """
    )
    suspend fun getSummaryForRange(
        start: Long,
        end: Long
    ): SummaryResult?

    data class SummaryResult(
        val totalIncome: Double?,
        val totalExpense: Double?
    )

    @Query(
        """
        SELECT category,
               SUM(amount) AS totalExpense
        FROM transactions
        WHERE type = 'Expense'
        AND dateMillis BETWEEN :start AND :end
        GROUP BY category
        """
    )
    suspend fun getExpenseByCategoryForRange(
        start: Long,
        end: Long
    ): List<CategoryAggregate>

    data class CategoryAggregate(
        val category: String,
        val totalExpense: Double
    )

    @Query(
        """
        SELECT id, title, category, amount, type, dateMillis
        FROM transactions
        WHERE dateMillis BETWEEN :start AND :end
        ORDER BY dateMillis DESC
        """
    )
    suspend fun getTransactionsBriefForRange(
        start: Long,
        end: Long
    ): List<TransactionBriefRow>

    data class TransactionBriefRow(
        val id: Long,
        val title: String?,
        val category: String,
        val amount: Double,
        val type: String,
        val dateMillis: Long
    )

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)
}