package com.i2medier.financialpro.planner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Delete
    suspend fun delete(bill: BillEntity)

    @Query("SELECT * FROM bills ORDER BY is_paid ASC, due_date ASC, id DESC")
    suspend fun getAll(): List<BillEntity>

    @Query(
        "SELECT * FROM bills " +
            "WHERE ((:category = 'all') OR category = :category OR category = 'all') " +
            "ORDER BY is_paid ASC, due_date ASC, id DESC"
    )
    fun getByCategoryFlow(category: String): Flow<List<BillEntity>>

    @Query(
        "SELECT COUNT(*) FROM bills " +
            "WHERE is_paid = 0 " +
            "AND due_date >= :startDate AND due_date < :endDate " +
            "AND ((:category = 'all') OR category = :category OR category = 'all')"
    )
    fun getUnpaidCountByCategoryInRangeFlow(
        category: String,
        startDate: Long,
        endDate: Long
    ): Flow<Int>

    @Query(
        "SELECT * FROM bills " +
            "WHERE is_paid = 0 AND due_date < :endDate " +
            "ORDER BY due_date ASC, id ASC"
    )
    suspend fun getUnpaidDueUntil(endDate: Long): List<BillEntity>

    @Query(
        "SELECT * FROM bills " +
            "WHERE is_paid = 0 AND due_date >= :startDate AND due_date < :endDate " +
            "ORDER BY due_date ASC, id ASC"
    )
    suspend fun getUnpaidDueBetween(startDate: Long, endDate: Long): List<BillEntity>
}
