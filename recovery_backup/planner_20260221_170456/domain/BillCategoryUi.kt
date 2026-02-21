package com.i2medier.financialpro.planner.domain

data class BillCategoryOption(
    val key: String,
    val label: String,
    val emoji: String
)
object BillCategoryUi {
    private val categories = listOf(
        BillCategoryOption("housing", "Housing", "🏠"),
        BillCategoryOption("utilities", "Utilities", "⚡"),
        BillCategoryOption("internet_phone", "Internet & Phone", "📶"),
        BillCategoryOption("transport", "Transport", "🚗"),
        BillCategoryOption("credit_loans", "Credit / Loans", "💳"),
        BillCategoryOption("subscriptions", "Subscriptions", "🔔"),
        BillCategoryOption("health", "Health", "🏥"),
        BillCategoryOption("education", "Education", "📚"),
        BillCategoryOption("other", "Other", "🛒")
    )
    fun options(): List<BillCategoryOption> = categories
    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase().orEmpty()
        if (raw == "all") return "other"
        return categories.firstOrNull { it.key == raw }?.key ?: "other"
    }
    fun emojiFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.emoji ?: "🛒"
    fun labelFor(value: String?): String {
        return categories.firstOrNull { it.key == key }?.label ?: "Other"
}
