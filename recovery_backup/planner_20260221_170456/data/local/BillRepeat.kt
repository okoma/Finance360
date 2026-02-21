package com.i2medier.financialpro.planner.data.local

object BillRepeat {
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    fun normalize(value: String?): String? {
        return when (value?.trim()?.lowercase()) {
            WEEKLY -> WEEKLY
            MONTHLY -> MONTHLY
            else -> null
        }
    }
}
