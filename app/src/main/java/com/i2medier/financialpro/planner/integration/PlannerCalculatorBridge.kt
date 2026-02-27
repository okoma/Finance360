package com.i2medier.financialpro.planner.integration

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.AccountEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.repository.PlannerRepository
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.ui.CalculatorRegistry
import com.i2medier.financialpro.util.AnalyticsTracker
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object PlannerCalculatorBridge {
    private data class CategoryOption(
        val key: String,
        val label: String,
        val emoji: String
    )

    fun openPlannerWithResult(
        context: Context,
        amount: Double,
        type: TransactionType,
        title: String,
        note: String
    ) {
        AnalyticsTracker.logCalculatorAddToPlanner(
            context,
            context.javaClass.simpleName,
            "tap"
        )
        if (amount <= 0.0) {
            AnalyticsTracker.logCalculatorAddToPlanner(
                context,
                context.javaClass.simpleName,
                "invalid_amount"
            )
            Toast.makeText(context, "No valid amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedNote = buildPlannerNote(title = title, note = note)

        val activity = context as? FragmentActivity
        if (activity != null) {
            val request = PlannerAddRequest(
                amount = amount,
                suggestedType = type,
                note = normalizedNote,
                title = title,
                calculatorCategory = CalculatorRegistry.primaryCategoryForActivity(context.javaClass)
            )
            val openedInline = runCatching {
                showInlineAddSheet(
                    activity = activity,
                    request = request
                )
            }.isSuccess
            if (openedInline) return
        }

        val request = PlannerAddRequest(
            amount = amount,
            suggestedType = type,
            note = normalizedNote,
            title = title,
            calculatorCategory = CalculatorRegistry.primaryCategoryForActivity(context.javaClass)
        )

        val intent = PlannerIntegrationContract.createOpenPlannerIntent(context, request)
        AnalyticsTracker.logCalculatorAddToPlanner(
            context,
            context.javaClass.simpleName,
            "redirect_to_planner"
        )
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun showInlineAddSheet(
        activity: FragmentActivity,
        request: PlannerAddRequest
    ) {
        val dialog = BottomSheetDialog(activity, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_add_transaction, null, false)
        dialog.setContentView(content)

        val btnClose = content.findViewById<ImageView>(R.id.btnClose)
        val toggleType = content.findViewById<MaterialButtonToggleGroup>(R.id.toggleTransactionType)
        val btnExpense = content.findViewById<MaterialButton>(R.id.btnExpense)
        val btnIncome = content.findViewById<MaterialButton>(R.id.btnIncome)
        val btnSavings = content.findViewById<MaterialButton>(R.id.btnSavings)
        val amountInput = content.findViewById<TextInputEditText>(R.id.etAmount)
        val amountLayout = content.findViewById<TextInputLayout>(R.id.tilAmount)
        val noteInput = content.findViewById<TextInputEditText>(R.id.etNote)
        val dateInput = content.findViewById<TextInputEditText>(R.id.etDate)
        val categoryCard = content.findViewById<android.view.View>(R.id.cardCategory)
        val tvCategoryEmoji = content.findViewById<TextView>(R.id.tvCategoryEmoji)
        val tvCategoryName = content.findViewById<TextView>(R.id.tvCategoryName)
        val btnAdd = content.findViewById<MaterialButton>(R.id.btnAddTransaction)

        var selectedType = request.suggestedType
        var selectedDateMillis = System.currentTimeMillis()
        amountLayout.hint = "${CurrencyManager.getCurrencySymbol(activity)}0"
        amountInput.setText(formatAmountForInput(request.amount))
        noteInput.setText(request.note)
        dateInput.setText(formatDate(selectedDateMillis))

        fun setSelectedType(type: TransactionType) {
            selectedType = type
            val checkedId = when (type) {
                TransactionType.EXPENSE -> btnExpense.id
                TransactionType.INCOME -> btnIncome.id
                TransactionType.SAVING -> btnSavings.id
            }
            if (toggleType.checkedButtonId != checkedId) {
                toggleType.check(checkedId)
            }
            updateActionButton(btnAdd, type)
        }

        var selectedCategory = categoryOptionsFor(selectedType)
            .firstOrNull { it.key == request.calculatorCategory }
            ?: categoryOptionsFor(selectedType).first()

        fun updateSelectedCategoryUi() {
            tvCategoryEmoji.text = selectedCategory.emoji
            tvCategoryName.text = selectedCategory.label
        }

        setSelectedType(selectedType)
        updateSelectedCategoryUi()

        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val nextType = when (checkedId) {
                btnIncome.id -> TransactionType.INCOME
                btnSavings.id -> TransactionType.SAVING
                else -> TransactionType.EXPENSE
            }
            setSelectedType(nextType)
            selectedCategory = categoryOptionsFor(selectedType).first()
            updateSelectedCategoryUi()
        }

        categoryCard.setOnClickListener {
            val options = categoryOptionsFor(selectedType)
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.planner_hint_category))
                .setItems(labels) { _, which ->
                    selectedCategory = options[which]
                    updateSelectedCategoryUi()
                }
                .show()
        }

        val openDatePicker = {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedDateMillis
            }
            DatePickerDialog(
                activity,
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedDateMillis = picked.timeInMillis
                    dateInput.setText(formatDate(selectedDateMillis))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        dateInput.setOnClickListener { openDatePicker() }
        btnClose.setOnClickListener { dialog.dismiss() }

        btnAdd.setOnClickListener {
            val amountValue = parseAmount(amountInput.text?.toString().orEmpty())
            val note = noteInput.text?.toString()?.trim().orEmpty().ifBlank { null }
            if (amountValue == null || amountValue <= 0.0) {
                Toast.makeText(activity, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if ((selectedType == TransactionType.EXPENSE || selectedType == TransactionType.SAVING) && note.isNullOrBlank()) {
                Toast.makeText(activity, "Note required for ${selectedType.name.lowercase()}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDateMillis > System.currentTimeMillis()) {
                Toast.makeText(activity, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val app = activity.application
            val database = PlannerDatabase.getInstance(app)
            val repository = PlannerRepository(
                transactionDao = database.transactionDao(),
                goalDao = database.goalDao(),
                streakDao = database.streakDao(),
                billDao = database.billDao(),
                accountDao = database.accountDao()
            )

            activity.lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.addTransaction(
                            TransactionEntity(
                                amount = amountValue,
                                type = selectedType,
                                date = selectedDateMillis.toUtcMidnight(),
                                accountId = when (selectedType) {
                                    TransactionType.SAVING -> AccountEntity.DEFAULT_SAVINGS_ID
                                    TransactionType.INCOME -> AccountEntity.DEFAULT_CASH_ID
                                    TransactionType.EXPENSE -> AccountEntity.DEFAULT_CASH_ID
                                },
                                note = note,
                                category = selectedCategory.key
                            )
                        )
                        if (selectedType == TransactionType.SAVING) {
                            repository.refreshSavingStreakFromTransactions()
                        }
                    }
                }.onSuccess {
                    AnalyticsTracker.logPlannerTransactionSaved(
                        activity,
                        selectedType.name,
                        selectedCategory.key,
                        "calculator_inline_sheet"
                    )
                    AnalyticsTracker.logCalculatorAddToPlanner(
                        activity,
                        activity.javaClass.simpleName,
                        "success"
                    )
                    Toast.makeText(activity, "Added to Planner", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure {
                    AnalyticsTracker.logCalculatorAddToPlanner(
                        activity,
                        activity.javaClass.simpleName,
                        "failed"
                    )
                    Toast.makeText(activity, "Failed to add transaction", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun categoryOptionsFor(type: TransactionType): List<CategoryOption> {
        return when (type) {
            TransactionType.EXPENSE -> listOf(
                CategoryOption("housing", "Housing", "🏠"),
                CategoryOption("food", "Food", "🍽️"),
                CategoryOption("transport", "Transport", "🚗"),
                CategoryOption("utilities", "Utilities", "💡"),
                CategoryOption("shopping", "Shopping", "🛍️"),
                CategoryOption("health", "Healthcare", "💊"),
                CategoryOption("bills", "Bills", "🧾"),
                CategoryOption("entertainment", "Entertainment", "🎬"),
                CategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Expense", "🏷️")
            )
            TransactionType.INCOME -> listOf(
                CategoryOption("salary", "Salary", "💼"),
                CategoryOption("bonus", "Bonus", "💰"),
                CategoryOption("freelance", "Freelance", "🧑‍💻"),
                CategoryOption("business", "Business", "🏢"),
                CategoryOption("investment", "Investment", "📈"),
                CategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Income", "🏷️")
            )
            TransactionType.SAVING -> listOf(
                CategoryOption("emergency_fund", "Emergency Fund", "🏦"),
                CategoryOption("general_savings", "General Savings", "💰")
            )
        }
    }

    private fun updateActionButton(button: MaterialButton, type: TransactionType) {
        when (type) {
            TransactionType.EXPENSE -> {
                button.text = "Add Expense"
                button.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"))
            }
            TransactionType.INCOME -> {
                button.text = "Add Income"
                button.setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
            }
            TransactionType.SAVING -> {
                button.text = "Add Saving"
                button.setBackgroundColor(android.graphics.Color.parseColor("#3B82F6"))
            }
        }
    }

    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun parseAmount(raw: String): Double? {
        val normalized = raw.replace(",", "").trim()
        return normalized.toDoubleOrNull()
    }

    private fun formatAmountForInput(value: Double): String {
        val rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        return rounded.stripTrailingZeros().toPlainString()
    }

    private fun buildPlannerNote(title: String, note: String): String {
        val calculatorName = title.trim().ifBlank { "Calculator" }
        val raw = note.trim()
        if (raw.isBlank()) return "Result from $calculatorName"

        val hasNumber = raw.any { it.isDigit() }
        return when {
            raw.contains(calculatorName, ignoreCase = true) -> raw
            hasNumber -> "Result from $calculatorName"
            else -> "$raw from $calculatorName"
        }
    }
}
