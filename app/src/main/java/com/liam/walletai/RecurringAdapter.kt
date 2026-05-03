package com.liam.walletai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liam.walletai.data.RecurringExpenseEntity
import java.text.NumberFormat
import java.util.Locale

class RecurringAdapter(
    private var items: List<RecurringExpenseEntity>,
    private val onItemClick: (RecurringExpenseEntity) -> Unit
) : RecyclerView.Adapter<RecurringAdapter.ViewHolder>() {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

    fun submitList(newItems: List<RecurringExpenseEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val tvDayAndStatus: TextView = itemView.findViewById(R.id.tvDayAndStatus)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = "${item.type} • ${item.category} • ${item.accountName}"

        val status = if (item.isActive) "Active" else "Inactive"
        holder.tvDayAndStatus.text = "Every ${item.dayOfMonth}th • $status"

        holder.tvAmount.text = rupiahFormat.format(item.amount)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}
