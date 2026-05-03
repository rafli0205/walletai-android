package com.liam.walletai

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liam.walletai.data.BudgetEntity
import java.text.NumberFormat
import java.util.Locale

class BudgetAdapter(
    private val categories: List<String>,
    private val budgets: Map<String, BudgetEntity>,
    private val onBudgetChanged: (category: String, newAmount: Long?) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    private val inputValues = mutableMapOf<String, Long?>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvCurrentBudget: TextView = itemView.findViewById(R.id.tvCurrentBudget)
        val etNewBudget: EditText = itemView.findViewById(R.id.etNewBudget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategory.text = category

        val existing = budgets[category]
        if (existing != null) {
            holder.tvCurrentBudget.text =
                "Budget: ${rupiahFormat.format(existing.amount.toDouble())}"
        } else {
            holder.tvCurrentBudget.text = "Budget: not set"
        }

        holder.etNewBudget.setText("") // biar tiap bind kosong

        holder.etNewBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString()?.trim()
                val value = text?.toLongOrNull()
                inputValues[category] = value
                onBudgetChanged(category, value)
            }
        })
    }
}
