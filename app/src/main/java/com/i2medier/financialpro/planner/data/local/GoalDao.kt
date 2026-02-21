package com.i2medier.financialpro.planner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE target_date IS NOT NULL ORDER BY target_date ASC")
    suspend fun getWithTargetDate(): List<GoalEntity>
}
