package com.liam.walletai.ai

data class AiInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: AiInsightType,
    val severity: AiInsightSeverity,
    val category: String? = null,
    val value: Double? = null,
    val extraData: Map<String, String> = emptyMap()
)

enum class AiInsightType {
    MONTHLY_SPENDING_TREND,
    TOP_SPENDING_CATEGORY,
    CATEGORY_SPIKE,
    UNUSUAL_LARGE_TRANSACTIONS,
    POTENTIAL_SAVINGS,
    RECURRING_SPENDING
}

enum class AiInsightSeverity {
    INFO,
    WARNING,
    CRITICAL
}