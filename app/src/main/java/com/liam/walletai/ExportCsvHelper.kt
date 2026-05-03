package com.liam.walletai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.liam.walletai.data.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class ExportCsvHelper(
    private val context: Context,
    private val db: AppDatabase
) {

    fun exportTransactionsCsv(from: Long, to: Long, label: String) {
        val activity = context as? androidx.appcompat.app.AppCompatActivity
        if (activity == null) {
            Toast.makeText(context, "Cannot export from here", Toast.LENGTH_SHORT).show()
            return
        }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = db.transactionDao()
                val transactions = dao.getTransactionsInRange(from, to)

                if (transactions.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "No transactions in selected range", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val file = writeCsvFile(transactions, label)
                shareCsvFile(file)
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun writeCsvFile(list: List<TransactionEntity>, label: String): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()

        val safeLabel = label.replace(":", "-").replace(" ", "_")
        val file = File(dir, "transactions_$safeLabel.csv")
        FileWriter(file).use { writer ->
            writer.appendLine("id,title,amount,category,account,type,date,dateMillis,notes,receiptImageUri")
            list.forEach { t ->
                writer.appendLine(
                    listOf(
                        t.id,
                        escapeCsv(t.title),
                        t.amount,
                        escapeCsv(t.category),
                        escapeCsv(t.account),
                        escapeCsv(t.type),
                        escapeCsv(t.date),
                        t.dateMillis,
                        escapeCsv(t.notes),
                        escapeCsv(t.receiptImageUri)
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    private fun escapeCsv(value: Any?): String {
        val s = value?.toString() ?: ""
        return if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }
    }

    private fun shareCsvFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
