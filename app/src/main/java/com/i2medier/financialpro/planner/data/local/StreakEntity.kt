package com.i2medier.financialpro.planner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "last_save_date")
    val lastSaveDate: Long,
    @ColumnInfo(name = "current_streak")
    val currentStreak: Int
)
