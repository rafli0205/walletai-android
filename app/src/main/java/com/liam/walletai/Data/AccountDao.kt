package com.liam.walletai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AccountEntity>)

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAll(): List<AccountEntity>
}
