package com.liam.walletai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import com.liam.walletai.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvTitle: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvCategoryAccount: TextView
    private lateinit var tvTypeDate: TextView
    private lateinit var tvNotes: TextView
    private lateinit var ivReceipt: ImageView
    private lateinit var cardReceipt: MaterialCardView

    private var transactionId: Int = 0
    private var receiptUri: String? = null

    private val db by lazy { DatabaseProvider.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        toolbar = findViewById(R.id.toolbarTransactionDetail)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvAmount = findViewById(R.id.tvDetailAmount)
        tvCategoryAccount = findViewById(R.id.tvDetailCategoryAccount)
        tvTypeDate = findViewById(R.id.tvDetailTypeDate)
        tvNotes = findViewById(R.id.tvDetailNotes)
        ivReceipt = findViewById(R.id.ivDetailReceipt)
        cardReceipt = findViewById(R.id.cardDetailReceipt)

        transactionId = intent.getIntExtra("transaction_id", 0)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transaction detail"

        toolbar.inflateMenu(R.menu.menu_transaction_detail)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }

        bindFromIntent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }

    private fun bindFromIntent() {
        val title = intent.getStringExtra("title").orEmpty()
        val amount = intent.getStringExtra("amount").orEmpty()
        val category = intent.getStringExtra("category").orEmpty()
        val account = intent.getStringExtra("account").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val date = intent.getStringExtra("date").orEmpty()
        val notes = intent.getStringExtra("notes").orEmpty()
        receiptUri = intent.getStringExtra("receipt_uri")

        tvTitle.text = title
        tvAmount.text = amount
        tvCategoryAccount.text = "$category • $account"
        tvTypeDate.text = "$type • $date"
        tvNotes.text = if (notes.isBlank()) "No notes" else notes

        val amountColorRes = if (type.equals("Income", ignoreCase = true)) {
            R.color.md_theme_secondary
        } else {
            R.color.md_theme_error
        }
        tvAmount.setTextColor(ContextCompat.getColor(this, amountColorRes))

        if (!receiptUri.isNullOrBlank()) {
            cardReceipt.visibility = View.VISIBLE

            try {
                val uri = Uri.parse(receiptUri)

                Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(ivReceipt)

                ivReceipt.setOnClickListener {
                    val intent = Intent(this, ReceiptFullScreenActivity::class.java).apply {
                        putExtra("receipt_uri", receiptUri)
                    }
                    startActivity(intent)
                }
            } catch (e: SecurityException) {
                cardReceipt.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Cannot access receipt image anymore",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                cardReceipt.visibility = View.GONE
            }
        } else {
            cardReceipt.visibility = View.GONE
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        if (transactionId == 0) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteById(transactionId)
            launch(Dispatchers.Main) {
                Toast.makeText(
                    this@TransactionDetailActivity,
                    "Transaction deleted",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
        }
    }
}
