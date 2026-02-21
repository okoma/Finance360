package com.i2medier.financialpro.planner.domain

data class GoalCategoryOption(
    val key: String,
    val label: String,
    val emoji: String
)
object GoalCategoryUi {
    private val categories = listOf(
        GoalCategoryOption("emergency", "Emergency", "🛟"),
        GoalCategoryOption("home", "Home", "🏠"),
        GoalCategoryOption("tech", "Tech", "💻"),
        GoalCategoryOption("travel", "Travel", "✈️"),
        GoalCategoryOption("education", "Education", "🎓"),
        GoalCategoryOption("health", "Health", "🏥"),
        GoalCategoryOption("business", "Business", "💼"),
        GoalCategoryOption("other", "Other", "⭐")
    )
    fun options(): List<GoalCategoryOption> = categories
    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase().orEmpty()
        return categories.firstOrNull { it.key == raw }?.key ?: "other"
    }
    fun emojiFor(value: String?): String {
        val key = normalize(value)
        return categories.firstOrNull { it.key == key }?.emoji ?: "⭐"
    fun labelFor(value: String?): String {
        return categories.firstOrNull { it.key == key }?.label ?: "Other"
    fun titleWithIcon(value: String?, title: String): String {
        return "${emojiFor(value)} $title"
}
