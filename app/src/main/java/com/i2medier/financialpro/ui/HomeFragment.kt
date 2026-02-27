package com.i2medier.financialpro.ui

import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.InputFilter
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.PopupMenu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.activity.MainActivity
import com.i2medier.financialpro.activity.ReminderCenterActivity
import com.i2medier.financialpro.activity.SearchActivity
import com.i2medier.financialpro.adapter.home.BrowseCategory
import com.i2medier.financialpro.adapter.home.BrowseCategoryAdapter
import com.i2medier.financialpro.adapter.home.CalculatorCategory
import com.i2medier.financialpro.adapter.home.CategoryChipAdapter
import com.i2medier.financialpro.adapter.home.FeaturedCalculatorAdapter
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.presentation.PlannerViewModel
import com.i2medier.financialpro.ui.search.RecentCalculatorStore
import com.i2medier.financialpro.util.CurrencyManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var plannerViewModel: PlannerViewModel

    private val calculatorCategories = listOf(
        CalculatorCategory(CalculatorRegistry.CATEGORY_ALL, "All"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_LOANS, "Loans"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_SAVINGS, "Savings"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_INVESTING, "Investing"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_TAX, "Tax"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_BUDGET, "Budget"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_BUSINESS, "Business"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_RETIREMENT, "Retirement"),
        CalculatorCategory(CalculatorRegistry.CATEGORY_INSURANCE, "Insurance")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        plannerViewModel = ViewModelProvider(
            this,
            PlannerViewModel.Factory(requireActivity().application)
        )[PlannerViewModel::class.java]

        val rvCategoryChips = view.findViewById<RecyclerView>(R.id.rvCategoryChips)
        val rvFeatured = view.findViewById<RecyclerView>(R.id.rvFeaturedCalculators)
        val rvBrowseCategories = view.findViewById<RecyclerView>(R.id.rvBrowseCategories)
        val btnPlanMore = view.findViewById<ImageButton>(R.id.btnPlanMenu)
        val tvPlanTitle = view.findViewById<TextView>(R.id.tvPlanTitle)
        val searchIcon = view.findViewById<View>(R.id.btnSearch)
        val notificationBell = view.findViewById<View>(R.id.btnNotifications)
        val nativeAdHome = view.findViewById<FrameLayout>(R.id.nativeAdHome)
        val progressSpent = view.findViewById<ProgressBar>(R.id.progressSpent)
        val tvSpentVsBudget = view.findViewById<TextView>(R.id.tvSpentVsBudgetValue)
        val tvSavingsTarget = view.findViewById<TextView>(R.id.tvSavingsTarget)
        val tvBillsDue = view.findViewById<TextView>(R.id.tvBillsDue)
        rvCategoryChips.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvFeatured.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvBrowseCategories.layoutManager = GridLayoutManager(requireContext(), 2)

        rvCategoryChips.adapter = CategoryChipAdapter(calculatorCategories) { category ->
            viewModel.onCategorySelected(category.id)
        }

        btnPlanMore.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                menuInflater.inflate(R.menu.menu_home_plan_period, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_plan_today -> viewModel.onPeriodSelected(HomePlanPeriod.TODAY)
                        R.id.action_plan_yesterday -> viewModel.onPeriodSelected(HomePlanPeriod.YESTERDAY)
                        R.id.action_plan_last_7_days -> viewModel.onPeriodSelected(HomePlanPeriod.LAST_7_DAYS)
                        R.id.action_plan_month -> viewModel.onPeriodSelected(HomePlanPeriod.THIS_MONTH)
                    }
                    true
                }
            }.show()
        }

        val openPlannerClick = View.OnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_planner)
        }

        view.findViewById<View>(R.id.btnOpenPlanner).setOnClickListener(openPlannerClick)
        view.findViewById<View>(R.id.btnAddGoal).setOnClickListener { showCreateGoalBottomSheet() }
        AdAdmob(requireActivity()).NativeAd(nativeAdHome, requireActivity())
        searchIcon.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        notificationBell.setOnClickListener {
            startActivity(Intent(requireContext(), ReminderCenterActivity::class.java))
        }

        val openCalculatorsClick = View.OnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_calculators)
        }

        val mainActivity = activity as? MainActivity
        val loansCount = CalculatorRegistry.byCategory(requireContext(), CalculatorRegistry.CATEGORY_LOANS).size
        val savingsCount = CalculatorRegistry.byCategory(requireContext(), CalculatorRegistry.CATEGORY_SAVINGS).size
        val investingCount = CalculatorRegistry.byCategory(requireContext(), CalculatorRegistry.CATEGORY_INVESTING).size
        val taxCount = CalculatorRegistry.byCategory(requireContext(), CalculatorRegistry.CATEGORY_TAX).size
        val browseItems = listOf(
            BrowseCategory(
                id = CalculatorRegistry.CATEGORY_LOANS,
                title = "Loans",
                subtitle = "Loan & Mortgage",
                toolCount = loansCount,
                iconRes = R.drawable.ic_money,
                colorRes = R.color.home_browse_loans
            ),
            BrowseCategory(
                id = CalculatorRegistry.CATEGORY_SAVINGS,
                title = "Savings",
                subtitle = "Build Your Savings",
                toolCount = savingsCount,
                iconRes = R.drawable.money_recive,
                colorRes = R.color.home_browse_savings
            ),
            BrowseCategory(
                id = CalculatorRegistry.CATEGORY_INVESTING,
                title = "Investing",
                subtitle = "Grow Your Wealth",
                toolCount = investingCount,
                iconRes = R.drawable.ic_calculator,
                colorRes = R.color.home_browse_investing
            ),
            BrowseCategory(
                id = CalculatorRegistry.CATEGORY_TAX,
                title = "Tax",
                subtitle = "Tax Estimators",
                toolCount = taxCount,
                iconRes = R.drawable.ic_percentage,
                colorRes = R.color.home_browse_tax
            )
        )
        rvBrowseCategories.adapter = BrowseCategoryAdapter(browseItems) { item ->
            mainActivity?.selectCalculatorsCategory(item.id) ?: openCalculatorsClick.onClick(rvBrowseCategories)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.featuredCalculators.collect { items ->
                        rvFeatured.adapter = FeaturedCalculatorAdapter(items) { item ->
                            RecentCalculatorStore.record(requireContext(), item.activityClass.name)
                            startActivity(Intent(requireContext(), item.activityClass))
                        }
                    }
                }
                launch {
                    viewModel.selectedPeriod.collect { period ->
                        tvPlanTitle.text = getString(periodTitleRes(period))
                    }
                }
                launch {
                    viewModel.monthlyPlanStats.collect { stats ->
                        val spent = CurrencyManager.format(requireContext(), stats.totalSpent)
                        val budget = CurrencyManager.format(requireContext(), stats.budget)
                        tvSpentVsBudget.text = getString(R.string.spent_vs_budget_value, spent, budget)
                        progressSpent.progress = if (stats.budget > 0.0) {
                            ((stats.totalSpent / stats.budget) * 100.0).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        tvSavingsTarget.text = getString(
                            R.string.savings_target_value,
                            CurrencyManager.format(requireContext(), stats.totalSaved)
                        )
                        if (stats.billsDue > 0) {
                            val text = getString(R.string.bills_due_value, stats.billsDue)
                            val valueText = stats.billsDue.toString()
                            val start = text.lastIndexOf(valueText)
                            if (start >= 0) {
                                val styled = SpannableString(text).apply {
                                    setSpan(
                                        ForegroundColorSpan(Color.parseColor("#D32F2F")),
                                        start,
                                        start + valueText.length,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                                tvBillsDue.text = styled
                            } else {
                                tvBillsDue.text = text
                            }
                        } else {
                            tvBillsDue.text = getString(R.string.bills_cleared)
                        }
                    }
                }
            }
        }
    }

    private fun periodTitleRes(period: HomePlanPeriod): Int {
        return when (period) {
            HomePlanPeriod.TODAY -> R.string.today_plan
            HomePlanPeriod.YESTERDAY -> R.string.yesterday_plan
            HomePlanPeriod.LAST_7_DAYS -> R.string.last_7_days_plan
            HomePlanPeriod.THIS_MONTH -> R.string.this_month_plan
        }
    }

    private fun showCreateGoalBottomSheet() {
        val context = requireContext()
        var selectedTargetDateMillis: Long? = null
        var selectedCategory = "other"
        val dialog = BottomSheetDialog(context, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_goal_form, null, false)
        dialog.setContentView(content)

        val btnClose = content.findViewById<android.widget.ImageView>(R.id.btnClose)
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

        sheetTitle.text = getString(R.string.planner_create_new_goal)
        saveButton.text = getString(R.string.planner_create)
        titleInput.filters = arrayOf(InputFilter.LengthFilter(30))
        descriptionInput.filters = arrayOf(InputFilter.LengthFilter(100))
        targetLayout.hint = "${CurrencyManager.getCurrencySymbol(context)}0"
        categoryEmoji.text = GoalCategoryUi.emojiFor(selectedCategory)
        categoryName.text = GoalCategoryUi.labelFor(selectedCategory)

        categoryCard.setOnClickListener {
            val options = GoalCategoryUi.options()
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            android.app.AlertDialog.Builder(context)
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
            val target = targetInput.text?.toString()?.trim().orEmpty().toDoubleOrNull()
            val description = descriptionInput.text?.toString()?.trim().orEmpty().ifBlank { null }
            val error = plannerViewModel.addGoal(
                title = title,
                targetAmount = target,
                description = description,
                targetDate = selectedTargetDateMillis,
                category = selectedCategory
            )
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.planner_create_goal), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
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

    private fun formatDisplayDate(millis: Long): String {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(millis))
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
}
