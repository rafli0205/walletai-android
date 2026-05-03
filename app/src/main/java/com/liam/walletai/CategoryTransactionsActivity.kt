package com.liam.walletai

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liam.walletai.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class CategoryTransactionsActivity : AppCompatActivity() {

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    private lateinit var toolbar: Toolbar
    private lateinit var tvTitle: TextView
    private lateinit var tvSummaryInfo: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rv: RecyclerView
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_transactions)

        toolbar = findViewById(R.id.toolbarCategoryTx)
        tvTitle = findViewById(R.id.tvTitle)
        tvSummaryInfo = findViewById(R.id.tvSummaryInfo)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rv = findViewById(R.id.rvTransactions)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val category = intent.getStringExtra("category") ?: "Unknown"
        val account = intent.getStringExtra("account") ?: "All"
        val from = intent.getLongExtra("from", 0L)
        val to = intent.getLongExtra("to", Long.MAX_VALUE)
        val label = intent.getStringExtra("label") ?: ""

        supportActionBar?.title = category
        tvTitle.text = label

        rv.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(emptyList())
        rv.adapter = adapter

        loadTransactions(category, account, from, to)
    }

    private fun loadTransactions(category: String, account: String, from: Long, to: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = db.transactionDao()
            val entities = dao.getExpensesByCategoryAndRange(from, to, category, account)

            val totalAmount = entities.sumOf { it.amount }

            val items = entities.map { tx ->
                val formattedDate = DateUtils.formatDateFromMillis(tx.dateMillis)

                TransactionItem(
                    id = tx.id,
                    title = tx.title,
                    subtitle = "${tx.category} • ${tx.account} • $formattedDate",
                    amount = formatRupiah(tx.amount),
                    isIncome = tx.type == "Income",
                    category = tx.category,
                    account = tx.account,
                    type = tx.type,
                    date = formattedDate,
                    notes = tx.notes,
                    receiptImageUri = tx.receiptImageUri
                )
            }

            withContext(Dispatchers.Main) {
                if (items.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                    tvSummaryInfo.text = "Total: ${formatRupiah(0.0)}"
                } else {
                    tvEmptyState.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    tvSummaryInfo.text = "Total in $category: ${formatRupiah(totalAmount)}"
                    adapter.submitList(items)
                }
            }
        }
    }

    private fun formatRupiah(amount: Double): String {
        val localeID = Locale("in", "ID")
        return NumberFormat.getCurrencyInstance(localeID).format(amount)
    }
}
