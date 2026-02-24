package com.i2medier.financialpro.planner.domain

data class BillCategoryOption(
    val key: String,
    val label: String,
    val emoji: String
)

object BillCategoryUi {
    private val categories = listOf(
        BillCategoryOption("housing", "Housing", "\uD83C\uDFE0"),
        BillCategoryOption("utilities", "Utilities", "\u26A1"),
        BillCategoryOption("internet_phone", "Internet & Phone", "\uD83D\uDCF6"),
        BillCategoryOption("transport", "Transport", "\uD83D\uDE97"),
        BillCategoryOption("credit_loans", "Credit / Loans", "\uD83D\uDCB3"),
        BillCategoryOption("subscriptions", "Subscriptions", "\uD83D\uDD14"),
        BillCategoryOption("health", "Health", "\uD83C\uDFE5"),
        BillCategoryOption("education", "Education", "\uD83D\uDCDA"),
        BillCategoryOption("other", "Other", "\uD83D\uDED2")
    )

    fun options(): List<BillCategoryOption> = categories

    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase().orEmpty()
        if (raw == "all") return "other"
        return categories.firstOrNull { it.key == raw }?.key ?: "other"
    }

    fun emojiFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.emoji ?: "\uD83D\uDED2"
    }

    fun labelFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.label ?: "Other"
    }
}
