package com.i2medier.financialpro.ui

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.widget.SwitchCompat
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.activity.MainActivity
import com.i2medier.financialpro.activity.BillDetailActivity
import com.i2medier.financialpro.activity.GoalDetailActivity
import com.i2medier.financialpro.activity.NetWorthOverviewActivity
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.StreakEntity
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.BillRepeat
import com.i2medier.financialpro.planner.domain.BillCategoryUi
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.monthEndExclusiveUtc
import com.i2medier.financialpro.planner.domain.monthStartUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.integration.PlannerAddRequest
import com.i2medier.financialpro.planner.integration.PlannerIntegrationContract
import com.i2medier.financialpro.planner.presentation.BillAdapter
import com.i2medier.financialpro.planner.presentation.GoalProgressAdapter
import com.i2medier.financialpro.planner.presentation.GoalProgressItem
import com.i2medier.financialpro.planner.presentation.PlannerViewModel
import com.i2medier.financialpro.planner.presentation.TransactionHistoryAdapter
import com.i2medier.financialpro.planner.reminder.PlannerReminderConstants
import com.i2medier.financialpro.planner.reminder.PlannerReminderManager
import com.i2medier.financialpro.ui.widgets.BudgetDonutChartView
import com.i2medier.financialpro.util.CurrencyManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class PlannerFragment : Fragment() {
    private data class TransactionCategoryOption(
        val key: String,
        val label: String,
        val emoji: String,
        val goalId: Long? = null
    )

    private enum class TransactionFilter {
        ALL,
        INCOME,
        EXPENSE,
        SAVING
    }

    private enum class BillFilter {
        ALL,
        UNPAID,
        PAID,
        DUE_SOON
    }

    private enum class PlannerTopTab {
        SUMMARY,
        HISTORY,
        GOALS,
        BILLS,
        NET_WORTH
    }

    private enum class GoalsSubTab {
        ALL,
        STREAK,
        MISSED,
        TIPS
    }

    private enum class SummaryStreakTab {
        STREAK,
        MISSED,
        TIPS
    }

    companion object {
        private const val STATE_SELECTED_FILTER = "planner_selected_filter"
        private const val STATE_SELECTED_BILL_FILTER = "planner_selected_bill_filter"
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
        private const val TOP_EXPENSE_HIGH_THRESHOLD = 0.30f
    }

    private lateinit var plannerViewModel: PlannerViewModel
    private var currentGoals: List<GoalEntity> = emptyList()
    private var currentGoalProgress: Map<Long, Double> = emptyMap()
    private var allTransactions: List<TransactionEntity> = emptyList()
    private var selectedTransactionFilter: TransactionFilter = TransactionFilter.ALL
    private var selectedBillFilter: BillFilter = BillFilter.ALL
    private var allBills: List<BillEntity> = emptyList()
    private lateinit var reminderStatusView: TextView
    private var pendingReminderTime: Pair<Int, Int>? = null
    private var hasLoadedGoals = false
    private var pendingCalculatorPrefill: PlannerAddRequest? = null
    private var selectedGoalsSubTab: GoalsSubTab = GoalsSubTab.ALL
    private var selectedSummaryStreakTab: SummaryStreakTab = SummaryStreakTab.STREAK
    private var selectedBudgetMonthOffset: Int = 0
    private var pendingBillInsertDialog: BottomSheetDialog? = null
    private var pendingBillInsertCountBefore: Int = -1

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val pending = pendingReminderTime
        pendingReminderTime = null
        if (isGranted && pending != null) {
            val prefs = requireContext().getSharedPreferences(
                PlannerReminderConstants.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            val weekendHour = prefs.getInt(
                PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR,
                PlannerReminderConstants.DEFAULT_WEEKEND_HOUR
            )
            val weekendMinute = prefs.getInt(
                PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE,
                PlannerReminderConstants.DEFAULT_WEEKEND_MINUTE
            )
            val exact = PlannerReminderManager.updateReminderSchedule(
                requireContext(),
                weekdayHour = pending.first,
                weekdayMinute = pending.second,
                weekendHour = weekendHour,
                weekendMinute = weekendMinute
            )
            updateReminderStatus()
            toast(getString(R.string.planner_reminder_saved))
            if (!exact) {
                toast(getString(R.string.planner_exact_alarm_delayed))
            }
        } else if (pending != null) {
            toast(getString(R.string.planner_notification_permission_required))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_planner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PlannerReminderManager.ensureReminderScheduledSmart(requireContext().applicationContext, null)
        reminderStatusView = view.findViewById(R.id.tvReminderStatus)
        updateReminderStatus()
        // Load history ad immediately as it's visible on first tab
        AdAdmob(requireActivity()).NativeAd(
            view.findViewById(R.id.nativeAdPlannerHistory),
            requireActivity()
        )
        // Goals and Bills ads will be loaded lazily when tabs are first opened

        plannerViewModel = ViewModelProvider(
            this,
            PlannerViewModel.Factory(requireActivity().application)
        )[PlannerViewModel::class.java]

        view.findViewById<View>(R.id.btnBackHome).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_home)
        }

        val incomeAmount = view.findViewById<TextView>(R.id.tvIncomeAmount)
        val plannedAmount = view.findViewById<TextView>(R.id.tvPlannedAmount)
        val spentAmount = view.findViewById<TextView>(R.id.tvSpentAmount)
        val savingsAmount = view.findViewById<TextView>(R.id.tvSavingsAmount)
        val budgetHeadline = view.findViewById<TextView>(R.id.tvBudgetHeadline)
        val budgetSpentIncome = view.findViewById<TextView>(R.id.tvBudgetSpentIncome)
        val budgetProgress = view.findViewById<android.widget.ProgressBar>(R.id.progressBudgetUsage)
        val budgetRemaining = view.findViewById<TextView>(R.id.tvBudgetRemaining)
        val budgetSavingsRate = view.findViewById<TextView>(R.id.tvBudgetSavingsRate)
        val budgetTargetValue = view.findViewById<TextView>(R.id.tvBudgetTargetValue)
        val budgetTargetProgress = view.findViewById<android.widget.ProgressBar>(R.id.progressBudgetTargetUsed)
        val budgetTargetHint = view.findViewById<TextView>(R.id.tvBudgetTargetHint)
        val budgetSavingsTargetValue = view.findViewById<TextView>(R.id.tvBudgetSavingsTargetValue)
        val budgetSavingsTargetProgress = view.findViewById<android.widget.ProgressBar>(R.id.progressBudgetSavingsToTarget)
        val budgetSavingsTargetHint = view.findViewById<TextView>(R.id.tvBudgetSavingsTargetHint)
        val budgetDailySpend = view.findViewById<TextView>(R.id.tvBudgetDailySpend)
        val budgetDaysLeft = view.findViewById<TextView>(R.id.tvBudgetDaysLeft)
        val budgetRunRate = view.findViewById<TextView>(R.id.tvBudgetRunRate)
        val budgetTrendSummary = view.findViewById<TextView>(R.id.tvBudgetTrendSummary)
        val budgetTrendBars = listOf(
            view.findViewById<View>(R.id.viewBudgetBar0),
            view.findViewById<View>(R.id.viewBudgetBar1),
            view.findViewById<View>(R.id.viewBudgetBar2),
            view.findViewById<View>(R.id.viewBudgetBar3),
            view.findViewById<View>(R.id.viewBudgetBar4),
            view.findViewById<View>(R.id.viewBudgetBar5),
            view.findViewById<View>(R.id.viewBudgetBar6)
        )
        val budgetTrendDays = listOf(
            view.findViewById<TextView>(R.id.tvBudgetDay0),
            view.findViewById<TextView>(R.id.tvBudgetDay1),
            view.findViewById<TextView>(R.id.tvBudgetDay2),
            view.findViewById<TextView>(R.id.tvBudgetDay3),
            view.findViewById<TextView>(R.id.tvBudgetDay4),
            view.findViewById<TextView>(R.id.tvBudgetDay5),
            view.findViewById<TextView>(R.id.tvBudgetDay6)
        )
        val chipSummaryStreak = view.findViewById<TextView>(R.id.chipSummaryStreak)
        val chipSummaryMissed = view.findViewById<TextView>(R.id.chipSummaryMissed)
        val chipSummaryTips = view.findViewById<TextView>(R.id.chipSummaryTips)
        val panelSummaryStreak = view.findViewById<View>(R.id.panelSummaryStreak)
        val panelSummaryMissed = view.findViewById<View>(R.id.panelSummaryMissed)
        val panelSummaryTips = view.findViewById<View>(R.id.panelSummaryTips)
        val summaryStreakStatus = view.findViewById<TextView>(R.id.tvSummaryStreakStatus)
        val summarySavedTodayStatus = view.findViewById<TextView>(R.id.tvSummarySavedTodayStatus)
        val summaryStreakDayViews = listOf(
            view.findViewById<TextView>(R.id.tvSummaryStreakDay0),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay1),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay2),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay3),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay4),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay5),
            view.findViewById<TextView>(R.id.tvSummaryStreakDay6)
        )
        val summaryMissedBody = view.findViewById<TextView>(R.id.tvSummaryMissedBody)
        val summaryTipsBody = view.findViewById<TextView>(R.id.tvSummaryTipsBody)
        val budgetPrevMonth = view.findViewById<TextView>(R.id.btnBudgetPrevMonth)
        val budgetNextMonth = view.findViewById<TextView>(R.id.btnBudgetNextMonth)
        val budgetMonthLabel = view.findViewById<TextView>(R.id.tvBudgetMonthLabel)
        val budgetCardIncome = view.findViewById<TextView>(R.id.tvBudgetCardIncome)
        val budgetCardSpent = view.findViewById<TextView>(R.id.tvBudgetCardSpent)
        val budgetCardSavings = view.findViewById<TextView>(R.id.tvBudgetCardSavings)
        val budgetComparisonTitle = view.findViewById<TextView>(R.id.tvBudgetComparisonTitle)
        val spendingCurrent = view.findViewById<TextView>(R.id.tvSpendingCurrent)
        val spendingArrow = view.findViewById<ImageView>(R.id.ivSpendingArrow)
        val spendingChange = view.findViewById<TextView>(R.id.tvSpendingChange)
        val savingsCurrent = view.findViewById<TextView>(R.id.tvSavingsCurrent)
        val savingsArrow = view.findViewById<ImageView>(R.id.ivSavingsArrow)
        val savingsChange = view.findViewById<TextView>(R.id.tvSavingsChange)
        val incomeCurrent = view.findViewById<TextView>(R.id.tvIncomeCurrent)
        val incomeArrow = view.findViewById<ImageView>(R.id.ivIncomeArrow)
        val incomeChange = view.findViewById<TextView>(R.id.tvIncomeChange)
        val budgetCenterSpent = view.findViewById<TextView>(R.id.tvBudgetCenterSpent)
        val budgetCenterRemaining = view.findViewById<TextView>(R.id.tvBudgetCenterRemaining)
        val budgetCenterProgress = view.findViewById<android.widget.ProgressBar>(R.id.progressBudgetCenter)
        val budgetMeterLeft = view.findViewById<TextView>(R.id.tvBudgetMeterLeft)
        val budgetMeterRight = view.findViewById<TextView>(R.id.tvBudgetMeterRight)
        val budgetDonutChart = view.findViewById<BudgetDonutChartView>(R.id.donutBudgetChart)
        val budgetSavedAmount = view.findViewById<TextView>(R.id.tvBudgetSavedAmount)
        val budgetMainProgress = view.findViewById<android.widget.ProgressBar>(R.id.progressBudgetMain)
        val budgetTotalSpentInline = view.findViewById<TextView>(R.id.tvBudgetTotalSpentInline)
        val topExpenseNames = listOf(
            view.findViewById<TextView>(R.id.tvTopExpense1Name),
            view.findViewById<TextView>(R.id.tvTopExpense2Name),
            view.findViewById<TextView>(R.id.tvTopExpense3Name)
        )
        val topExpenseIcons = listOf(
            view.findViewById<ImageView>(R.id.ivTopExpense1Icon),
            view.findViewById<ImageView>(R.id.ivTopExpense2Icon),
            view.findViewById<ImageView>(R.id.ivTopExpense3Icon)
        )
        val topExpenseAmounts = listOf(
            view.findViewById<TextView>(R.id.tvTopExpense1Amount),
            view.findViewById<TextView>(R.id.tvTopExpense2Amount),
            view.findViewById<TextView>(R.id.tvTopExpense3Amount)
        )
        val topExpenseProgress = listOf(
            view.findViewById<android.widget.ProgressBar>(R.id.progressTopExpense1),
            view.findViewById<android.widget.ProgressBar>(R.id.progressTopExpense2),
            view.findViewById<android.widget.ProgressBar>(R.id.progressTopExpense3)
        )
        val topExpenseCards = listOf(
            view.findViewById<CardView>(R.id.cardTopExpense1),
            view.findViewById<CardView>(R.id.cardTopExpense2),
            view.findViewById<CardView>(R.id.cardTopExpense3)
        )
        val topExpensesEmpty = view.findViewById<TextView>(R.id.tvTopExpensesEmpty)
        val btnQuickAddExpense = view.findViewById<Button>(R.id.btnQuickAddExpense)
        val btnQuickAddIncome = view.findViewById<Button>(R.id.btnQuickAddIncome)
        val btnQuickAddSaving = view.findViewById<Button>(R.id.btnQuickAddSaving)
        val netWorthTotal = view.findViewById<TextView>(R.id.tvNetWorthTotal)
        val netWorthAssets = view.findViewById<TextView>(R.id.tvNetWorthAssets)
        val netWorthLiabilities = view.findViewById<TextView>(R.id.tvNetWorthLiabilities)
        val netWorthTrend = view.findViewById<TextView>(R.id.tvNetWorthTrend)
        val netWorthHint = view.findViewById<TextView>(R.id.tvNetWorthHint)
        val goalsStreakStatus = view.findViewById<TextView>(R.id.tvGoalsStreakStatus)
        val goalsSavedTodayStatus = view.findViewById<TextView>(R.id.tvGoalsSavedTodayStatus)
        val goalsStreakDayViews = listOf(
            view.findViewById<TextView>(R.id.tvGoalsStreakDay0),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay1),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay2),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay3),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay4),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay5),
            view.findViewById<TextView>(R.id.tvGoalsStreakDay6)
        )
        val goalsMissedBody = view.findViewById<TextView>(R.id.tvGoalsMissedBody)
        val goalsTipsBody = view.findViewById<TextView>(R.id.tvGoalsTipsBody)
        val btnCreateGoal = view.findViewById<Button>(R.id.btnCreateGoal)
        val btnCreateGoalEmpty = view.findViewById<Button>(R.id.btnCreateGoalEmpty)
        val goalsEmptyLayout = view.findViewById<View>(R.id.layoutGoalsEmptyState)
        val rvGoals = view.findViewById<RecyclerView>(R.id.rvGoals)
        val rvTransactions = view.findViewById<RecyclerView>(R.id.rvTransactions)
        val rvBills = view.findViewById<RecyclerView>(R.id.rvBills)
        val transactionsEmptyView = view.findViewById<TextView>(R.id.tvTransactionsEmpty)
        val billsEmptyView = view.findViewById<TextView>(R.id.tvBillsEmpty)
        val btnAddTransaction = view.findViewById<Button>(R.id.btnAddTransaction)
        val btnAddBill = view.findViewById<Button>(R.id.btnAddBill)
        val chipBillAll = view.findViewById<TextView>(R.id.chipBillAll)
        val chipBillUnpaid = view.findViewById<TextView>(R.id.chipBillUnpaid)
        val chipBillPaid = view.findViewById<TextView>(R.id.chipBillPaid)
        val chipBillDueSoon = view.findViewById<TextView>(R.id.chipBillDueSoon)
        val chipAll = view.findViewById<TextView>(R.id.chipFilterAll)
        val chipIncome = view.findViewById<TextView>(R.id.chipFilterIncome)
        val chipExpense = view.findViewById<TextView>(R.id.chipFilterExpense)
        val chipSaving = view.findViewById<TextView>(R.id.chipFilterSaving)
        val panelSummary = view.findViewById<View>(R.id.panelSummary)
        val panelGoals = view.findViewById<View>(R.id.panelGoals)
        val panelGoalsAll = view.findViewById<View>(R.id.panelGoalsAll)
        val panelGoalsStreak = view.findViewById<View>(R.id.panelGoalsStreak)
        val panelGoalsMissed = view.findViewById<View>(R.id.panelGoalsMissed)
        val panelGoalsTips = view.findViewById<View>(R.id.panelGoalsTips)
        val panelNetWorth = view.findViewById<View>(R.id.panelNetWorth)
        val panelHistory = view.findViewById<View>(R.id.panelHistory)
        val panelBills = view.findViewById<View>(R.id.panelBills)
        val layoutGoalsSubTabs = view.findViewById<View>(R.id.layoutGoalsSubTabs)
        val chipGoalsSubAll = view.findViewById<TextView>(R.id.chipGoalsSubAll)
        val chipGoalsSubStreak = view.findViewById<TextView>(R.id.chipGoalsSubStreak)
        val chipGoalsSubMissed = view.findViewById<TextView>(R.id.chipGoalsSubMissed)
        val chipGoalsSubTips = view.findViewById<TextView>(R.id.chipGoalsSubTips)
        val tabSummary = view.findViewById<TextView>(R.id.tabSummary)
        val tabHistory = view.findViewById<TextView>(R.id.tabHistory)
        val tabGoals = view.findViewById<TextView>(R.id.tabGoals)
        val tabAdd = view.findViewById<TextView>(R.id.tabAdd)
        var latestStreak: StreakEntity? = null
        var latestSavedToday = false
        var latestMonthlyStats = com.i2medier.financialpro.planner.data.repository.MonthlyStats()
        var latestPlannedTotal = 0.0

        selectedTransactionFilter = savedInstanceState
            ?.getString(STATE_SELECTED_FILTER)
            ?.let { runCatching { TransactionFilter.valueOf(it) }.getOrNull() }
            ?: TransactionFilter.ALL
        selectedBillFilter = savedInstanceState
            ?.getString(STATE_SELECTED_BILL_FILTER)
            ?.let { runCatching { BillFilter.valueOf(it) }.getOrNull() }
            ?: BillFilter.ALL

        view.findViewById<View>(R.id.cardGoal1).visibility = View.GONE
        view.findViewById<View>(R.id.cardGoal2).visibility = View.GONE

        val goalAdapter = GoalProgressAdapter(
            onGoalClicked = { goal -> openGoalDetail(goal.id) },
            onAddMoneyClicked = { goal -> showAddMoneyDialog(goal) },
            activity = requireActivity()
        )
        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        rvGoals.adapter = goalAdapter
        rvGoals.isNestedScrollingEnabled = true

        fun submitGoalsToList() {
            val items = currentGoals.map { goal ->
                GoalProgressItem(goal = goal, savedAmount = currentGoalProgress[goal.id] ?: 0.0)
            }
            
            // Insert ads every 4 items
            val listWithAds = mutableListOf<GoalProgressAdapter.Item>()
            items.forEachIndexed { index, item ->
                listWithAds.add(GoalProgressAdapter.Item.Goal(item))
                // Insert ad after every 4 items
                if ((index + 1) % 4 == 0 && index < items.size - 1) {
                    listWithAds.add(GoalProgressAdapter.Item.Ad("goal-ad-${index / 4}"))
                }
            }
            
            goalAdapter.submitList(listWithAds)
            val hasGoals = items.isNotEmpty()
            btnCreateGoal.visibility = if (hasGoals) View.VISIBLE else View.GONE
            rvGoals.visibility = if (hasGoals) View.VISIBLE else View.GONE
            goalsEmptyLayout.visibility = if (hasGoals) View.GONE else View.VISIBLE
        }

        val transactionAdapter = TransactionHistoryAdapter(
            onDeleteClicked = { transaction -> confirmDeleteTransaction(transaction) }
        )
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = transactionAdapter
        rvTransactions.isNestedScrollingEnabled = true

        val billAdapter = BillAdapter(
            onBillClicked = { bill -> openBillDetail(bill.id) },
            onMarkPaid = { bill -> confirmMarkBillPaid(bill) },
            onEdit = { bill -> showAddBillDialog(existingBill = bill) },
            onDelete = { bill -> confirmDeleteBill(bill) },
            activity = requireActivity()
        )
        rvBills.layoutManager = LinearLayoutManager(requireContext())
        rvBills.adapter = billAdapter
        rvBills.isNestedScrollingEnabled = true

        fun updateFilterChipStyles() {
            val chips = listOf(
                chipAll to TransactionFilter.ALL,
                chipIncome to TransactionFilter.INCOME,
                chipExpense to TransactionFilter.EXPENSE,
                chipSaving to TransactionFilter.SAVING
            )
            chips.forEach { (chip, filter) ->
                val selected = selectedTransactionFilter == filter
                chip.setBackgroundResource(
                    if (selected) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_unselected
                )
                chip.setTextColor(
                    resources.getColor(
                        if (selected) android.R.color.white else R.color.colorDark,
                        requireContext().theme
                    )
                )
            }
        }

        fun applyTransactionFilter() {
            val filtered = when (selectedTransactionFilter) {
                TransactionFilter.ALL -> allTransactions
                TransactionFilter.INCOME -> allTransactions.filter { it.type == TransactionType.INCOME }
                TransactionFilter.EXPENSE -> allTransactions.filter { it.type == TransactionType.EXPENSE }
                TransactionFilter.SAVING -> allTransactions.filter { it.type == TransactionType.SAVING }
            }.take(20)
            transactionAdapter.submitList(filtered)
            transactionsEmptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            updateFilterChipStyles()
        }

        fun updateBillFilterChipStyles() {
            val chips = listOf(
                chipBillAll to BillFilter.ALL,
                chipBillUnpaid to BillFilter.UNPAID,
                chipBillPaid to BillFilter.PAID,
                chipBillDueSoon to BillFilter.DUE_SOON
            )
            chips.forEach { (chip, filter) ->
                val selected = selectedBillFilter == filter
                chip.setBackgroundResource(
                    if (selected) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_unselected
                )
                chip.setTextColor(
                    resources.getColor(
                        if (selected) android.R.color.white else R.color.colorDark,
                        requireContext().theme
                    )
                )
            }
        }

        fun applyBillFilter() {
            val todayUtc = System.currentTimeMillis().toUtcMidnight()
            val dueSoonEnd = todayUtc + (7L * DAY_IN_MILLIS)
            val filtered = when (selectedBillFilter) {
                BillFilter.ALL -> allBills
                BillFilter.UNPAID -> allBills.filter { !it.isPaid }
                BillFilter.PAID -> allBills.filter { it.isPaid }
                BillFilter.DUE_SOON -> allBills.filter {
                    !it.isPaid && it.dueDate >= todayUtc && it.dueDate < dueSoonEnd
                }
            }
            
            // Insert ads every 4 items
            val listWithAds = mutableListOf<BillAdapter.Item>()
            filtered.forEachIndexed { index, bill ->
                listWithAds.add(BillAdapter.Item.Bill(bill))
                // Insert ad after every 4 items
                if ((index + 1) % 4 == 0 && index < filtered.size - 1) {
                    listWithAds.add(BillAdapter.Item.Ad("bill-ad-${index / 4}"))
                }
            }
            
            billAdapter.submitList(listWithAds)
            billsEmptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            updateBillFilterChipStyles()
        }

        fun setGoalsSubTab(selected: GoalsSubTab) {
            selectedGoalsSubTab = selected
            panelGoalsAll.visibility = if (selected == GoalsSubTab.ALL) View.VISIBLE else View.GONE
            panelGoalsStreak.visibility = if (selected == GoalsSubTab.STREAK) View.VISIBLE else View.GONE
            panelGoalsMissed.visibility = if (selected == GoalsSubTab.MISSED) View.VISIBLE else View.GONE
            panelGoalsTips.visibility = if (selected == GoalsSubTab.TIPS) View.VISIBLE else View.GONE

            val chips = listOf(
                chipGoalsSubAll to GoalsSubTab.ALL,
                chipGoalsSubStreak to GoalsSubTab.STREAK,
                chipGoalsSubMissed to GoalsSubTab.MISSED,
                chipGoalsSubTips to GoalsSubTab.TIPS
            )
            chips.forEach { (chip, tab) ->
                val isSelected = tab == selected
                chip.setBackgroundResource(
                    if (isSelected) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_unselected
                )
                chip.setTextColor(
                    resources.getColor(
                        if (isSelected) android.R.color.white else R.color.colorDark,
                        requireContext().theme
                    )
                )
            }
        }

        fun setSummaryStreakTab(selected: SummaryStreakTab) {
            selectedSummaryStreakTab = selected
            panelSummaryStreak.visibility = if (selected == SummaryStreakTab.STREAK) View.VISIBLE else View.GONE
            panelSummaryMissed.visibility = if (selected == SummaryStreakTab.MISSED) View.VISIBLE else View.GONE
            panelSummaryTips.visibility = if (selected == SummaryStreakTab.TIPS) View.VISIBLE else View.GONE

            val chips = listOf(
                chipSummaryStreak to SummaryStreakTab.STREAK,
                chipSummaryMissed to SummaryStreakTab.MISSED,
                chipSummaryTips to SummaryStreakTab.TIPS
            )
            chips.forEach { (chip, tab) ->
                val isSelected = tab == selected
                chip.setBackgroundResource(
                    if (isSelected) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_unselected
                )
                chip.setTextColor(
                    resources.getColor(
                        if (isSelected) android.R.color.white else R.color.colorDark,
                        requireContext().theme
                    )
                )
            }
        }

        fun updateGoalsSubInsights(
            transactions: List<TransactionEntity>,
            streak: StreakEntity?,
            savedToday: Boolean
        ) {
            val savedTodayTotal = savedTodayTotal(transactions)
            goalsSavedTodayStatus.text = formatSavedTodayStatus(savedTodayTotal)
            goalsStreakStatus.text = formatStreakStatus(streak, savedToday)
            updateRecentSavingDays(transactions, goalsStreakDayViews)
            summarySavedTodayStatus.text = formatSavedTodayStatus(savedTodayTotal)
            summaryStreakStatus.text = formatStreakStatus(streak, savedToday)
            updateRecentSavingDays(transactions, summaryStreakDayViews)

            val todayUtc = System.currentTimeMillis().toUtcMidnight()
            val savedDates = transactions.asSequence()
                .filter { it.type == TransactionType.SAVING && it.amount > 0.0 }
                .map { it.date.toUtcMidnight() }
                .toSet()

            val missedDates = (1..30).map { todayUtc - (it * DAY_IN_MILLIS) }
                .filter { it !in savedDates }
            val recentMissed = missedDates.take(8)
                .joinToString(", ") { formatDisplayDate(it) }
                .ifBlank { "None" }
            goalsMissedBody.text = "Missed in last 30 days: ${missedDates.size}\nRecent missed: $recentMissed"
            summaryMissedBody.text = "Missed in last 30 days: ${missedDates.size}\nRecent missed: $recentMissed"

            val streakDays = streak?.currentStreak ?: 0
            val tips = mutableListOf<String>()
            tips += if (savedToday) "Great job. You saved today." else "Add a small saving today to keep momentum."
            tips += if (streakDays >= 7) "You are building a strong habit (${streakDays} days)." else "Build toward a 7-day streak."
            tips += reminderStatusView.text.toString()
            goalsTipsBody.text = tips.joinToString("\n")
            summaryTipsBody.text = tips.joinToString("\n")
        }

        fun updateBudgetSummary() {
            val utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, selectedBudgetMonthOffset)
            }.timeInMillis
            val monthStart = monthStartUtc(utcNow)
            val monthEnd = monthEndExclusiveUtc(utcNow)
            val monthTransactions = allTransactions.filter { it.date >= monthStart && it.date < monthEnd }
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val spent = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val saved = monthTransactions.filter { it.type == TransactionType.SAVING }.sumOf { it.amount }
            val target = latestPlannedTotal
            val budgetBase = income
            val usedPercent = if (budgetBase > 0.0) ((spent / budgetBase) * 100.0).coerceIn(0.0, 100.0) else 0.0
            val remaining = budgetBase - spent
            val savingsRate = if (income > 0.0) ((saved / income) * 100.0).coerceAtLeast(0.0) else 0.0
            val targetUsagePercent = if (target > 0.0) ((spent / target) * 100.0).coerceIn(0.0, 100.0) else 0.0
            val savingsToTargetPercent = if (target > 0.0) ((saved / target) * 100.0).coerceIn(0.0, 100.0) else 0.0

            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val dayOfMonth = utcCalendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            val maxDayOfMonth = utcCalendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            val daysLeft = (maxDayOfMonth - dayOfMonth).coerceAtLeast(0)
            val dailyAverageSpent = spent / dayOfMonth.toDouble()
            val projectedMonthSpend = dailyAverageSpent * maxDayOfMonth
            val safeDailyBudget = if (daysLeft > 0) (remaining.coerceAtLeast(0.0) / daysLeft.toDouble()) else 0.0

            budgetHeadline.text = when {
                budgetBase <= 0.0 -> "Add monthly income to start budgeting."
                remaining >= 0.0 -> "You are within budget this period."
                else -> "You are over budget this period."
            }
            budgetSpentIncome.text = "Spent ${formatCurrency(spent)} of monthly income ${formatCurrency(budgetBase)}"
            budgetProgress.progress = usedPercent.roundToInt()
            budgetRemaining.text = if (remaining >= 0.0) {
                "Remaining: ${formatCurrency(remaining)}"
            } else {
                "Over budget: ${formatCurrency(kotlin.math.abs(remaining))}"
            }
            budgetSavingsRate.text = "Savings rate: ${"%.1f".format(Locale.getDefault(), savingsRate)}%"

            budgetTargetValue.text = if (target > 0.0) {
                "Target coverage: ${formatCurrency(spent)} of ${formatCurrency(target)}"
            } else {
                "Target coverage: add goals to set your monthly target"
            }
            budgetTargetProgress.progress = targetUsagePercent.roundToInt()
            budgetTargetHint.text = if (target > 0.0) {
                "${"%.1f".format(Locale.getDefault(), targetUsagePercent)}% of target used"
            } else {
                "No target set yet"
            }

            budgetSavingsTargetValue.text = if (target > 0.0) {
                "Saved toward target: ${formatCurrency(saved)} of ${formatCurrency(target)}"
            } else {
                "Saved this month: ${formatCurrency(saved)}"
            }
            budgetSavingsTargetProgress.progress = savingsToTargetPercent.roundToInt()
            budgetSavingsTargetHint.text = if (target > 0.0) {
                "${"%.1f".format(Locale.getDefault(), savingsToTargetPercent)}% target progress"
            } else {
                "Set a target to track savings progress"
            }

            budgetDailySpend.text = "Daily average spent: ${formatCurrency(dailyAverageSpent)}"
            budgetDaysLeft.text = "Days left: $daysLeft • Safe daily budget: ${formatCurrency(safeDailyBudget)}"
            budgetRunRate.text = "Projected month spend: ${formatCurrency(projectedMonthSpend)}"

            // Modern budget section (matches design mock)
            budgetMonthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthStart)
            budgetCardIncome.text = formatCurrency(income)
            budgetCardSpent.text = formatCurrency(spent)
            budgetCardSavings.text = formatCurrency(saved)
            val prevMonthStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = monthStart
                add(Calendar.MONTH, -1)
            }.timeInMillis
            val prevMonthEnd = monthEndExclusiveUtc(prevMonthStart)
            val prevMonthTransactions = allTransactions.filter {
                it.date >= prevMonthStart && it.date < prevMonthEnd
            }
            val prevIncome = prevMonthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val prevSpent = prevMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val prevSaved = prevMonthTransactions.filter { it.type == TransactionType.SAVING }.sumOf { it.amount }

            budgetComparisonTitle.text = "vs ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(prevMonthStart)}"
            spendingCurrent.text = formatCurrency(spent)
            savingsCurrent.text = formatCurrency(saved)
            incomeCurrent.text = formatCurrency(income)

            fun bindBudgetDelta(
                currentValue: Double,
                previousValue: Double,
                arrowView: ImageView,
                valueView: TextView,
                positiveMeansGood: Boolean
            ) {
                val delta = currentValue - previousValue
                if (kotlin.math.abs(delta) < 0.01) {
                    arrowView.setImageResource(R.drawable.ic_minus)
                    arrowView.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
                    valueView.text = "Same"
                    valueView.setTextColor(Color.parseColor("#64748B"))
                    return
                }
                val wentUp = delta > 0.0
                arrowView.setImageResource(if (wentUp) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
                val good = if (positiveMeansGood) wentUp else !wentUp
                val color = Color.parseColor(if (good) "#10B981" else "#EF4444")
                arrowView.imageTintList = ColorStateList.valueOf(color)
                valueView.setTextColor(color)
                valueView.text = formatCurrency(kotlin.math.abs(delta))
            }

            bindBudgetDelta(spent, prevSpent, spendingArrow, spendingChange, positiveMeansGood = false)
            bindBudgetDelta(saved, prevSaved, savingsArrow, savingsChange, positiveMeansGood = true)
            bindBudgetDelta(income, prevIncome, incomeArrow, incomeChange, positiveMeansGood = true)

            budgetCenterSpent.text = formatCurrency(spent)
            budgetCenterRemaining.text = "Remaining: ${formatCurrency(remaining)}"
            budgetCenterProgress.progress = usedPercent.roundToInt()
            budgetMeterLeft.text = formatCurrency(income)
            budgetMeterRight.text = formatCurrency(spent)
            budgetSavedAmount.text = formatCurrency(remaining.coerceAtLeast(0.0))
            budgetMainProgress.progress = if (income > 0.0) {
                ((spent / income).coerceIn(0.0, 1.0) * 100.0).roundToInt()
            } else {
                0
            }
            budgetTotalSpentInline.text = formatCurrency(spent)

            val groupedExpense = monthTransactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE && it.amount > 0.0 }
                .groupBy { tx ->
                    normalizeExpenseCategoryKey(tx.category, tx.note)
                }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(3)

            val topBase = groupedExpense.firstOrNull()?.value ?: 0.0
            topExpensesEmpty.visibility = if (groupedExpense.isEmpty()) View.VISIBLE else View.GONE
            repeat(3) { index ->
                val item = groupedExpense.getOrNull(index)
                if (item == null) {
                    topExpenseCards[index].visibility = View.GONE
                } else {
                    topExpenseCards[index].visibility = View.VISIBLE
                    val categoryKey = item.key
                    topExpenseNames[index].text = expenseCategoryLabel(categoryKey)
                    topExpenseIcons[index].setImageResource(expenseIconForCategory(categoryKey))
                    topExpenseAmounts[index].text = formatCurrency(item.value)
                    val pct = if (topBase > 0.0) ((item.value / topBase) * 100.0).roundToInt() else 0
                    topExpenseProgress[index].progress = pct.coerceIn(0, 100)
                    val shareOfIncome = if (income > 0.0) (item.value / income).toFloat() else 0f
                    val isHighCategory = shareOfIncome >= TOP_EXPENSE_HIGH_THRESHOLD
                    
                    // Match donut chart colors to top expense cards for consistency
                    val categoryColor = BudgetDonutChartView.UI_DONUT_COLORS.getOrNull(index)
                        ?: BudgetDonutChartView.UI_DONUT_SPENT_COLOR
                    
                    topExpenseNames[index].setTextColor(
                        Color.parseColor(if (isHighCategory) "#2E7D32" else "#1E293B")
                    )
                    topExpenseAmounts[index].setTextColor(categoryColor)
                    topExpenseProgress[index].progressTintList = ColorStateList.valueOf(categoryColor)
                }
            }

            // Build donut chart with category breakdown (matching skills design)
            val donutSegments = mutableListOf<BudgetDonutChartView.ChartSegment>()
            
            if (income > 0.0) {
                // Group expenses by category for breakdown
                val categoryExpenses = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE && it.amount > 0.0 }
                    .groupBy { normalizeExpenseCategoryKey(it.category, it.note) }
                    .mapValues { (_, items) -> items.sumOf { it.amount } }
                    .entries
                    .sortedByDescending { it.value }
                
                // Add top expense categories as segments (like skills: Rent, Food, Transport, etc.)
                categoryExpenses.take(4).forEachIndexed { index, (category, amount) ->
                    val pct = ((amount / income) * 100.0).toFloat()
                    if (pct > 0f) {
                        val color = BudgetDonutChartView.UI_DONUT_COLORS.getOrElse(index) { 
                            BudgetDonutChartView.UI_DONUT_SPENT_COLOR 
                        }
                        donutSegments += BudgetDonutChartView.ChartSegment(
                            percentage = pct,
                            color = color
                        )
                    }
                }
                
                // Add savings segment
                if (saved > 0.0) {
                    val savingPct = ((saved / income) * 100.0).toFloat()
                    if (savingPct > 0f) {
                        donutSegments += BudgetDonutChartView.ChartSegment(
                            percentage = savingPct,
                            color = BudgetDonutChartView.UI_DONUT_SAVING_COLOR
                        )
                    }
                }
                
                // Add remaining/unspent segment (matching skills design)
                val totalUsedPct = donutSegments.sumOf { it.percentage.toDouble() }.toFloat()
                if (totalUsedPct < 100f) {
                    donutSegments += BudgetDonutChartView.ChartSegment(
                        percentage = 100f - totalUsedPct,
                        color = BudgetDonutChartView.UI_DONUT_REMAINING_COLOR
                    )
                }
            } else {
                // Fallback: when no income, show expense categories as proportion of total spending
                val categoryExpenses = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE && it.amount > 0.0 }
                    .groupBy { normalizeExpenseCategoryKey(it.category, it.note) }
                    .mapValues { (_, items) -> items.sumOf { it.amount } }
                    .entries
                    .sortedByDescending { it.value }
                
                val totalSpent = categoryExpenses.sumOf { it.value }
                if (totalSpent > 0.0) {
                    categoryExpenses.take(4).forEachIndexed { index, (category, amount) ->
                        val pct = ((amount / totalSpent) * 100.0).toFloat()
                        if (pct > 0f) {
                            val color = BudgetDonutChartView.UI_DONUT_COLORS.getOrElse(index) { 
                                BudgetDonutChartView.UI_DONUT_SPENT_COLOR 
                            }
                            donutSegments += BudgetDonutChartView.ChartSegment(
                                percentage = pct,
                                color = color
                            )
                        }
                    }
                }
            }
            
            budgetDonutChart.setData(donutSegments)
        }

        fun updateBudgetTrendChart() {
            val todayUtc = System.currentTimeMillis().toUtcMidnight()
            val dates = (6 downTo 0).map { offset -> todayUtc - (offset * DAY_IN_MILLIS) }
            val dailyExpenses = dates.map { dateUtc ->
                allTransactions
                    .asSequence()
                    .filter { it.type == TransactionType.EXPENSE && it.date.toUtcMidnight() == dateUtc }
                    .sumOf { it.amount }
            }
            val maxExpense = dailyExpenses.maxOrNull() ?: 0.0
            val totalExpense = dailyExpenses.sum()
            val symbols = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays

            dates.forEachIndexed { index, dateUtc ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = dateUtc
                val dayName = symbols[calendar.get(Calendar.DAY_OF_WEEK)]
                budgetTrendDays[index].text = dayName.take(1)
            }

            val minBarDp = 10
            val maxBarDp = 70
            dailyExpenses.forEachIndexed { index, value ->
                val ratio = if (maxExpense > 0.0) (value / maxExpense).coerceIn(0.0, 1.0) else 0.0
                val heightDp = if (value <= 0.0) minBarDp else (minBarDp + ((maxBarDp - minBarDp) * ratio)).roundToInt()
                val lp = budgetTrendBars[index].layoutParams
                lp.height = dpToPx(heightDp.toFloat())
                budgetTrendBars[index].layoutParams = lp
                budgetTrendBars[index].alpha = if (value <= 0.0) 0.45f else 1f
            }

            budgetTrendSummary.text = if (totalExpense <= 0.0) {
                "No expenses recorded in the last 7 days."
            } else {
                "Last 7 days spent: ${formatCurrency(totalExpense)} • Highest day: ${formatCurrency(maxExpense)}"
            }
        }

        fun updateNetWorthSummary() {
            val totalIncomeAllTime = allTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val totalExpenseAllTime = allTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val totalSavingAllTime = allTransactions
                .filter { it.type == TransactionType.SAVING }
                .sumOf { it.amount }
            val unpaidLiabilities = allBills
                .filter { !it.isPaid }
                .sumOf { it.amount }
            val liquidAssets = (totalIncomeAllTime - totalExpenseAllTime).coerceAtLeast(0.0)
            val assets = liquidAssets + totalSavingAllTime
            val liabilities = unpaidLiabilities
            val netWorth = assets - liabilities
            val monthDelta = latestMonthlyStats.totalIncome - latestMonthlyStats.totalExpense

            netWorthTotal.text = formatCurrency(netWorth)
            netWorthAssets.text = "Assets: ${formatCurrency(assets)}"
            netWorthLiabilities.text = "Liabilities: ${formatCurrency(liabilities)}"
            netWorthTrend.text = if (monthDelta >= 0.0) {
                "This month trend: +${formatCurrency(monthDelta)}"
            } else {
                "This month trend: -${formatCurrency(kotlin.math.abs(monthDelta))}"
            }
            netWorthHint.text = if (liabilities > assets) {
                "Liabilities are above assets. Focus on bill reduction."
            } else {
                "Net worth is stable. Keep growing savings and reducing bills."
            }
        }

        chipAll.setOnClickListener {
            selectedTransactionFilter = TransactionFilter.ALL
            applyTransactionFilter()
        }
        chipIncome.setOnClickListener {
            selectedTransactionFilter =
                if (selectedTransactionFilter == TransactionFilter.INCOME) TransactionFilter.ALL
                else TransactionFilter.INCOME
            applyTransactionFilter()
        }
        chipExpense.setOnClickListener {
            selectedTransactionFilter =
                if (selectedTransactionFilter == TransactionFilter.EXPENSE) TransactionFilter.ALL
                else TransactionFilter.EXPENSE
            applyTransactionFilter()
        }
        chipSaving.setOnClickListener {
            selectedTransactionFilter =
                if (selectedTransactionFilter == TransactionFilter.SAVING) TransactionFilter.ALL
                else TransactionFilter.SAVING
            applyTransactionFilter()
        }
        updateFilterChipStyles()

        chipBillAll.setOnClickListener {
            selectedBillFilter = BillFilter.ALL
            applyBillFilter()
        }
        chipBillUnpaid.setOnClickListener {
            selectedBillFilter = if (selectedBillFilter == BillFilter.UNPAID) BillFilter.ALL else BillFilter.UNPAID
            applyBillFilter()
        }
        chipBillPaid.setOnClickListener {
            selectedBillFilter = if (selectedBillFilter == BillFilter.PAID) BillFilter.ALL else BillFilter.PAID
            applyBillFilter()
        }
        chipBillDueSoon.setOnClickListener {
            selectedBillFilter = if (selectedBillFilter == BillFilter.DUE_SOON) BillFilter.ALL else BillFilter.DUE_SOON
            applyBillFilter()
        }
        updateBillFilterChipStyles()

        fun setTopTab(selected: PlannerTopTab) {
            panelSummary.visibility = if (selected == PlannerTopTab.SUMMARY) View.VISIBLE else View.GONE
            panelGoals.visibility = if (selected == PlannerTopTab.GOALS) View.VISIBLE else View.GONE
            panelHistory.visibility = if (selected == PlannerTopTab.HISTORY) View.VISIBLE else View.GONE
            panelBills.visibility = if (selected == PlannerTopTab.BILLS) View.VISIBLE else View.GONE
            panelNetWorth.visibility = if (selected == PlannerTopTab.NET_WORTH) View.VISIBLE else View.GONE
            
            layoutGoalsSubTabs.visibility = View.GONE
            val tabs = listOf(
                tabSummary to PlannerTopTab.SUMMARY,
                tabHistory to PlannerTopTab.HISTORY,
                tabGoals to PlannerTopTab.GOALS,
                tabAdd to PlannerTopTab.BILLS
            )
            tabs.forEach { (tabView, tab) ->
                val isSelected = tab == selected
                tabView.setBackgroundResource(
                    if (isSelected) R.drawable.bg_planner_tab_selected else android.R.color.transparent
                )
                tabView.setTextColor(
                    resources.getColor(
                        if (isSelected) android.R.color.white else R.color.colorDark,
                        requireContext().theme
                    )
                )
                tabView.paint.isFakeBoldText = isSelected
            }
        }

        tabSummary.setOnClickListener {
            setTopTab(PlannerTopTab.SUMMARY)
        }
        tabHistory.setOnClickListener {
            setTopTab(PlannerTopTab.HISTORY)
        }
        tabGoals.setOnClickListener {
            setTopTab(PlannerTopTab.GOALS)
        }
        tabAdd.setOnClickListener {
            setTopTab(PlannerTopTab.BILLS)
        }
        view.findViewById<View>(R.id.btnReminderSettings).setOnClickListener {
            startActivity(android.content.Intent(requireContext(), NetWorthOverviewActivity::class.java))
        }
        chipGoalsSubAll.setOnClickListener { setGoalsSubTab(GoalsSubTab.ALL) }
        chipGoalsSubStreak.setOnClickListener { setGoalsSubTab(GoalsSubTab.STREAK) }
        chipGoalsSubMissed.setOnClickListener { setGoalsSubTab(GoalsSubTab.MISSED) }
        chipGoalsSubTips.setOnClickListener { setGoalsSubTab(GoalsSubTab.TIPS) }
        chipSummaryStreak.setOnClickListener { setSummaryStreakTab(SummaryStreakTab.STREAK) }
        chipSummaryMissed.setOnClickListener { setSummaryStreakTab(SummaryStreakTab.MISSED) }
        chipSummaryTips.setOnClickListener { setSummaryStreakTab(SummaryStreakTab.TIPS) }
        setGoalsSubTab(GoalsSubTab.ALL)
        setSummaryStreakTab(SummaryStreakTab.STREAK)
        val requestedTab = PlannerIntegrationContract.getRequestedTab(activity?.intent)
        when (requestedTab) {
            PlannerIntegrationContract.TAB_HISTORY -> setTopTab(PlannerTopTab.HISTORY)
            PlannerIntegrationContract.TAB_GOALS -> setTopTab(PlannerTopTab.GOALS)
            PlannerIntegrationContract.TAB_BILLS -> setTopTab(PlannerTopTab.BILLS)
            else -> setTopTab(PlannerTopTab.SUMMARY)
        }
        PlannerIntegrationContract.clearRequestedTab(activity?.intent)

        btnCreateGoal.setOnClickListener {
            showCreateGoalDialog()
        }
        btnCreateGoalEmpty.setOnClickListener {
            showCreateGoalDialog()
        }
        btnAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
        btnAddBill.setOnClickListener {
            showAddBillDialog()
        }
        budgetPrevMonth.setOnClickListener {
            selectedBudgetMonthOffset = (selectedBudgetMonthOffset - 1).coerceAtLeast(-24)
            updateBudgetSummary()
        }
        budgetNextMonth.setOnClickListener {
            selectedBudgetMonthOffset = (selectedBudgetMonthOffset + 1).coerceAtMost(0)
            updateBudgetSummary()
        }
        btnQuickAddExpense.setOnClickListener {
            showAddTransactionDialog(
                prefill = PlannerAddRequest(
                    amount = 0.0,
                    suggestedType = TransactionType.EXPENSE,
                    note = ""
                )
            )
        }
        btnQuickAddIncome.setOnClickListener {
            showAddTransactionDialog(
                prefill = PlannerAddRequest(
                    amount = 0.0,
                    suggestedType = TransactionType.INCOME,
                    note = ""
                )
            )
        }
        btnQuickAddSaving.setOnClickListener {
            showAddTransactionDialog(
                prefill = PlannerAddRequest(
                    amount = 0.0,
                    suggestedType = TransactionType.SAVING,
                    note = ""
                )
            )
        }
        consumeCalculatorPrefillIntent()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    plannerViewModel.monthlyStats.collect { stats ->
                        latestMonthlyStats = stats
                        incomeAmount.text = formatCurrency(stats.totalIncome)
                        spentAmount.text = formatCurrency(stats.totalExpense)
                        savingsAmount.text = formatCurrency(stats.totalSaved)
                        updateBudgetSummary()
                        updateNetWorthSummary()
                    }
                }
                launch {
                    plannerViewModel.plannedTotal.collect { total ->
                        latestPlannedTotal = total
                        plannedAmount.text = formatCurrency(total)
                        updateBudgetSummary()
                    }
                }
                launch {
                    plannerViewModel.goals.collect { goals ->
                        currentGoals = goals
                        submitGoalsToList()
                        hasLoadedGoals = true
                        pendingCalculatorPrefill?.let { request ->
                            pendingCalculatorPrefill = null
                            openPrefilledCalculatorRequest(request)
                        }
                    }
                }
                launch {
                    plannerViewModel.goalProgress.collect { progress ->
                        currentGoalProgress = progress
                        submitGoalsToList()
                    }
                }
                launch {
                    plannerViewModel.transactions.collect { transactions ->
                        allTransactions = transactions
                        applyTransactionFilter()
                        val savedTodayTotal = savedTodayTotal(transactions)
                        latestSavedToday = savedTodayTotal > 0.0
                        updateGoalsSubInsights(transactions, latestStreak, latestSavedToday)
                        updateBudgetTrendChart()
                        updateBudgetSummary()
                        updateNetWorthSummary()
                        updateReminderStatus()
                    }
                }
                launch {
                    plannerViewModel.bills.collect { bills ->
                        if (pendingBillInsertDialog != null && bills.size > pendingBillInsertCountBefore) {
                            selectedBillFilter = BillFilter.ALL
                            pendingBillInsertDialog?.dismiss()
                            pendingBillInsertDialog = null
                            pendingBillInsertCountBefore = -1
                        }
                        allBills = bills
                        applyBillFilter()
                        updateNetWorthSummary()
                    }
                }
                launch {
                    plannerViewModel.streak.collect { streak ->
                        latestStreak = streak
                        updateGoalsSubInsights(allTransactions, streak, latestSavedToday)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_FILTER, selectedTransactionFilter.name)
        outState.putString(STATE_SELECTED_BILL_FILTER, selectedBillFilter.name)
    }

    override fun onDestroyView() {
        // Clean up dialogs to prevent memory leaks
        pendingBillInsertDialog?.dismiss()
        pendingBillInsertDialog = null
        
        super.onDestroyView()
    }

    private fun showCreateGoalDialog(existingGoal: GoalEntity? = null) {
        val context = requireContext()
        var selectedTargetDateMillis: Long? = existingGoal?.targetDate
        var selectedCategory = GoalCategoryUi.normalize(existingGoal?.category)
        val dialog = BottomSheetDialog(context, R.style.PlannerMaterial3BottomSheetDialog)
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

        sheetTitle.text = if (existingGoal == null) getString(R.string.planner_create_new_goal)
        else getString(R.string.planner_edit_goal_title)
        saveButton.text = if (existingGoal == null) getString(R.string.planner_create)
        else getString(R.string.planner_save)

        titleInput.filters = arrayOf(InputFilter.LengthFilter(30))
        descriptionInput.filters = arrayOf(InputFilter.LengthFilter(100))
        targetLayout.hint = amountPlaceholder()
        titleInput.setText(existingGoal?.title.orEmpty())
        targetInput.setText(existingGoal?.targetAmount?.let { formatCurrencyInput(it) }.orEmpty())
        targetInput.enableCurrencyFormatting()
        descriptionInput.setText(existingGoal?.description.orEmpty())
        categoryEmoji.text = GoalCategoryUi.emojiFor(selectedCategory)
        categoryName.text = GoalCategoryUi.labelFor(selectedCategory)

        categoryCard.setOnClickListener {
            val options = GoalCategoryUi.options()
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            AlertDialog.Builder(context)
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
            showDatePicker(
                initialMillis = selectedTargetDateMillis ?: System.currentTimeMillis(),
                onDateSelected = { millis ->
                    selectedTargetDateMillis = millis.toUtcMidnight()
                    updateTargetDateText()
                }
            )
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
            val error = if (existingGoal == null) {
                plannerViewModel.addGoal(
                    title = title,
                    targetAmount = target,
                    description = description,
                    targetDate = selectedTargetDateMillis,
                    category = selectedCategory
                )
            } else {
                plannerViewModel.updateGoal(
                    goal = existingGoal,
                    title = title,
                    targetAmount = target,
                    description = description,
                    targetDate = selectedTargetDateMillis,
                    category = selectedCategory
                )
            }
            if (error != null) {
                toast(error)
            } else {
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
    }

    private fun showAddMoneyDialog(goal: GoalEntity) {
        val context = requireContext()
        var selectedDateMillis = System.currentTimeMillis()
        val dialog = BottomSheetDialog(context, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater
            .inflate(R.layout.bottom_sheet_add_to_goal, null, false)
        dialog.setContentView(content)

        val savedAmount = currentGoalProgress[goal.id] ?: 0.0
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
            showDatePicker(
                initialMillis = selectedDateMillis,
                onDateSelected = { millis ->
                    selectedDateMillis = millis
                    dateInput.setText(formatBottomSheetDate(selectedDateMillis))
                }
            )
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
                category = CalculatorRegistry.CATEGORY_ALL
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

    private fun showAddTransactionDialog(prefill: PlannerAddRequest? = null) {
        val context = requireContext()
        var selectedDateMillis = System.currentTimeMillis()
        val selectableGoals = currentGoals
        val dialog = BottomSheetDialog(context, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater
            .inflate(R.layout.bottom_sheet_add_transaction, null, false)
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

        if (prefill != null && prefill.amount > 0.0) {
            amountInput.setText(formatCurrencyInput(prefill.amount))
        } else {
            amountInput.setText("")
        }
        amountLayout.hint = amountPlaceholder()
        amountInput.enableCurrencyFormatting()
        noteInput.setText(prefill?.note.orEmpty())
        dateInput.setText(formatBottomSheetDate(selectedDateMillis))

        var selectedType = prefill?.suggestedType ?: TransactionType.EXPENSE
        var selectedCategory = TransactionCategoryOption(
            key = prefill?.calculatorCategory ?: CalculatorRegistry.CATEGORY_ALL,
            label = getString(R.string.planner_goal_none),
            emoji = "🏷️"
        )

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
            val options = transactionCategoryOptions(type, selectableGoals)
            selectedCategory = options.firstOrNull() ?: selectedCategory
            tvCategoryEmoji.text = selectedCategory.emoji
            tvCategoryName.text = selectedCategory.label
            updateTransactionActionButton(btnAdd, type)
        }

        setSelectedType(selectedType)
        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val nextType = when (checkedId) {
                btnIncome.id -> TransactionType.INCOME
                btnSavings.id -> TransactionType.SAVING
                else -> TransactionType.EXPENSE
            }
            setSelectedType(nextType)
        }

        categoryCard.setOnClickListener {
            val options = transactionCategoryOptions(selectedType, selectableGoals)
            showCategoryPicker(
                title = getString(R.string.planner_hint_category),
                options = options
            ) { selected ->
                selectedCategory = selected
                tvCategoryEmoji.text = selected.emoji
                tvCategoryName.text = selected.label
            }
        }

        val openDatePicker = {
            showDatePicker(
                initialMillis = selectedDateMillis,
                onDateSelected = { millis ->
                    selectedDateMillis = millis
                    dateInput.setText(formatBottomSheetDate(selectedDateMillis))
                }
            )
        }
        dateInput.setOnClickListener { openDatePicker() }
        dateLayout.setEndIconOnClickListener { openDatePicker() }
        btnClose.setOnClickListener { dialog.dismiss() }

        btnAdd.setOnClickListener {
            val amount = parseCurrencyInput(amountInput.text?.toString().orEmpty())
            val note = noteInput.text?.toString()?.trim().orEmpty().ifBlank { null }
            val error = plannerViewModel.addTransaction(
                amount = amount,
                type = selectedType,
                dateMillis = selectedDateMillis,
                goalId = if (selectedType == TransactionType.SAVING) selectedCategory.goalId else null,
                note = note,
                category = selectedCategory.key
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

    private fun showAddBillDialog(existingBill: BillEntity? = null) {
        val context = requireContext()
        var dueDateMillis = (existingBill?.dueDate ?: System.currentTimeMillis()).toUtcMidnight()
        var selectedBillCategory = BillCategoryUi.normalize(existingBill?.category)

        val dialog = BottomSheetDialog(context, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_bill_form, null, false)
        dialog.setContentView(content)

        val btnClose = content.findViewById<ImageView>(R.id.btnClose)
        val sheetTitle = content.findViewById<TextView>(R.id.tvSheetTitle)
        val titleInput = content.findViewById<TextInputEditText>(R.id.etBillTitle)
        val amountInput = content.findViewById<TextInputEditText>(R.id.etBillAmount)
        val amountLayout = content.findViewById<TextInputLayout>(R.id.tilBillAmount)
        val categoryCard = content.findViewById<View>(R.id.cardBillCategory)
        val categoryEmoji = content.findViewById<TextView>(R.id.tvBillCategoryEmoji)
        val categoryName = content.findViewById<TextView>(R.id.tvBillCategoryName)
        val repeatInput = content.findViewById<TextInputEditText>(R.id.etBillRepeat)
        val repeatLayout = content.findViewById<TextInputLayout>(R.id.tilBillRepeat)
        val dueDateInput = content.findViewById<TextInputEditText>(R.id.etBillDueDate)
        val dueDateLayout = content.findViewById<TextInputLayout>(R.id.tilBillDueDate)
        val saveButton = content.findViewById<MaterialButton>(R.id.btnBillSave)

        sheetTitle.text = if (existingBill == null) getString(R.string.planner_add_bill_title)
        else getString(R.string.planner_edit_bill_title)
        saveButton.text = if (existingBill == null) getString(R.string.planner_add) else getString(R.string.planner_save)

        titleInput.filters = arrayOf(InputFilter.LengthFilter(60))
        titleInput.setText(existingBill?.title.orEmpty())
        amountLayout.hint = amountPlaceholder()
        amountInput.setText(existingBill?.amount?.let { formatCurrencyInput(it) }.orEmpty())
        amountInput.enableCurrencyFormatting()
        categoryEmoji.text = BillCategoryUi.emojiFor(selectedBillCategory)
        categoryName.text = BillCategoryUi.labelFor(selectedBillCategory)

        categoryCard.setOnClickListener {
            val options = BillCategoryUi.options()
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.planner_hint_category))
                .setItems(labels) { _, which ->
                    val selected = options[which]
                    selectedBillCategory = selected.key
                    categoryEmoji.text = selected.emoji
                    categoryName.text = selected.label
                }
                .show()
        }

        val repeatOptions = listOf(
            getString(R.string.planner_bill_repeat_none),
            getString(R.string.planner_bill_repeat_weekly),
            getString(R.string.planner_bill_repeat_monthly)
        )
        var selectedRepeatIndex = when (existingBill?.repeat) {
            BillRepeat.WEEKLY -> 1
            BillRepeat.MONTHLY -> 2
            else -> 0
        }
        fun showRepeatPicker() {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.planner_bill_repeat_hint))
                .setItems(repeatOptions.toTypedArray()) { _, which ->
                    selectedRepeatIndex = which
                    repeatInput.setText(repeatOptions[selectedRepeatIndex])
                }
                .show()
        }
        repeatInput.setText(repeatOptions[selectedRepeatIndex])
        repeatInput.setOnClickListener { showRepeatPicker() }
        repeatLayout.setEndIconOnClickListener { showRepeatPicker() }

        fun updateDueDateText() {
            dueDateInput.setText(getString(R.string.planner_bill_due_date, formatDisplayDate(dueDateMillis)))
        }
        updateDueDateText()
        val openDatePicker = {
            showDatePicker(
                initialMillis = dueDateMillis,
                onDateSelected = { millis ->
                    dueDateMillis = millis.toUtcMidnight()
                    updateDueDateText()
                }
            )
        }
        dueDateInput.setOnClickListener { openDatePicker() }
        dueDateLayout.setEndIconOnClickListener { openDatePicker() }
        btnClose.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            val amount = parseCurrencyInput(amountInput.text?.toString().orEmpty())
            val repeat = when (selectedRepeatIndex) {
                1 -> BillRepeat.WEEKLY
                2 -> BillRepeat.MONTHLY
                else -> null
            }
            val error = if (existingBill == null) {
                plannerViewModel.addBill(
                    title = title,
                    amount = amount,
                    dueDateMillis = dueDateMillis,
                    repeat = repeat,
                    category = selectedBillCategory
                )
            } else {
                plannerViewModel.updateBill(
                    bill = existingBill,
                    title = title,
                    amount = amount,
                    dueDateMillis = dueDateMillis,
                    repeat = repeat,
                    category = selectedBillCategory
                )
            }
            if (error != null) {
                toast(error)
            } else {
                if (existingBill == null) {
                    pendingBillInsertCountBefore = allBills.size
                    pendingBillInsertDialog = dialog
                    saveButton.isEnabled = false
                    saveButton.alpha = 0.7f
                } else {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
    }

    private fun consumeCalculatorPrefillIntent() {
        val hostIntent = activity?.intent ?: return
        val request = PlannerIntegrationContract.getRequestFromIntent(hostIntent) ?: return
        PlannerIntegrationContract.clearPlannerExtras(hostIntent)
        if (hasLoadedGoals) {
            openPrefilledCalculatorRequest(request)
        } else {
            pendingCalculatorPrefill = request
        }
    }

    private fun openPrefilledCalculatorRequest(request: PlannerAddRequest) {
        val source = request.title?.takeIf { it.isNotBlank() } ?: getString(R.string.planner_source_calculator)
        toast(getString(R.string.planner_prefilled_from, source))
        showAddTransactionDialog(prefill = request)
    }

    private fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            requireContext(),
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

    private fun confirmDeleteGoal(goal: GoalEntity) {
        val linkedTransactionCount = allTransactions.count { it.goalId == goal.id }
        if (linkedTransactionCount == 0) {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Goal")
                .setMessage(getString(R.string.planner_delete_goal_message, goal.title))
                .setNegativeButton(getString(android.R.string.cancel), null)
                .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                    plannerViewModel.deleteGoal(goal)
                }
                .show()
            return
        }

        val options = arrayOf(
            getString(R.string.planner_delete_goal_keep_transactions),
            getString(R.string.planner_delete_goal_with_transactions),
            getString(R.string.planner_delete_goal_reassign_transactions)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.planner_delete_goal))
            .setMessage(getString(R.string.planner_delete_goal_options_message, linkedTransactionCount))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> plannerViewModel.deleteGoal(goal)
                    1 -> plannerViewModel.deleteGoalAndTransactions(goal)
                    2 -> showReassignGoalDialog(goal)
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun showReassignGoalDialog(oldGoal: GoalEntity) {
        val candidates = currentGoals.filter { it.id != oldGoal.id }
        if (candidates.isEmpty()) {
            toast(getString(R.string.planner_reassign_requires_goal))
            return
        }
        val labels = candidates.map { it.title }.toTypedArray()
        var selectedIndex = 0
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.planner_reassign_transactions))
            .setSingleChoiceItems(labels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_reassign)) { _, _ ->
                plannerViewModel.reassignGoalAndDelete(oldGoal, candidates[selectedIndex])
            }
            .show()
    }

    private fun confirmDeleteTransaction(transaction: TransactionEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.planner_delete_transaction))
            .setMessage(getString(R.string.planner_delete_transaction_message))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                plannerViewModel.deleteTransaction(transaction)
            }
            .show()
    }

    private fun confirmMarkBillPaid(bill: BillEntity) {
        if (bill.isPaid) return
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.planner_mark_paid))
            .setMessage(getString(R.string.planner_bill_mark_paid_message, bill.title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_mark_paid)) { _, _ ->
                plannerViewModel.markBillPaid(bill)
            }
            .show()
    }

    private fun transactionCategoryOptions(
        type: TransactionType,
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
                TransactionCategoryOption("entertainment", "Entertainment", "🎬"),
                TransactionCategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Expense", "🏷️")
            )
            TransactionType.INCOME -> listOf(
                TransactionCategoryOption("salary", "Salary", "💼"),
                TransactionCategoryOption("bonus", "Bonus", "💰"),
                TransactionCategoryOption("freelance", "Freelance", "🧑‍💻"),
                TransactionCategoryOption("business", "Business", "🏢"),
                TransactionCategoryOption("investment", "Investment", "📈"),
                TransactionCategoryOption(CalculatorRegistry.CATEGORY_ALL, "Other Income", "🏷️")
            )
            TransactionType.SAVING -> {
                val goalOptions = goals.map {
                    TransactionCategoryOption(
                        key = "goal_${it.id}",
                        label = it.title,
                        emoji = "🎯",
                        goalId = it.id
                    )
                }
                goalOptions + listOf(
                    TransactionCategoryOption("emergency_fund", "Emergency Fund", "🏦"),
                    TransactionCategoryOption("general_savings", "General Savings", "💰")
                )
            }
        }
    }

    private fun updateTransactionActionButton(button: MaterialButton, type: TransactionType) {
        when (type) {
            TransactionType.EXPENSE -> {
                button.text = getString(R.string.planner_add_expense)
                button.setBackgroundColor(Color.parseColor("#EF4444"))
            }
            TransactionType.INCOME -> {
                button.text = getString(R.string.planner_add_income)
                button.setBackgroundColor(Color.parseColor("#10B981"))
            }
            TransactionType.SAVING -> {
                button.text = getString(R.string.planner_add_saving)
                button.setBackgroundColor(Color.parseColor("#3B82F6"))
            }
        }
    }

    private fun showCategoryPicker(
        title: String,
        options: List<TransactionCategoryOption>,
        onSelected: (TransactionCategoryOption) -> Unit
    ) {
        val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(labels) { _, which -> onSelected(options[which]) }
            .show()
    }

    private fun formatBottomSheetDate(millis: Long): String {
        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun amountPlaceholder(): String {
        return "${CurrencyManager.getCurrencySymbol(requireContext())}0"
    }

    private fun confirmDeleteBill(bill: BillEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.planner_delete))
            .setMessage(getString(R.string.planner_bill_delete_message, bill.title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                plannerViewModel.deleteBill(bill)
            }
            .show()
    }

    private fun showReminderSettingsDialog() {
        val context = requireContext()
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        var enabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
        var weekdayHour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKDAY_HOUR
        )
        var weekdayMinute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKDAY_MINUTE
        )
        var weekendHour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKEND_HOUR
        )
        var weekendMinute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKEND_MINUTE
        )

        val titleView = TextView(context).apply {
            text = getString(R.string.planner_daily_reminder_settings)
            textSize = 14f
        }
        val toggle = SwitchCompat(context).apply {
            text = getString(R.string.planner_enable_daily_reminders)
            isChecked = enabled
        }
        val weekdayTimeButton = Button(context).apply {
            isAllCaps = false
            text = getString(R.string.planner_weekday_time, formatTime(weekdayHour, weekdayMinute))
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.5f
            setOnClickListener {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        weekdayHour = selectedHour
                        weekdayMinute = selectedMinute
                        text = getString(R.string.planner_weekday_time, formatTime(weekdayHour, weekdayMinute))
                    },
                    weekdayHour,
                    weekdayMinute,
                    false
                ).show()
            }
        }
        val weekendTimeButton = Button(context).apply {
            isAllCaps = false
            text = getString(R.string.planner_weekend_time, formatTime(weekendHour, weekendMinute))
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.5f
            setOnClickListener {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        weekendHour = selectedHour
                        weekendMinute = selectedMinute
                        text = getString(R.string.planner_weekend_time, formatTime(weekendHour, weekendMinute))
                    },
                    weekendHour,
                    weekendMinute,
                    false
                ).show()
            }
        }

        toggle.setOnCheckedChangeListener { _, isChecked ->
            enabled = isChecked
            weekdayTimeButton.isEnabled = isChecked
            weekdayTimeButton.alpha = if (isChecked) 1f else 0.5f
            weekendTimeButton.isEnabled = isChecked
            weekendTimeButton.alpha = if (isChecked) 1f else 0.5f
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).roundToInt()
            setPadding(padding, padding, padding, padding / 2)
            addView(
                titleView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                toggle,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * resources.displayMetrics.density).roundToInt()
                }
            )
            addView(
                weekdayTimeButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (10 * resources.displayMetrics.density).roundToInt()
                }
            )
            addView(
                weekendTimeButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * resources.displayMetrics.density).roundToInt()
                }
            )
        }

        AlertDialog.Builder(context)
            .setTitle(getString(R.string.planner_reminder))
            .setView(content)
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_save)) { _, _ ->
                if (enabled) {
                    scheduleReminderWithPermissionCheck(
                        weekdayHour,
                        weekdayMinute,
                        weekendHour,
                        weekendMinute
                    )
                } else {
                    PlannerReminderManager.disableReminder(context)
                    updateReminderStatus()
                    toast(getString(R.string.planner_reminder_disabled))
                }
            }
            .show()
    }

    private fun scheduleReminderWithPermissionCheck(
        weekdayHour: Int,
        weekdayMinute: Int,
        weekendHour: Int,
        weekendMinute: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingReminderTime = weekdayHour to weekdayMinute
                requireContext().getSharedPreferences(
                    PlannerReminderConstants.PREFS_NAME,
                    android.content.Context.MODE_PRIVATE
                )
                    .edit()
                    .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR, weekendHour)
                    .putInt(PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE, weekendMinute)
                    .apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        val exact = PlannerReminderManager.updateReminderSchedule(
            requireContext(),
            weekdayHour = weekdayHour,
            weekdayMinute = weekdayMinute,
            weekendHour = weekendHour,
            weekendMinute = weekendMinute
        )
        updateReminderStatus()
        toast(getString(R.string.planner_reminder_saved))
        if (!exact) {
            toast(getString(R.string.planner_exact_alarm_delayed))
        }
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance().apply {
            currency = CurrencyManager.getCurrency(requireContext())
        }.format(amount)
    }

    private fun formatDisplayDate(millis: Long): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(cal.time)
    }

    private fun addField(container: LinearLayout, view: View, topMarginDp: Int) {
        container.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (topMarginDp > 0) {
                    topMargin = (topMarginDp * resources.displayMetrics.density).roundToInt()
                }
            }
        )
    }

    private fun EditText.styleModalField(iconRes: Int? = null) {
        background = resources.getDrawable(R.drawable.bg_modal_input_outline, requireContext().theme)
        val padH = (10 * resources.displayMetrics.density).roundToInt()
        val padV = (9 * resources.displayMetrics.density).roundToInt()
        textSize = 15f
        setPadding(padH, padV, padH, padV)
        setCompoundDrawablesWithIntrinsicBounds(iconRes ?: 0, 0, 0, 0)
        compoundDrawablePadding = if (iconRes != null) (8 * resources.displayMetrics.density).roundToInt() else 0
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

    private fun Button.stylePrimaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_primary, requireContext().theme)
        setTextColor(resources.getColor(android.R.color.white, requireContext().theme))
        minimumHeight = (46 * resources.displayMetrics.density).roundToInt()
        elevation = 2f * resources.displayMetrics.density
    }

    private fun Button.styleSecondaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_secondary, requireContext().theme)
        setTextColor(resources.getColor(R.color.colorDark, requireContext().theme))
        minimumHeight = (46 * resources.displayMetrics.density).roundToInt()
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

    private fun updateReminderStatus() {
        val context = requireContext()
        val prefs = context.getSharedPreferences(
            PlannerReminderConstants.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val enabled = prefs.getBoolean(PlannerReminderConstants.KEY_REMINDER_ENABLED, true)
        if (!enabled) {
            reminderStatusView.text = getString(R.string.planner_reminder_off)
            return
        }

        val hour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKDAY_HOUR
        )
        val minute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKDAY_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKDAY_MINUTE
        )
        val weekendHour = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_HOUR,
            PlannerReminderConstants.DEFAULT_WEEKEND_HOUR
        )
        val weekendMinute = prefs.getInt(
            PlannerReminderConstants.KEY_WEEKEND_REMINDER_MINUTE,
            PlannerReminderConstants.DEFAULT_WEEKEND_MINUTE
        )

        val savedToday = allTransactions.any { tx ->
            tx.type == TransactionType.SAVING && tx.date == System.currentTimeMillis().toUtcMidnight()
        }

        val base = getString(
            R.string.planner_reminder_on_weekday_weekend,
            formatTime(hour, minute),
            formatTime(weekendHour, weekendMinute)
        )
        reminderStatusView.text = if (savedToday) {
            getString(R.string.planner_reminder_on_skipped_today, base)
        } else {
            base
        }
    }

    private fun formatStreakStatus(streak: StreakEntity?, savedToday: Boolean): String {
        val days = streak?.currentStreak ?: 0
        val message = when {
            days <= 0 -> getString(R.string.planner_streak_message_start)
            savedToday && days == 1 -> getString(R.string.planner_streak_message_one)
            !savedToday && days == 1 -> getString(R.string.planner_streak_message_continue_today)
            days < 7 -> getString(R.string.planner_streak_message_week)
            days < 30 -> getString(R.string.planner_streak_message_fire)
            else -> getString(R.string.planner_streak_message_consistent)
        }
        return getString(R.string.planner_streak_format, days, message)
    }

    private fun savedTodayTotal(transactions: List<TransactionEntity>): Double {
        val todayUtc = System.currentTimeMillis().toUtcMidnight()
        return transactions
            .filter { tx -> tx.type == TransactionType.SAVING && tx.date == todayUtc }
            .sumOf { it.amount }
    }

    private fun formatSavedTodayStatus(savedTodayTotal: Double): String {
        return if (savedTodayTotal > 0.0) {
            "${getString(R.string.planner_saved_today_yes)} • ${formatCurrency(savedTodayTotal)}"
        } else {
            getString(R.string.planner_saved_today_no)
        }
    }

    private fun updateRecentSavingDays(transactions: List<TransactionEntity>, dayViews: List<TextView>) {
        val savedDates = transactions
            .asSequence()
            .filter { it.type == TransactionType.SAVING && it.amount > 0.0 }
            .map { it.date.toUtcMidnight() }
            .toSet()

        val todayUtc = System.currentTimeMillis().toUtcMidnight()
        dayViews.forEachIndexed { index, dayView ->
            val dayUtc = todayUtc - ((6 - index).toLong() * DAY_IN_MILLIS)
            val saved = savedDates.contains(dayUtc)
            dayView.text = dayShortLabel(dayUtc)
            dayView.background = resources.getDrawable(
                if (saved) R.drawable.bg_streak_day_saved else R.drawable.bg_streak_day_idle,
                requireContext().theme
            )
            dayView.setTextColor(
                resources.getColor(
                    if (saved) android.R.color.white else R.color.colorDark,
                    requireContext().theme
                )
            )
        }
    }

    private fun dayShortLabel(dayUtc: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault()).apply {
            timeInMillis = dayUtc
        }
        val label = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
            ?: "D"
        return label.take(1).uppercase(Locale.getDefault())
    }

    private fun normalizeExpenseCategoryKey(category: String?, note: String?): String {
        val key = category?.trim().orEmpty().lowercase(Locale.getDefault())
        if (key.isNotBlank() && key != CalculatorRegistry.CATEGORY_ALL) return key
        return deriveTopExpenseCategoryFromNote(note)
    }

    private fun deriveTopExpenseCategoryFromNote(note: String?): String {
        val raw = note?.trim().orEmpty()
        if (raw.isBlank()) return CalculatorRegistry.CATEGORY_ALL
        val lower = raw.lowercase(Locale.getDefault())
        return when {
            "rent" in lower || "house" in lower || "mortgage" in lower -> "housing"
            "food" in lower || "grocery" in lower || "groceries" in lower || "lunch" in lower || "dinner" in lower || "breakfast" in lower || "restaurant" in lower -> "food"
            "transport" in lower || "uber" in lower || "taxi" in lower || "bus" in lower || "fuel" in lower || "gas" in lower || "fare" in lower -> "transport"
            "utility" in lower || "electric" in lower || "water" in lower || "internet" in lower || "light" in lower -> "utilities"
            "shop" in lower || "mall" in lower || "buy" in lower -> "shopping"
            "health" in lower || "clinic" in lower || "hospital" in lower || "drug" in lower || "medicine" in lower -> "health"
            "bill" in lower || "subscription" in lower -> "bills"
            "entertainment" in lower || "movie" in lower || "cinema" in lower || "game" in lower || "concert" in lower || "netflix" in lower || "spotify" in lower -> "entertainment"
            else -> CalculatorRegistry.CATEGORY_ALL
        }
    }

    private fun expenseCategoryLabel(categoryKey: String): String {
        return when (categoryKey.lowercase(Locale.getDefault())) {
            "housing" -> "Housing"
            "food" -> "Food"
            "transport" -> "Transport"
            "utilities" -> "Utilities"
            "shopping" -> "Shopping"
            "health" -> "Healthcare"
            "bills" -> "Bills"
            "entertainment" -> "Entertainment"
            else -> getString(R.string.planner_other_expense)
        }
    }

    private fun expenseIconForCategory(categoryKey: String): Int {
        return when (categoryKey.lowercase(Locale.getDefault())) {
            "housing" -> R.drawable.budget_ui_ic_home
            "food" -> R.drawable.budget_ui_ic_restaurant
            "transport" -> R.drawable.budget_ui_ic_directions_car
            "entertainment" -> R.drawable.budget_ui_ic_wallet
            else -> R.drawable.budget_ui_ic_wallet
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }

    private fun openGoalDetail(goalId: Long) {
        val intent = android.content.Intent(requireContext(), GoalDetailActivity::class.java)
            .putExtra(GoalDetailActivity.EXTRA_GOAL_ID, goalId)
        startActivity(intent)
    }

    private fun openBillDetail(billId: Long) {
        val intent = android.content.Intent(requireContext(), BillDetailActivity::class.java)
            .putExtra(BillDetailActivity.EXTRA_BILL_ID, billId)
        startActivity(intent)
    }
}
