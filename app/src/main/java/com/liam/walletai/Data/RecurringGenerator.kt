package com.liam.walletai.data

import com.liam.walletai.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurringGenerator {

    suspend fun generateForCurrentMonth(db: AppDatabase) = withContext(Dispatchers.IO) {
        val recurringDao = db.recurringExpenseDao()
        val txDao = db.transactionDao()

        val recurringList: List<RecurringExpenseEntity> = recurringDao.getAllActive()
        if (recurringList.isEmpty()) return@withContext

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        val (from, to) = getMonthRangeMillis(currentYear, currentMonth)
        val existingThisMonth = txDao.getTransactionsInRange(from, to)

        val df = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))

        for (recurring in recurringList) {
            val alreadyExists = existingThisMonth.any { tx ->
                tx.title == recurring.title &&
                        tx.category == recurring.category &&
                        tx.account == recurring.accountName &&
                        tx.amount == recurring.amount &&
                        tx.type == recurring.type
            }
            if (alreadyExists) continue

            val txCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, recurring.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateMillis = txCal.timeInMillis
            val dateText = df.format(txCal.time)

            val newTx = TransactionEntity(
                id = 0, // auto-generate
                title = recurring.title,
                amount = recurring.amount,
                category = recurring.category,
                account = recurring.accountName,
                type = recurring.type,
                date = dateText,
                dateMillis = dateMillis,
                notes = recurring.note,
                receiptImageUri = null
            )

            txDao.insertTransaction(newTx)
        }
    }

    private fun getMonthRangeMillis(year: Int, monthIndexZeroBased: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()

        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndexZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return start to end
    }
}
