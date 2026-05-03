package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.liam.walletai.data.DatabaseProvider
import com.liam.walletai.data.RecurringExpenseDao
import com.liam.walletai.data.RecurringExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecurringListActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: RecurringAdapter

    private lateinit var dao: RecurringExpenseDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_list)

        toolbar = findViewById(R.id.toolbarRecurring)
        recycler = findViewById(R.id.recyclerRecurring)
        fabAdd = findViewById(R.id.fabAddRecurring)

        val db = DatabaseProvider.getDatabase(this)
        dao = db.recurringExpenseDao()

        toolbar.setNavigationOnClickListener { finish() }

        adapter = RecurringAdapter(emptyList()) { entity ->
            toggleActive(entity)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        fabAdd.setOnClickListener { view ->
            view.playClickScaleAnimation {
                startActivity(Intent(this, AddRecurringActivity::class.java))
                overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecurring()
    }

    private fun loadRecurring() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list: List<RecurringExpenseEntity> = dao.getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }

    private fun toggleActive(entity: RecurringExpenseEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = entity.copy(isActive = !entity.isActive)
            dao.insert(updated)
            val list = dao.getAll()
            withContext(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }
    }
}
