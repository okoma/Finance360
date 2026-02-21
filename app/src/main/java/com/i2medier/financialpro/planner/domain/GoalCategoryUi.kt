package com.i2medier.financialpro.planner.domain

data class GoalCategoryOption(
    val key: String,
    val label: String,
    val emoji: String
)

object GoalCategoryUi {
    private val categories = listOf(
        GoalCategoryOption("emergency", "Emergency", "E"),
        GoalCategoryOption("home", "Home", "H"),
        GoalCategoryOption("tech", "Tech", "T"),
        GoalCategoryOption("travel", "Travel", "T"),
        GoalCategoryOption("education", "Education", "E"),
        GoalCategoryOption("health", "Health", "H"),
        GoalCategoryOption("business", "Business", "B"),
        GoalCategoryOption("other", "Other", "O")
    )

    fun options(): List<GoalCategoryOption> = categories

    fun normalize(value: String?): String {
        val raw = value?.trim()?.lowercase().orEmpty()
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

    fun titleWithIcon(value: String?, title: String): String {
        return "${emojiFor(value)} $title"
    }
}
