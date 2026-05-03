package com.liam.walletai.ai

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.liam.walletai.R

class AiInsightAdapter(
    private var items: List<AiInsight>,
    private val onItemClick: (AiInsight) -> Unit
) : RecyclerView.Adapter<AiInsightAdapter.AiInsightViewHolder>() {

    fun submitList(newItems: List<AiInsight>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AiInsightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_insight, parent, false)
        return AiInsightViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: AiInsightViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AiInsightViewHolder(
        itemView: View,
        private val onItemClick: (AiInsight) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvInsightTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvInsightDescription)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvInsightBadge)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivInsightIcon)
        private val cardIcon: MaterialCardView = itemView.findViewById(R.id.cardInsightIcon)

        fun bind(item: AiInsight) {
            val ctx = itemView.context

            tvTitle.text = item.title
            tvDescription.text = item.description

            tvBadge.text = when (item.severity) {
                AiInsightSeverity.INFO -> "INFO"
                AiInsightSeverity.WARNING -> "WARNING"
                AiInsightSeverity.CRITICAL -> "CRITICAL"
            }

            val badgeColor = when (item.severity) {
                AiInsightSeverity.INFO -> R.color.md_theme_secondary
                AiInsightSeverity.WARNING -> R.color.md_theme_tertiary
                AiInsightSeverity.CRITICAL -> R.color.md_theme_error
            }

            (tvBadge.background as? GradientDrawable)?.setColor(
                ContextCompat.getColor(ctx, badgeColor)
            )

            val (iconRes, bgColorRes) = when (item.type) {
                AiInsightType.MONTHLY_SPENDING_TREND ->
                    R.drawable.ic_trending_up_24 to R.color.md_theme_secondary

                AiInsightType.TOP_SPENDING_CATEGORY ->
                    R.drawable.ic_category_24 to R.color.md_theme_primary

                AiInsightType.CATEGORY_SPIKE ->
                    R.drawable.ic_warning_24 to R.color.md_theme_error

                AiInsightType.UNUSUAL_LARGE_TRANSACTIONS ->
                    R.drawable.ic_warning_24 to R.color.md_theme_error

                AiInsightType.POTENTIAL_SAVINGS ->
                    R.drawable.ic_savings_24 to R.color.md_theme_tertiary

                AiInsightType.RECURRING_SPENDING ->
                    R.drawable.ic_trending_up_24 to R.color.md_theme_primary
            }

            ivIcon.setImageResource(iconRes)
            ivIcon.imageTintList =
                ContextCompat.getColorStateList(ctx, R.color.md_theme_onPrimary)
            cardIcon.setCardBackgroundColor(ContextCompat.getColor(ctx, bgColorRes))

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}