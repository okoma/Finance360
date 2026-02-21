package com.i2medier.financialpro.planner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        ),
            entity = BillEntity::class,
            childColumns = ["bill_id"],
            entity = AccountEntity::class,
            childColumns = ["account_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["goal_id"]),
        Index(value = ["bill_id"]),
        Index(value = ["category"]),
        Index(value = ["account_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    @ColumnInfo(name = "type")
    val type: TransactionType,
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,
    @ColumnInfo(name = "bill_id")
    val billId: Long? = null,
    @ColumnInfo(name = "account_id")
    val accountId: Long = AccountEntity.DEFAULT_CASH_ID,
    val note: String? = null,
    @ColumnInfo(name = "category")
    val category: String = "all"
