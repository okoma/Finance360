package com.i2medier.financialpro.planner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["due_date"]),
        Index(value = ["is_paid"]),
        Index(value = ["category"]),
        Index(value = ["due_date", "is_paid"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    @ColumnInfo(name = "due_date")
    val dueDate: Long,
    @ColumnInfo(name = "is_paid")
    val isPaid: Boolean = false,
    val repeat: String? = null,
    val category: String = "all",
    @ColumnInfo(name = "created_at")
    val createdAt: Long
