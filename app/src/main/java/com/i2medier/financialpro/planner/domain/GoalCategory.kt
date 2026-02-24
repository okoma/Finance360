package com.i2medier.financialpro.planner.domain

object GoalCategory {
    const val EMERGENCY = "emergency"
    const val HOME = "home"
    const val TECH = "tech"
    const val TRAVEL = "travel"
    const val EDUCATION = "education"
    const val HEALTH = "health"
    const val BUSINESS = "business"
    const val OTHER = "other"

    data class Option(
        val key: String,
        val label: String,
        val emoji: String
    )

    val options: List<Option> = listOf(
        Option(EMERGENCY, "Emergency", "🛟"),
        Option(HOME, "Home", "🏠"),
        Option(TECH, "Tech", "💻"),
        Option(TRAVEL, "Travel", "✈️"),
        Option(EDUCATION, "Education", "🎓"),
        Option(HEALTH, "Health", "🏥"),
        Option(BUSINESS, "Business", "💼"),
        Option(OTHER, "Other", "⭐")
    )

    fun normalize(key: String?): String {
        val value = key?.trim()?.lowercase().orEmpty()
        return options.firstOrNull { it.key == value }?.key ?: OTHER
    }

    fun optionFor(key: String?): Option {
        val normalized = normalize(key)
        return options.firstOrNull { it.key == normalized } ?: options.last()
    }

    fun titleWithEmoji(title: String, category: String?): String {
        return "${optionFor(category).emoji} $title"
    }
}
