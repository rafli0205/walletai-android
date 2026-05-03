package com.liam.walletai.data

import com.liam.walletai.AppDatabase
import com.liam.walletai.DateUtils
import com.liam.walletai.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

data class SummaryResult(
    val income: Double,
    val expense: Double
)

class AppRepository(
    private val db: AppDatabase
) {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    private fun formatRupiah(amount: Double): String =
        rupiahFormat.format(amount)

    suspend fun generateRecurringForCurrentMonth() {
        RecurringGenerator.generateForCurrentMonth(db)
    }

    suspend fun getTransactionsInRange(from: Long, to: Long): List<TransactionItem> =
        withContext(Dispatchers.IO) {
            val data = db.transactionDao().getTransactionsInRange(from, to)
            data.map { tx ->
                val isIncome = tx.type.equals("Income", ignoreCase = true)
                val prefix = if (isIncome) "+ " else "- "
                val amountText = prefix + formatRupiah(tx.amount)

                val formattedDate = DateUtils.formatDateFromMillis(tx.dateMillis)

                TransactionItem(
                    id = tx.id,
                    title = tx.title,
                    subtitle = "${tx.category} • ${tx.account} • $formattedDate",
                    amount = amountText,
                    isIncome = isIncome,
                    date = formattedDate,
                    category = tx.category,
                    account = tx.account,
                    type = tx.type,
                    notes = tx.notes,
                    receiptImageUri = tx.receiptImageUri
                )
            }
        }

    suspend fun getSummaryInRange(from: Long, to: Long): SummaryResult =
        withContext(Dispatchers.IO) {
            val dao = db.transactionDao()
            val income = dao.getTotalIncomeInRange(from, to)
            val expense = dao.getTotalExpenseInRange(from, to)
            SummaryResult(income = income, expense = expense)
        }

    // NEW: total expense per kategori untuk rentang waktu
    suspend fun getExpenseByCategoryInRange(
        from: Long,
        to: Long
    ): List<TransactionDao.CategoryAggregate> =
        withContext(Dispatchers.IO) {
            db.transactionDao().getExpenseByCategoryForRange(from, to)
        }
}
