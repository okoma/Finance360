package com.i2medier.financialpro.planner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun countByAccount(accountId: Long): Int

    @Query(
        "SELECT * FROM transactions " +
            "WHERE date >= :startDate AND date < :endDate " +
            "AND ((:category = 'all') OR category = :category OR category = 'all') " +
            "ORDER BY date DESC, id DESC"
    )
    fun getByCategoryInRangeFlow(category: String, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = 'SAVING' AND date = :date LIMIT 1")
    suspend fun getSavingForDate(date: Long): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE goal_id = :goalId AND type = 'SAVING'")
    suspend fun getTotalSavingForGoal(goalId: Long): Double?

    @Query("DELETE FROM transactions WHERE goal_id = :goalId")
    suspend fun deleteByGoalId(goalId: Long)

    @Query("UPDATE transactions SET goal_id = :newGoalId WHERE goal_id = :oldGoalId")
    suspend fun reassignGoal(oldGoalId: Long, newGoalId: Long)
}
