package com.i2medier.financialpro.planner.integration

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.AccountEntity
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.data.repository.PlannerRepository
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.domain.validateTransactionInput
import com.i2medier.financialpro.ui.CalculatorRegistry
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
object PlannerCalculatorBridge {
    private data class TransactionCategoryOption(
        val key: String,
        val label: String,
        val emoji: String,
        val goalId: Long? = null
    )
    fun openPlannerWithResult(
        context: Context,
        amount: Double,
        type: TransactionType,
        title: String,
        note: String
    ) {
        if (amount <= 0.0) {
            Toast.makeText(context, "No valid amount to add", Toast.LENGTH_SHORT).show()
            return
        }
        val category = CalculatorRegistry.primaryCategoryForActivity(context.javaClass)
        val request = PlannerAddRequest(
            amount = amount,
            suggestedType = type,
            note = note,
            title = title,
            calculatorCategory = category
        )
        val activity = context as? ComponentActivity
        if (activity == null) {
            Toast.makeText(context, "Unable to open planner sheet", Toast.LENGTH_SHORT).show()
        showInlineBottomSheet(activity, request)
    }
    private fun showInlineBottomSheet(activity: ComponentActivity, request: PlannerAddRequest) {
        activity.lifecycleScope.launch {
            val database = PlannerDatabase.getInstance(activity.applicationContext)
            val repository = PlannerRepository(
                transactionDao = database.transactionDao(),
                goalDao = database.goalDao(),
                streakDao = database.streakDao(),
                billDao = database.billDao(),
                accountDao = database.accountDao()
            )
            val goals = withContext(Dispatchers.IO) { database.goalDao().getAll() }
            var selectedDateMillis = System.currentTimeMillis()
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
            val dateLayout = content.findViewById<TextInputLayout>(R.id.tilDate)
            val categoryCard = content.findViewById<View>(R.id.cardCategory)
            val tvCategoryEmoji = content.findViewById<TextView>(R.id.tvCategoryEmoji)
            val tvCategoryName = content.findViewById<TextView>(R.id.tvCategoryName)
            val btnAdd = content.findViewById<MaterialButton>(R.id.btnAddTransaction)
            if (request.amount > 0.0) {
                amountInput.setText(formatCurrencyInput(request.amount))
            } else {
                amountInput.setText("")
            }
            amountLayout.hint = "${CurrencyManager.getCurrencySymbol(activity)}0"
            amountInput.enableCurrencyFormatting()
            noteInput.setText(request.note)
            dateInput.setText(formatBottomSheetDate(selectedDateMillis))
            var selectedType = request.suggestedType
            var selectedCategory = TransactionCategoryOption(
                key = request.calculatorCategory,
                label = activity.getString(R.string.planner_goal_none),
                emoji = "🏷️"
            fun setSelectedType(type: TransactionType) {
                selectedType = type
                val checkedId = when (type) {
                    TransactionType.EXPENSE -> btnExpense.id
                    TransactionType.INCOME -> btnIncome.id
                    TransactionType.SAVING -> btnSavings.id
                }
                if (toggleType.checkedButtonId != checkedId) {
                    toggleType.check(checkedId)
                val options = transactionCategoryOptions(type, goals)
                selectedCategory = options.find { it.key == selectedCategory.key } ?: options.first()
                tvCategoryEmoji.text = selectedCategory.emoji
                tvCategoryName.text = selectedCategory.label
                updateTransactionActionButton(activity, btnAdd, type)
            setSelectedType(selectedType)
            toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                val nextType = when (checkedId) {
                    btnIncome.id -> TransactionType.INCOME
                    btnSavings.id -> TransactionType.SAVING
                    else -> TransactionType.EXPENSE
                setSelectedType(nextType)
            categoryCard.setOnClickListener {
                val options = transactionCategoryOptions(selectedType, goals)
                showCategoryPicker(
                    context = activity,
                    title = activity.getString(R.string.planner_hint_category),
                    options = options
                ) { selected ->
                    selectedCategory = selected
                    tvCategoryEmoji.text = selected.emoji
                    tvCategoryName.text = selected.label
            val openDatePicker = {
                showDatePicker(
                    activity = activity,
                    initialMillis = selectedDateMillis,
                    onDateSelected = { millis ->
                        selectedDateMillis = millis
                        dateInput.setText(formatBottomSheetDate(selectedDateMillis))
                    }
                )
            dateInput.setOnClickListener { openDatePicker() }
            dateLayout.setEndIconOnClickListener { openDatePicker() }
            btnClose.setOnClickListener { dialog.dismiss() }
            btnAdd.setOnClickListener {
                val amountValue = parseCurrencyInput(amountInput.text?.toString().orEmpty())
                val noteValue = noteInput.text?.toString()?.trim().orEmpty().ifBlank { null }
                val validation = validateTransactionInput(
                    amount = amountValue,
                    type = selectedType,
                    note = noteValue,
                    dateMillis = selectedDateMillis
                if (!validation.isValid) {
                    Toast.makeText(activity, validation.error ?: "Invalid transaction", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    repository.addTransaction(
                        TransactionEntity(
                            amount = amountValue ?: 0.0,
                            type = selectedType,
                            date = selectedDateMillis.toUtcMidnight(),
                            goalId = if (selectedType == TransactionType.SAVING) selectedCategory.goalId else null,
                            accountId = when (selectedType) {
                                TransactionType.SAVING -> AccountEntity.DEFAULT_SAVINGS_ID
                                TransactionType.INCOME -> AccountEntity.DEFAULT_CASH_ID
                                TransactionType.EXPENSE -> AccountEntity.DEFAULT_CASH_ID
                            },
                            note = noteValue,
                            category = selectedCategory.key
                        )
                    )
                    if (selectedType == TransactionType.SAVING) {
                        repository.updateSavingStreakForNow()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Added to planner", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
            dialog.show()
            dialog.applyRoundedCornersAndHeight(activity)
    private fun transactionCategoryOptions(
        goals: List<GoalEntity>
    ): List<TransactionCategoryOption> {
        return when (type) {
            TransactionType.EXPENSE -> listOf(
                TransactionCategoryOption("housing", "Housing", "🏠"),
                TransactionCategoryOption("food", "Food", "🍽️"),
                TransactionCategoryOption("transport", "Transport", "🚗"),
                TransactionCategoryOption("utilities", "Utilities", "💡"),
                TransactionCategoryOption("shopping", "Shopping", "🛍️"),
                TransactionCategoryOption("health", "Healthcare", "💊"),
                TransactionCategoryOption("bills", "Bills", "🧾"),
                TransactionCategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Expense", "🏷️")
            TransactionType.INCOME -> listOf(
                TransactionCategoryOption("salary", "Salary", "💼"),
                TransactionCategoryOption("bonus", "Bonus", "💰"),
                TransactionCategoryOption("freelance", "Freelance", "🧑‍💻"),
                TransactionCategoryOption("business", "Business", "🏢"),
                TransactionCategoryOption("investment", "Investment", "📈"),
                TransactionCategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Income", "🏷️")
            TransactionType.SAVING -> {
                val goalOptions = goals.map {
                    TransactionCategoryOption(
                        key = "goal_${it.id}",
                        label = it.title,
                        emoji = "🎯",
                        goalId = it.id
                goalOptions + listOf(
                    TransactionCategoryOption("emergency_fund", "Emergency Fund", "🏦"),
                    TransactionCategoryOption("general_savings", "General Savings", "💰")
    private fun updateTransactionActionButton(
        button: MaterialButton,
        type: TransactionType
        when (type) {
            TransactionType.EXPENSE -> {
                button.text = context.getString(R.string.planner_add_expense)
                button.setBackgroundColor(Color.parseColor("#EF4444"))
            TransactionType.INCOME -> {
                button.text = context.getString(R.string.planner_add_income)
                button.setBackgroundColor(Color.parseColor("#10B981"))
                button.text = context.getString(R.string.planner_add_saving)
                button.setBackgroundColor(Color.parseColor("#3B82F6"))
    private fun showCategoryPicker(
        options: List<TransactionCategoryOption>,
        onSelected: (TransactionCategoryOption) -> Unit
        val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(labels) { _, which -> onSelected(options[which]) }
            .show()
    private fun showDatePicker(
        activity: ComponentActivity,
        initialMillis: Long,
        onDateSelected: (Long) -> Unit
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            activity,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onDateSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    private fun formatBottomSheetDate(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    private fun TextInputEditText.enableCurrencyFormatting() {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencySymbol = symbols.currencySymbol
        val grouping = symbols.groupingSeparator
        val decimal = symbols.decimalSeparator
        val watcher = object : TextWatcher {
            private var editing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (editing) return
                val raw = s?.toString().orEmpty()
                if (raw.isBlank()) return
                val normalized = normalizeCurrencyInput(raw, currencySymbol, grouping, decimal)
                if (normalized.isBlank() || normalized == "." || normalized == "-") return
                val value = normalized.toDoubleOrNull() ?: return
                val formatted = formatCurrencyInput(value)
                if (formatted == raw) return
                editing = true
                setText(formatted)
                setSelection(formatted.length)
                editing = false
        addTextChangedListener(watcher)
    private fun parseCurrencyInput(text: String): Double? {
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault())
        val currency = symbols.currencySymbol
        val normalized = normalizeCurrencyInput(text, currency, grouping, decimal)
        return normalized.toDoubleOrNull()
    private fun normalizeCurrencyInput(
        raw: String,
        currency: String,
        grouping: Char,
        decimal: Char
    ): String {
        var text = raw.trim()
        if (currency.isNotBlank()) {
            text = text.replace(currency, "")
        text = text.replace(grouping.toString(), "")
        if (decimal != '.') {
            text = text.replace(decimal, '.')
        text = text.replace(Regex("[^0-9.+-]"), "")
        val firstDot = text.indexOf('.')
        if (firstDot >= 0) {
            val before = text.substring(0, firstDot + 1)
            val after = text.substring(firstDot + 1).replace(".", "")
            text = before + after
        return text
    private fun formatCurrencyInput(value: Double): String {
        val symbols = DecimalFormatSymbols.getInstance(Locale.getDefault()).apply {
            decimalSeparator = '.'
        val formatter = DecimalFormat("#,##0.##", symbols)
        return formatter.format(value)
    private fun BottomSheetDialog.applyRoundedCornersAndHeight(context: Context) {
        findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundColor(Color.TRANSPARENT)
            (bottomSheet.parent as? android.view.View)?.setBackgroundColor(Color.TRANSPARENT)
            (((bottomSheet.parent as? android.view.View)?.parent) as? android.view.View)
                ?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            BottomSheetBehavior.from(bottomSheet).apply {
                isFitToContents = true
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
        window?.let { dialogWindow ->
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced = false
        @Suppress("DEPRECATION")
        window?.navigationBarColor = Color.WHITE
}
