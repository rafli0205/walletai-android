package com.liam.walletai

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class TransactionAdapter(
    private var items: List<TransactionItem>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var lastAnimatedPosition = -1

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardIconContainer: MaterialCardView =
            itemView.findViewById(R.id.cardIconContainer)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvTransactionSubtitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.tvAmount.text = item.amount

        val amountColor = if (item.isIncome) {
            ContextCompat.getColor(context, R.color.md_theme_secondary)
        } else {
            ContextCompat.getColor(context, R.color.md_theme_error)
        }
        holder.tvAmount.setTextColor(amountColor)

        if (item.isIncome) {
            holder.cardIconContainer.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.md_theme_secondaryContainer)
            )
            holder.ivIcon.setImageResource(R.drawable.ic_notifications_24)
            holder.ivIcon.imageTintList =
                ContextCompat.getColorStateList(context, R.color.md_theme_secondary)
        } else {
            holder.cardIconContainer.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.md_theme_primaryContainer)
            )
            holder.ivIcon.setImageResource(R.drawable.ic_receipt_24)
            holder.ivIcon.imageTintList =
                ContextCompat.getColorStateList(context, R.color.md_theme_primary)
        }

        if (position > lastAnimatedPosition) {
            val anim = AnimationUtils.loadAnimation(context, R.anim.item_fall_in)
            holder.itemView.startAnimation(anim)
            lastAnimatedPosition = position
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                putExtra("transaction_id", item.id)
                putExtra("title", item.title)
                putExtra("amount", item.amount)
                putExtra("category", item.category)
                putExtra("account", item.account)
                putExtra("type", item.type)
                putExtra("date", item.date)
                putExtra("notes", item.notes)
                putExtra("receipt_uri", item.receiptImageUri)
            }
            context.startActivity(intent)

            if (context is Activity) {
                context.overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: TransactionViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<TransactionItem>) {
        items = newItems
        notifyDataSetChanged()
        lastAnimatedPosition = -1
    }
}