package com.i2medier.financialpro.planner.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["type"]),
        Index(value = ["is_default"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "type")
    val type: AccountType,
    @ColumnInfo(name = "opening_balance")
    val openingBalance: Double = 0.0,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    val currency: String = ""
) {
    companion object {
        const val DEFAULT_CASH_ID = 1L
        const val DEFAULT_SAVINGS_ID = 2L
        const val DEFAULT_INVESTMENTS_ID = 3L
        const val DEFAULT_LOANS_ID = 4L
        const val DEFAULT_CREDIT_CARD_ID = 5L
    }
}
