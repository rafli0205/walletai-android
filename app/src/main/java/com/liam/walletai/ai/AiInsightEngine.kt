package com.liam.walletai.ai

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class AiInsightEngine {

    private val rupiahFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    fun generateInsights(
        currentMonth: MonthlySummary,
        lastMonth: MonthlySummary?,
        currentCategories: List<CategoryMonthlySummary>,
        lastCategories: List<CategoryMonthlySummary>?,
        currentTransactions: List<TransactionBrief>,
        largeTxThreshold: Double = 1_000_000.0,
        spikePercentThreshold: Double = 30.0
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        generateMonthlyTrendInsight(currentMonth, lastMonth)?.let { insights += it }
        generateTopSpendingCategoryInsight(currentMonth, currentCategories)?.let { insights += it }
        insights += generateCategorySpikeInsights(
            currentMonth = currentMonth,
            currentCategories = currentCategories,
            lastCategories = lastCategories,
            spikePercentThreshold = spikePercentThreshold
        )
        generateRecurringSpendingInsight(currentTransactions)?.let { insights += it }
        generateUnusualLargeTransactionsInsight(
            currentTransactions = currentTransactions,
            largeTxThreshold = largeTxThreshold
        )?.let { insights += it }
        generatePotentialSavingsInsight(
            currentMonth = currentMonth,
            currentCategories = currentCategories,
            lastCategories = lastCategories
        )?.let { insights += it }

        return insights
            .sortedByDescending { insightPriority(it) }
            .distinctBy { it.type to (it.category ?: "") to it.title }
            .take(5)
    }

    private fun insightPriority(insight: AiInsight): Int {
        return when (insight.severity) {
            AiInsightSeverity.CRITICAL -> 300
            AiInsightSeverity.WARNING -> 200
            AiInsightSeverity.INFO -> 100
        } + when (insight.type) {
            AiInsightType.CATEGORY_SPIKE -> 40
            AiInsightType.TOP_SPENDING_CATEGORY -> 30
            AiInsightType.POTENTIAL_SAVINGS -> 20
            AiInsightType.UNUSUAL_LARGE_TRANSACTIONS -> 15
            AiInsightType.MONTHLY_SPENDING_TREND -> 10
            AiInsightType.RECURRING_SPENDING -> 25
        }
    }

    private fun formatPercent(value: Double): Int = value.roundToInt()

    private fun formatRupiah(value: Double): String = rupiahFormat.format(value)

    private fun generateMonthlyTrendInsight(
        current: MonthlySummary,
        last: MonthlySummary?
    ): AiInsight? {
        if (last == null || last.totalExpense <= 0.0) return null

        val diff = current.totalExpense - last.totalExpense
        val percent = diff / last.totalExpense * 100.0

        val title: String
        val description: String
        val severity: AiInsightSeverity

        if (percent >= 15.0) {
            val p = formatPercent(percent)
            title = "Pengeluaran naik $p% dibanding bulan lalu"
            description =
                "Total pengeluaran bulan ini meningkat sekitar $p% dari bulan sebelumnya. Cek kategori yang paling banyak berubah."
            severity = if (p >= 30) AiInsightSeverity.CRITICAL else AiInsightSeverity.WARNING
        } else if (percent <= -10.0) {
            val p = formatPercent(abs(percent))
            title = "Pengeluaran turun $p% dibanding bulan lalu"
            description =
                "Total pengeluaran bulan ini turun sekitar $p%. Pola belanja kamu terlihat lebih efisien dibanding bulan lalu."
            severity = AiInsightSeverity.INFO
        } else {
            title = "Pengeluaran relatif stabil"
            description =
                "Total pengeluaran bulan ini masih berada di kisaran yang mirip dengan bulan sebelumnya."
            severity = AiInsightSeverity.INFO
        }

        return AiInsight(
            id = "MONTHLY_TREND_${current.year}_${current.month}",
            title = title,
            description = description,
            type = AiInsightType.MONTHLY_SPENDING_TREND,
            severity = severity,
            value = percent
        )
    }

    private fun generateTopSpendingCategoryInsight(
        currentMonth: MonthlySummary,
        currentCategories: List<CategoryMonthlySummary>
    ): AiInsight? {
        val expenseCategories = currentCategories.filter { it.totalExpense > 0.0 }
        val top = expenseCategories.maxByOrNull { it.totalExpense } ?: return null
        if (currentMonth.totalExpense <= 0.0) return null

        val contributionPercent = (top.totalExpense / currentMonth.totalExpense) * 100.0
        val amountText = formatRupiah(top.totalExpense)
        val contributionText = formatPercent(contributionPercent)

        val severity = when {
            contributionPercent >= 45.0 -> AiInsightSeverity.CRITICAL
            contributionPercent >= 30.0 -> AiInsightSeverity.WARNING
            else -> AiInsightSeverity.INFO
        }

        return AiInsight(
            id = "TOP_CATEGORY_${top.year}_${top.month}_${top.category}",
            title = "Kategori terbesar: ${top.category}",
            description = "Kategori ${top.category} menyumbang sekitar $contributionText% dari total pengeluaran bulan ini, senilai $amountText.",
            type = AiInsightType.TOP_SPENDING_CATEGORY,
            severity = severity,
            category = top.category,
            value = top.totalExpense
        )
    }

    private fun generateCategorySpikeInsights(
        currentMonth: MonthlySummary,
        currentCategories: List<CategoryMonthlySummary>,
        lastCategories: List<CategoryMonthlySummary>?,
        spikePercentThreshold: Double
    ): List<AiInsight> {
        if (lastCategories.isNullOrEmpty()) return emptyList()
        if (currentMonth.totalExpense <= 0.0) return emptyList()

        val lastMap = lastCategories.associateBy { it.category }

        return currentCategories
            .mapNotNull { current ->
                val last = lastMap[current.category] ?: return@mapNotNull null
                if (last.totalExpense <= 0.0 || current.totalExpense <= 0.0) return@mapNotNull null

                val diff = current.totalExpense - last.totalExpense
                if (diff <= 0.0) return@mapNotNull null

                val percent = diff / last.totalExpense * 100.0
                if (percent < spikePercentThreshold) return@mapNotNull null

                val shareOfExpense = current.totalExpense / currentMonth.totalExpense * 100.0
                val p = formatPercent(percent)
                val diffText = formatRupiah(diff)
                val shareText = formatPercent(shareOfExpense)

                val severity = when {
                    percent >= 60.0 || shareOfExpense >= 35.0 -> AiInsightSeverity.CRITICAL
                    else -> AiInsightSeverity.WARNING
                }

                AiInsight(
                    id = "CATEGORY_SPIKE_${current.year}_${current.month}_${current.category}",
                    title = "Kategori ${current.category} melonjak $p%",
                    description = "Pengeluaran ${current.category} naik sekitar $p% dibanding bulan lalu (+$diffText) dan kini menyumbang sekitar $shareText% dari total pengeluaran bulan ini.",
                    type = AiInsightType.CATEGORY_SPIKE,
                    severity = severity,
                    category = current.category,
                    value = percent
                )
            }
            .sortedByDescending { it.value ?: 0.0 }
            .take(2)
    }

    private fun generateRecurringSpendingInsight(
        currentTransactions: List<TransactionBrief>
    ): AiInsight? {
        val expenseTransactions = currentTransactions
            .filter { it.type.equals("Expense", ignoreCase = true) && it.amount > 0.0 }

        if (expenseTransactions.size < 3) return null

        data class RecurringCandidate(
            val key: String,
            val label: String,
            val category: String,
            val transactions: List<TransactionBrief>
        )

        val grouped = expenseTransactions.groupBy { tx ->
            normalizeRecurringKey(
                title = tx.title,
                category = tx.category
            )
        }

        val candidates = grouped.mapNotNull { (key, transactions) ->
            if (transactions.size < 2) return@mapNotNull null

            val distinctMonths = transactions
                .map { monthKey(it.dateMillis) }
                .distinct()

            if (distinctMonths.size < 2) return@mapNotNull null

            val sorted = transactions.sortedByDescending { it.dateMillis }
            val sample = sorted.first()

            val amounts = transactions.map { it.amount }
            val minAmount = amounts.minOrNull() ?: return@mapNotNull null
            val maxAmount = amounts.maxOrNull() ?: return@mapNotNull null
            val avgAmount = amounts.average()

            val variationPercent = if (avgAmount <= 0.0) {
                0.0
            } else {
                ((maxAmount - minAmount) / avgAmount) * 100.0
            }

            if (variationPercent > 35.0) return@mapNotNull null

            val label = sample.title?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: sample.category

            RecurringCandidate(
                key = key,
                label = label,
                category = sample.category,
                transactions = transactions
            )
        }

        val best = candidates.maxByOrNull { candidate ->
            candidate.transactions.size * 1000 + candidate.transactions.sumOf { it.amount }.toInt()
        } ?: return null

        val monthsCount = best.transactions.map { monthKey(it.dateMillis) }.distinct().size
        val averageAmount = best.transactions.map { it.amount }.average()
        val latestAmount = best.transactions.maxByOrNull { it.dateMillis }?.amount ?: averageAmount

        val severity = when {
            monthsCount >= 4 -> AiInsightSeverity.WARNING
            else -> AiInsightSeverity.INFO
        }

        return AiInsight(
            id = "RECURRING_SPENDING_${best.key}",
            title = "Ada pola pengeluaran rutin di ${best.label}",
            description = "Pengeluaran ${best.label} terdeteksi muncul di $monthsCount bulan berbeda dengan rata-rata sekitar ${formatRupiah(averageAmount)}. Transaksi terbaru tercatat sekitar ${formatRupiah(latestAmount)}.",
            type = AiInsightType.RECURRING_SPENDING,
            severity = severity,
            category = best.category,
            value = averageAmount,
            extraData = mapOf(
                "monthsCount" to monthsCount.toString(),
                "averageAmount" to averageAmount.toLong().toString()
            )
        )
    }

    private fun normalizeRecurringKey(title: String?, category: String): String {
        val raw = title?.trim().orEmpty()
        val cleanedTitle = raw
            .lowercase(Locale.getDefault())
            .replace(Regex("\\d+"), "")
            .replace(Regex("[^a-z ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (cleanedTitle.length >= 4) {
            "$category|$cleanedTitle"
        } else {
            "$category|${category.lowercase(Locale.getDefault())}"
        }
    }

    private fun monthKey(dateMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "$year-$month"
    }

    private fun generateUnusualLargeTransactionsInsight(
        currentTransactions: List<TransactionBrief>,
        largeTxThreshold: Double
    ): AiInsight? {
        val largeExpenses = currentTransactions
            .filter { it.type.equals("Expense", ignoreCase = true) && it.amount >= largeTxThreshold }
            .sortedByDescending { it.amount }

        if (largeExpenses.isEmpty()) return null

        val top3 = largeExpenses.take(3)
        val descriptionList = top3.joinToString("\n") { tx ->
            val title = tx.title?.takeIf { it.isNotBlank() } ?: tx.category
            "- $title: ${formatRupiah(tx.amount)}"
        }

        val severity = if (largeExpenses.size >= 3) {
            AiInsightSeverity.CRITICAL
        } else {
            AiInsightSeverity.WARNING
        }

        return AiInsight(
            id = "UNUSUAL_LARGE_TX_${currentTransactions.firstOrNull()?.dateMillis ?: 0L}",
            title = "Ada ${largeExpenses.size} transaksi besar bulan ini",
            description = "Beberapa transaksi pengeluaran besar terdeteksi:\n$descriptionList",
            type = AiInsightType.UNUSUAL_LARGE_TRANSACTIONS,
            severity = severity,
            value = largeExpenses.first().amount,
            extraData = mapOf(
                "count" to largeExpenses.size.toString(),
                "threshold" to largeTxThreshold.toLong().toString()
            )
        )
    }

    private fun generatePotentialSavingsInsight(
        currentMonth: MonthlySummary,
        currentCategories: List<CategoryMonthlySummary>,
        lastCategories: List<CategoryMonthlySummary>?
    ): AiInsight? {
        if (lastCategories.isNullOrEmpty()) return null
        if (currentMonth.totalExpense <= 0.0) return null

        val lastMap = lastCategories.associateBy { it.category }

        val bestOpportunity = currentCategories
            .mapNotNull { current ->
                val last = lastMap[current.category] ?: return@mapNotNull null
                val diff = current.totalExpense - last.totalExpense
                if (diff <= 0.0) return@mapNotNull null
                current to diff
            }
            .maxByOrNull { it.second }
            ?: return null

        val category = bestOpportunity.first.category
        val diff = bestOpportunity.second
        val reducedBy10Percent = bestOpportunity.first.totalExpense * 0.1
        val savingsTarget = minOf(diff, reducedBy10Percent)

        if (savingsTarget <= 0.0) return null

        val savingsText = formatRupiah(savingsTarget)
        val diffText = formatRupiah(diff)

        return AiInsight(
            id = "POTENTIAL_SAVINGS_${currentMonth.year}_${currentMonth.month}_$category",
            title = "Peluang hemat terbesar ada di $category",
            description = "Kategori $category naik sekitar $diffText dari bulan lalu. Jika ditekan sedikit saja bulan ini, potensi hemat realistisnya sekitar $savingsText.",
            type = AiInsightType.POTENTIAL_SAVINGS,
            severity = AiInsightSeverity.INFO,
            category = category,
            value = savingsTarget
        )
    }
}