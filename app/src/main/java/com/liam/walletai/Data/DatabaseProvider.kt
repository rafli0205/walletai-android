package com.liam.walletai.data

import android.content.Context
import androidx.room.Room
import com.liam.walletai.AppDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "wallet_ai_database"
            )
                // true = drop semua tabel saat migrasi nggak ketemu (aman untuk dev)
                .fallbackToDestructiveMigration(true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
