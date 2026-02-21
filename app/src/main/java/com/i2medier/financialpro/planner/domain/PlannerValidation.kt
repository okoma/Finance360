package com.i2medier.financialpro.planner.domain

import com.i2medier.financialpro.planner.data.local.TransactionType

data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

private const val MAX_AMOUNT = 1_000_000_000.0
private const val GOAL_TITLE_MAX_WORDS = 5
private const val GOAL_TITLE_MAX_CHARS = 30
private const val GOAL_NOTE_MAX_WORDS = 15
private const val GOAL_NOTE_MAX_CHARS = 100

fun validateGoalInput(title: String, targetAmount: Double?, description: String?): ValidationResult {
    val normalizedTitle = title.trim()
    val normalizedDescription = description?.trim().orEmpty()
    return when {
        normalizedTitle.isBlank() -> ValidationResult(false, "Goal title required")
        wordCount(normalizedTitle) > GOAL_TITLE_MAX_WORDS -> ValidationResult(false, "Goal title max $GOAL_TITLE_MAX_WORDS words")
        normalizedTitle.length > GOAL_TITLE_MAX_CHARS -> ValidationResult(false, "Goal title max $GOAL_TITLE_MAX_CHARS characters")
        targetAmount == null -> ValidationResult(false, "Invalid target amount")
        targetAmount <= 0.0 -> ValidationResult(false, "Target must be greater than 0")
        targetAmount > MAX_AMOUNT -> ValidationResult(false, "Target amount too large")
        normalizedDescription.isNotEmpty() && wordCount(normalizedDescription) > GOAL_NOTE_MAX_WORDS ->
            ValidationResult(false, "Goal note max $GOAL_NOTE_MAX_WORDS words")
        normalizedDescription.length > GOAL_NOTE_MAX_CHARS ->
            ValidationResult(false, "Goal note max $GOAL_NOTE_MAX_CHARS characters")
        else -> ValidationResult(true)
    }
}

fun validateTransactionInput(
    amount: Double?,
    type: TransactionType,
    note: String?,
    dateMillis: Long,
    nowProvider: () -> Long = { System.currentTimeMillis() }
): ValidationResult {
    return when {
        amount == null -> ValidationResult(false, "Invalid amount")
        amount <= 0.0 -> ValidationResult(false, "Amount must be greater than 0")
        amount > MAX_AMOUNT -> ValidationResult(false, "Amount too large")
        dateMillis > nowProvider() -> ValidationResult(false, "Date cannot be in the future")
        (type == TransactionType.EXPENSE || type == TransactionType.SAVING) && note.isNullOrBlank() ->
            ValidationResult(false, "Note required for ${type.name.lowercase()}")
        else -> ValidationResult(true)
    }
}

private fun wordCount(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim().split(Regex("\\s+")).size
}
