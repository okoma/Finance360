package com.i2medier.financialpro.planner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: StreakEntity)
    @Query("SELECT * FROM streak WHERE id = 1 LIMIT 1")
    suspend fun get(): StreakEntity?
    fun getFlow(): Flow<StreakEntity?>
}
