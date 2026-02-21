package com.i2medier.financialpro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.monthEndExclusiveUtc
import com.i2medier.financialpro.planner.domain.monthStartUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MonthlyPlanStats(
    val totalSpent: Double = 0.0,
    val budget: Double = 0.0,
    val totalSaved: Double = 0.0,
    val billsDue: Int = 0
)

enum class HomePlanPeriod {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    THIS_MONTH
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val plannerDatabase = PlannerDatabase.getInstance(application)
    private val transactionDao = plannerDatabase.transactionDao()
    private val billDao = plannerDatabase.billDao()
    private val context = application.applicationContext

    private val _selectedCategory = MutableStateFlow(CalculatorRegistry.CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory
    private val _selectedPeriod = MutableStateFlow(HomePlanPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<HomePlanPeriod> = _selectedPeriod

    val featuredCalculators: StateFlow<List<CalculatorRegistry.Item>> = _selectedCategory
        .map { category ->
            if (category == CalculatorRegistry.CATEGORY_ALL) {
                CalculatorRegistry.featured(context)
            } else {
                CalculatorRegistry.byCategory(context, category)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalculatorRegistry.featured(context))

    val monthlyPlanStats: StateFlow<MonthlyPlanStats> = combine(_selectedCategory, _selectedPeriod) { category, period ->
        category to period
    }.flatMapLatest { (category, period) ->
            val now = System.currentTimeMillis()
            val todayStart = now.toUtcMidnight()
            val (start, end) = when (period) {
                HomePlanPeriod.TODAY -> todayStart to (todayStart + DAY_IN_MILLIS)
                HomePlanPeriod.YESTERDAY -> (todayStart - DAY_IN_MILLIS) to todayStart
                HomePlanPeriod.LAST_7_DAYS -> (todayStart - (6L * DAY_IN_MILLIS)) to (todayStart + DAY_IN_MILLIS)
                HomePlanPeriod.THIS_MONTH -> monthStartUtc(now) to monthEndExclusiveUtc(now)
            }
            combine(
                transactionDao.getByCategoryInRangeFlow(category, start, end),
                billDao.getUnpaidCountByCategoryInRangeFlow(category, start, end)
            ) { transactions, unpaidBillsCount ->
                    MonthlyPlanStats(
                        totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                        budget = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        totalSaved = transactions.filter { it.type == TransactionType.SAVING }.sumOf { it.amount },
                        billsDue = unpaidBillsCount
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyPlanStats())

    fun onCategorySelected(categoryId: String) {
        _selectedCategory.value = categoryId
    }

    fun onPeriodSelected(period: HomePlanPeriod) {
        _selectedPeriod.value = period
    }

    companion object {
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
