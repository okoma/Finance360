package com.i2medier.financialpro.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.daysBetweenUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.presentation.PlannerViewModel
import com.i2medier.financialpro.planner.presentation.TransactionHistoryAdapter
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class GoalDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GOAL_ID = "extra_goal_id"
    }

    private lateinit var plannerViewModel: PlannerViewModel
    private var goalId: Long = 0L

    private var currentGoal: GoalEntity? = null
    private var allGoals: List<GoalEntity> = emptyList()
    private var goalProgress: Map<Long, Double> = emptyMap()
    private var savingTransactionsByGoal: Int = 0
    private var goalTimelineTransactions: List<TransactionEntity> = emptyList()
    private var hasLoadedGoals = false

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvDaysLeft: TextView
    private lateinit var tvDailySuggestion: TextView
    private lateinit var tvTargetDate: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvLinkedSavings: TextView
    private lateinit var goalTimelineAdapter: TransactionHistoryAdapter
    private lateinit var tvTimelineEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_goal_detail)

        goalId = intent.getLongExtra(EXTRA_GOAL_ID, 0L)
        if (goalId <= 0L) {
            finish()
            return
        }

        plannerViewModel = ViewModelProvider(
            this,
            PlannerViewModel.Factory(application)
        )[PlannerViewModel::class.java]

        tvTitle = findViewById(R.id.tvGoalTitle)
        tvDescription = findViewById(R.id.tvGoalDescription)
        tvProgress = findViewById(R.id.tvGoalProgress)
        progressBar = findViewById(R.id.progressGoal)
        tvDaysLeft = findViewById(R.id.tvGoalDaysLeft)
        tvDailySuggestion = findViewById(R.id.tvGoalDailySuggestion)
        tvTargetDate = findViewById(R.id.tvGoalTargetDate)
        tvCreatedAt = findViewById(R.id.tvGoalCreatedAt)
        tvLinkedSavings = findViewById(R.id.tvGoalLinkedSavings)
        tvTimelineEmpty = findViewById(R.id.tvGoalTimelineEmpty)

        goalTimelineAdapter = TransactionHistoryAdapter(
            onDeleteClicked = { transaction -> confirmDeleteTransaction(transaction) }
        )
        findViewById<RecyclerView>(R.id.rvGoalTimeline).apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = goalTimelineAdapter
            isNestedScrollingEnabled = true
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddMoney).setOnClickListener {
            currentGoal?.let { goal -> showAddMoneyDialog(goal) }
        }
        findViewById<Button>(R.id.btnEditGoal).setOnClickListener {
            currentGoal?.let { goal -> showEditGoalDialog(goal) }
        }
        findViewById<Button>(R.id.btnDeleteGoal).setOnClickListener {
            currentGoal?.let { goal -> confirmDeleteGoal(goal) }
        }

        val root = findViewById<View>(R.id.rootGoalDetail)
        val header = findViewById<View>(R.id.layoutGoalDetailHeader)
        val scroll = findViewById<View>(R.id.scrollGoalDetail)
        val headerTop = header.paddingTop
        val headerLeft = header.paddingLeft
        val headerRight = header.paddingRight
        val headerBottom = header.paddingBottom
        val scrollBottom = scroll.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(
                left = headerLeft + bars.left,
                top = headerTop + bars.top,
                right = headerRight + bars.right,
                bottom = headerBottom
            )
            scroll.updatePadding(bottom = scrollBottom + bars.bottom)
            insets
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    plannerViewModel.goals.collect { goals ->
                        hasLoadedGoals = true
                        allGoals = goals
                        val matched = goals.find { it.id == goalId }
                        when {
                            matched != null -> {
                                currentGoal = matched
                                renderGoal()
                            }
                            currentGoal != null -> {
                                // Goal was deleted while viewing details.
                                finish()
                            }
                            goals.isNotEmpty() -> {
                                // Goals are loaded and requested id doesn't exist.
                                finish()
                            }
                            else -> Unit // Wait for DB emission instead of closing on initial empty state.
                        }
                    }
                }
                launch {
                    plannerViewModel.goalProgress.collect { progress ->
                        goalProgress = progress
                        if (currentGoal != null) {
                            renderGoal()
                        }
                    }
                }
                launch {
                    plannerViewModel.transactions.collect { transactions ->
                        goalTimelineTransactions = transactions.filter { it.goalId == goalId }
                        savingTransactionsByGoal = goalTimelineTransactions.count { it.type == TransactionType.SAVING }
                        if (currentGoal != null || hasLoadedGoals) {
                            submitGoalTimeline()
                        }
                        if (currentGoal != null) {
                            renderGoal()
                        }
                    }
                }
            }
        }
    }

    private fun submitGoalTimeline() {
        val items = goalTimelineTransactions.take(30)
        goalTimelineAdapter.submitList(items)
        tvTimelineEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderGoal() {
        val goal = currentGoal ?: return
        val savedAmount = goalProgress[goal.id] ?: 0.0
        val target = goal.targetAmount
        val percent = if (target > 0.0) {
            ((savedAmount / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
        } else {
            0
        }

        tvTitle.text = goal.title
        tvDescription.text = goal.description
        tvDescription.visibility = if (goal.description.isNullOrBlank()) View.GONE else View.VISIBLE
        tvProgress.text = getString(
            R.string.planner_goal_detail_progress,
            formatCurrency(savedAmount),
            formatCurrency(target)
        )
        progressBar.progress = percent

        tvTargetDate.text = if (goal.targetDate != null) {
            getString(R.string.planner_goal_detail_target_date, formatDisplayDate(goal.targetDate))
        } else {
            getString(R.string.planner_goal_detail_no_target_date)
        }
        tvCreatedAt.text = getString(R.string.planner_goal_detail_created_at, formatDisplayDate(goal.createdAt))
        tvLinkedSavings.text = getString(R.string.planner_goal_detail_linked_savings, savingTransactionsByGoal)

        bindDeadlineInsights(goal, savedAmount, target)
    }

    private fun bindDeadlineInsights(goal: GoalEntity, savedAmount: Double, targetAmount: Double) {
        val targetDate = goal.targetDate
        if (targetDate == null) {
            tvDaysLeft.visibility = View.GONE
            tvDailySuggestion.visibility = View.GONE
            return
        }

        val todayUtc = System.currentTimeMillis().toUtcMidnight()
        val targetUtc = targetDate.toUtcMidnight()
        val daysLeft = daysBetweenUtc(todayUtc, targetUtc)
        val remaining = (targetAmount - savedAmount).coerceAtLeast(0.0)

        when {
            daysLeft < 0 -> {
                tvDaysLeft.text = getString(R.string.planner_goal_days_overdue, -daysLeft)
                tvDaysLeft.visibility = View.VISIBLE
                tvDailySuggestion.visibility = View.GONE
            }

            daysLeft == 0 -> {
                tvDaysLeft.text = getString(R.string.planner_goal_due_today)
                tvDaysLeft.visibility = View.VISIBLE
                tvDailySuggestion.visibility = View.GONE
            }

            else -> {
                tvDaysLeft.text = getString(R.string.planner_goal_days_left, daysLeft)
                tvDaysLeft.visibility = View.VISIBLE

                if (remaining > 0.0) {
                    val suggestedDaily = remaining / daysLeft.toDouble()
                    tvDailySuggestion.text = getString(
                        R.string.planner_goal_daily_suggestion,
                        formatCurrency(suggestedDaily)
                    )
                    tvDailySuggestion.visibility = View.VISIBLE
                } else {
                    tvDailySuggestion.visibility = View.GONE
                }
            }
        }
    }

    private fun showAddMoneyDialog(goal: GoalEntity) {
        var selectedDateMillis = System.currentTimeMillis()
        val dialog = BottomSheetDialog(this, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_add_to_goal, null, false)
        dialog.setContentView(content)

        val savedAmount = goalProgress[goal.id] ?: 0.0
        val remainingBefore = (goal.targetAmount - savedAmount).coerceAtLeast(0.0)

        val btnClose = content.findViewById<ImageView>(R.id.btnClose)
        val tvGoalName = content.findViewById<TextView>(R.id.tvGoalName)
        val tvGoalProgress = content.findViewById<TextView>(R.id.tvGoalProgress)
        val progressBar = content.findViewById<LinearProgressIndicator>(R.id.goalProgressBar)
        val tvLeftAmount = content.findViewById<TextView>(R.id.tvLeftAmount)
        val tvRightAmount = content.findViewById<TextView>(R.id.tvRightAmount)
        val amountInput = content.findViewById<TextInputEditText>(R.id.etAmount)
        val amountLayout = content.findViewById<TextInputLayout>(R.id.tilAmount)
        val dateInput = content.findViewById<TextInputEditText>(R.id.etDate)
        val dateLayout = content.findViewById<TextInputLayout>(R.id.tilDate)
        val btnAdd = content.findViewById<MaterialButton>(R.id.btnAdd)
        val tvProgressSummary = content.findViewById<TextView>(R.id.tvProgressSummary)

        tvGoalName.text = goal.title
        tvGoalProgress.text = "${formatCurrency(savedAmount)} / ${formatCurrency(goal.targetAmount)}"
        tvLeftAmount.text = getString(R.string.planner_goal_left_amount, formatCurrency(remainingBefore))
        tvRightAmount.text = tvLeftAmount.text
        val initialPercent = if (goal.targetAmount > 0.0) {
            ((savedAmount / goal.targetAmount) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        progressBar.progress = initialPercent
        tvProgressSummary.text = getString(
            R.string.planner_goal_saved_progress,
            formatCurrency(savedAmount),
            formatCurrency(goal.targetAmount)
        )

        amountInput.setText("")
        amountLayout.hint = amountPlaceholder()
        amountInput.enableCurrencyFormatting()
        dateInput.setText(formatBottomSheetDate(selectedDateMillis))

        fun updatePreviewWithAmount() {
            val addAmount = parseCurrencyInput(amountInput.text?.toString().orEmpty()) ?: 0.0
            val newSaved = (savedAmount + addAmount).coerceAtLeast(0.0)
            val remaining = (goal.targetAmount - newSaved).coerceAtLeast(0.0)
            val percent = if (goal.targetAmount > 0.0) {
                ((newSaved / goal.targetAmount) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                0
            }
            progressBar.progress = percent
            tvGoalProgress.text = "${formatCurrency(newSaved)} / ${formatCurrency(goal.targetAmount)}"
            tvLeftAmount.text = getString(R.string.planner_goal_left_amount, formatCurrency(remaining))
            tvRightAmount.text = tvLeftAmount.text
            tvProgressSummary.text = getString(
                R.string.planner_goal_saved_progress,
                formatCurrency(newSaved),
                formatCurrency(goal.targetAmount)
            )
        }

        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updatePreviewWithAmount()
        })

        val openDatePicker = {
            showDatePicker(selectedDateMillis) { millis ->
                selectedDateMillis = millis
                dateInput.setText(formatBottomSheetDate(selectedDateMillis))
            }
        }
        dateInput.setOnClickListener { openDatePicker() }
        dateLayout.setEndIconOnClickListener { openDatePicker() }
        btnClose.setOnClickListener { dialog.dismiss() }

        btnAdd.setOnClickListener {
            val amount = parseCurrencyInput(amountInput.text?.toString().orEmpty())
            val error = plannerViewModel.addTransaction(
                amount = amount,
                type = TransactionType.SAVING,
                dateMillis = selectedDateMillis,
                goalId = goal.id,
                note = getString(R.string.planner_goal_contribution_note, goal.title),
                category = "all"
            )
            if (error != null) {
                toast(error)
            } else {
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
    }

    private fun showEditGoalDialog(goal: GoalEntity) {
        var selectedTargetDateMillis: Long? = goal.targetDate
        var selectedCategory = GoalCategoryUi.normalize(goal.category)
        val dialog = BottomSheetDialog(this, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_goal_form, null, false)
        dialog.setContentView(content)

        val btnClose = content.findViewById<ImageView>(R.id.btnClose)
        val sheetTitle = content.findViewById<TextView>(R.id.tvSheetTitle)
        val titleInput = content.findViewById<TextInputEditText>(R.id.etGoalTitle)
        val targetInput = content.findViewById<TextInputEditText>(R.id.etGoalTargetAmount)
        val targetLayout = content.findViewById<TextInputLayout>(R.id.tilGoalTargetAmount)
        val descriptionInput = content.findViewById<TextInputEditText>(R.id.etGoalDescription)
        val categoryCard = content.findViewById<View>(R.id.cardGoalCategory)
        val categoryEmoji = content.findViewById<TextView>(R.id.tvGoalCategoryEmoji)
        val categoryName = content.findViewById<TextView>(R.id.tvGoalCategoryName)
        val dateInput = content.findViewById<TextInputEditText>(R.id.etGoalTargetDate)
        val dateLayout = content.findViewById<TextInputLayout>(R.id.tilGoalTargetDate)
        val saveButton = content.findViewById<MaterialButton>(R.id.btnGoalSave)

        sheetTitle.text = getString(R.string.planner_edit_goal_title)
        saveButton.text = getString(R.string.planner_save)

        titleInput.filters = arrayOf(InputFilter.LengthFilter(30))
        descriptionInput.filters = arrayOf(InputFilter.LengthFilter(100))
        titleInput.setText(goal.title)
        targetLayout.hint = amountPlaceholder()
        targetInput.setText(formatCurrencyInput(goal.targetAmount))
        targetInput.enableCurrencyFormatting()
        descriptionInput.setText(goal.description.orEmpty())
        categoryEmoji.text = GoalCategoryUi.emojiFor(selectedCategory)
        categoryName.text = GoalCategoryUi.labelFor(selectedCategory)

        categoryCard.setOnClickListener {
            val options = GoalCategoryUi.options()
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.planner_hint_category))
                .setItems(labels) { _, which ->
                    val selected = options[which]
                    selectedCategory = selected.key
                    categoryEmoji.text = selected.emoji
                    categoryName.text = selected.label
                }
                .show()
        }

        fun updateTargetDateText() {
            dateInput.setText(
                selectedTargetDateMillis?.let { getString(R.string.planner_goal_target_date_selected, formatDisplayDate(it)) }
                    ?: getString(R.string.planner_goal_target_date_optional)
            )
        }
        updateTargetDateText()

        val openDatePicker = {
            showDatePicker(selectedTargetDateMillis ?: System.currentTimeMillis()) { millis ->
                selectedTargetDateMillis = millis.toUtcMidnight()
                updateTargetDateText()
            }
        }
        dateInput.setOnClickListener { openDatePicker() }
        dateLayout.setEndIconOnClickListener { openDatePicker() }
        dateInput.setOnLongClickListener {
            selectedTargetDateMillis = null
            updateTargetDateText()
            true
        }
        btnClose.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            val target = parseCurrencyInput(targetInput.text?.toString().orEmpty())
            val description = descriptionInput.text?.toString()?.trim().orEmpty().ifBlank { null }
            val error = plannerViewModel.updateGoal(
                goal = goal,
                title = title,
                targetAmount = target,
                description = description,
                targetDate = selectedTargetDateMillis,
                category = selectedCategory
            )
            if (error != null) {
                toast(error)
            } else {
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
    }

    private fun confirmDeleteGoal(goal: GoalEntity) {
        if (savingTransactionsByGoal == 0) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.planner_delete_goal))
                .setMessage(getString(R.string.planner_delete_goal_message, goal.title))
                .setNegativeButton(getString(android.R.string.cancel), null)
                .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                    plannerViewModel.deleteGoal(goal)
                    finish()
                }
                .show()
            return
        }

        val options = arrayOf(
            getString(R.string.planner_delete_goal_keep_transactions),
            getString(R.string.planner_delete_goal_with_transactions),
            getString(R.string.planner_delete_goal_reassign_transactions)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_delete_goal))
            .setMessage(getString(R.string.planner_delete_goal_options_message, savingTransactionsByGoal))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        plannerViewModel.deleteGoal(goal)
                        finish()
                    }

                    1 -> {
                        plannerViewModel.deleteGoalAndTransactions(goal)
                        finish()
                    }

                    2 -> showReassignGoalDialog(goal)
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun showReassignGoalDialog(oldGoal: GoalEntity) {
        val candidates = allGoals.filter { it.id != oldGoal.id }
        if (candidates.isEmpty()) {
            toast(getString(R.string.planner_reassign_requires_goal))
            return
        }
        val labels = candidates.map { it.title }.toTypedArray()
        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_reassign_transactions))
            .setSingleChoiceItems(labels, selectedIndex) { _, which -> selectedIndex = which }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_reassign)) { _, _ ->
                plannerViewModel.reassignGoalAndDelete(oldGoal, candidates[selectedIndex])
                finish()
            }
            .show()
    }

    private fun confirmDeleteTransaction(transaction: TransactionEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_delete_transaction))
            .setMessage(getString(R.string.planner_delete_transaction_message))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                plannerViewModel.deleteTransaction(transaction)
            }
            .show()
    }

    private fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            this,
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
    }

    private fun addField(container: LinearLayout, view: View, topMarginDp: Int) {
        container.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (topMarginDp > 0) topMargin = dp(topMarginDp)
            }
        )
    }

    private fun EditText.styleModalField(iconRes: Int? = null) {
        background = resources.getDrawable(R.drawable.bg_modal_input_outline, theme)
        textSize = 15f
        setPadding(dp(10), dp(9), dp(10), dp(9))
        setCompoundDrawablesWithIntrinsicBounds(iconRes ?: 0, 0, 0, 0)
        compoundDrawablePadding = if (iconRes != null) dp(8) else 0
    }

    private fun EditText.enableCurrencyFormatting() {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencySymbol = NumberFormat.getCurrencyInstance(locale).currency?.symbol ?: symbols.currencySymbol
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
                if (normalized.isBlank()) return
                val value = normalized.toDoubleOrNull() ?: return
                editing = true
                setText(formatCurrencyInput(value))
                setSelection(text.length)
                editing = false
            }
        }
        addTextChangedListener(watcher)
    }

    private fun parseCurrencyInput(text: String): Double? {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencySymbol = NumberFormat.getCurrencyInstance(locale).currency?.symbol ?: symbols.currencySymbol
        val normalized = normalizeCurrencyInput(
            value = text,
            currencySymbol = currencySymbol,
            grouping = symbols.groupingSeparator,
            decimal = symbols.decimalSeparator
        )
        return normalized.toDoubleOrNull()
    }

    private fun normalizeCurrencyInput(
        value: String,
        currencySymbol: String,
        grouping: Char,
        decimal: Char
    ): String {
        val stripped = value
            .replace(currencySymbol, "")
            .replace("\\s".toRegex(), "")
            .replace(grouping.toString(), "")
            .replace(decimal, '.')
        val builder = StringBuilder()
        var dotSeen = false
        stripped.forEach { ch ->
            when {
                ch.isDigit() -> builder.append(ch)
                ch == '.' && !dotSeen -> {
                    builder.append('.')
                    dotSeen = true
                }
            }
        }
        val normalized = builder.toString()
        return if (normalized.startsWith(".")) "0$normalized" else normalized
    }

    private fun formatCurrencyInput(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(value)
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance().apply {
            currency = CurrencyManager.getCurrency(this@GoalDetailActivity)
        }.format(amount)
    }

    private fun formatDisplayDate(millis: Long): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun formatBottomSheetDate(millis: Long): String {
        val formatter = java.text.SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun amountPlaceholder(): String {
        return "${CurrencyManager.getCurrencySymbol(this)}0"
    }

    private fun Button.stylePrimaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_primary, theme)
        setTextColor(resources.getColor(android.R.color.white, theme))
        minimumHeight = dp(46)
        elevation = 2f * resources.displayMetrics.density
    }

    private fun Button.styleSecondaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_secondary, theme)
        setTextColor(resources.getColor(R.color.colorDark, theme))
        minimumHeight = dp(46)
        elevation = 2f * resources.displayMetrics.density
    }

    private fun BottomSheetDialog.applyRoundedCornersAndHeight() {
        findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundColor(Color.TRANSPARENT)
            (bottomSheet.parent as? android.view.View)?.setBackgroundColor(Color.TRANSPARENT)
            (((bottomSheet.parent as? android.view.View)?.parent) as? android.view.View)
                ?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                isFitToContents = true
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window?.navigationBarColor = Color.WHITE
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
