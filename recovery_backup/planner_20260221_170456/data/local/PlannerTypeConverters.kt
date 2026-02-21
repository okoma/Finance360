package com.i2medier.financialpro.planner.data.local

import androidx.room.TypeConverter
class PlannerTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    fun fromAccountType(type: AccountType): String = type.name
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
}
