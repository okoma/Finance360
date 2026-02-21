package com.i2medier.financialpro.planner.domain

data class BillCategoryOption(
    val key: String,
    val label: String,
    val emoji: String
)

object BillCategoryUi {
    private val categories = listOf(
        BillCategoryOption("housing", "Housing", "H"),
        BillCategoryOption("utilities", "Utilities", "U"),
        BillCategoryOption("internet_phone", "Internet & Phone", "I"),
        BillCategoryOption("transport", "Transport", "T"),
        BillCategoryOption("credit_loans", "Credit / Loans", "C"),
        BillCategoryOption("subscriptions", "Subscriptions", "S"),
        BillCategoryOption("health", "Health", "H"),
        BillCategoryOption("education", "Education", "E"),
        BillCategoryOption("other", "Other", "O")
    )

    fun options(): List<BillCategoryOption> = categories

    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase().orEmpty()
        if (raw == "all") return "other"
        return categories.firstOrNull { it.key == raw }?.key ?: "other"
    }

    fun emojiFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.emoji ?: "O"
    }

    fun labelFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.label ?: "Other"
    }
}
