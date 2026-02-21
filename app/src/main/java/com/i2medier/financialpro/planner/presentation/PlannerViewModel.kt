package com.i2medier.financialpro.planner.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.i2medier.financialpro.planner.data.local.AccountEntity
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.BillRepeat
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.StreakEntity
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.data.repository.MonthlyStats
import com.i2medier.financialpro.planner.data.repository.PlannerRepository
import com.i2medier.financialpro.planner.domain.BillCategoryUi
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.domain.validateGoalInput
import com.i2medier.financialpro.planner.domain.validateTransactionInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(
    application: Application,
    private val repository: PlannerRepository
) : AndroidViewModel(application) {

    val transactions: StateFlow<List<TransactionEntity>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.observeGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val streak: StateFlow<StreakEntity?> = repository.observeStreak()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val monthlyStats: StateFlow<MonthlyStats> = repository.observeMonthlyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyStats())

    val plannedTotal: StateFlow<Double> = repository.observePlannedTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val goalProgress: StateFlow<Map<Long, Double>> = repository.observeGoalProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val bills: StateFlow<List<BillEntity>> = repository.observeBillsByCategory("all")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addGoal(
        title: String,
        targetAmount: Double?,
        description: String? = null,
        targetDate: Long? = null,
        category: String = "other"
    ): String? {
        val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val validation = validateGoalInput(title.trim(), targetAmount, normalizedDescription)
        if (!validation.isValid) return validation.error

        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    title = title.trim(),
                    description = normalizedDescription,
                    targetAmount = targetAmount ?: 0.0,
                    targetDate = targetDate,
                    category = GoalCategoryUi.normalize(category),
                    createdAt = System.currentTimeMillis().toUtcMidnight()
                )
            )
        }
        return null
    }

    fun updateGoal(
        goal: GoalEntity,
        title: String,
        targetAmount: Double?,
        description: String? = null,
        targetDate: Long? = null,
        category: String = "other"
    ): String? {
        val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val validation = validateGoalInput(title.trim(), targetAmount, normalizedDescription)
        if (!validation.isValid) return validation.error

        viewModelScope.launch {
            repository.updateGoal(
                goal.copy(
                    title = title.trim(),
                    description = normalizedDescription,
                    targetAmount = targetAmount ?: 0.0,
                    targetDate = targetDate,
                    category = GoalCategoryUi.normalize(category)
                )
            )
        }
        return null
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }

    fun deleteGoalAndTransactions(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteTransactionsByGoal(goal.id)
            repository.deleteGoal(goal)
        }
    }

    fun reassignGoalAndDelete(oldGoal: GoalEntity, newGoal: GoalEntity) {
        viewModelScope.launch {
            repository.reassignTransactionsToGoal(oldGoal.id, newGoal.id)
            repository.deleteGoal(oldGoal)
        }
    }

    fun addTransaction(
        amount: Double?,
        type: TransactionType,
        dateMillis: Long = System.currentTimeMillis(),
        goalId: Long? = null,
        note: String? = null,
        category: String = "all"
    ): String? {
        val validation = validateTransactionInput(amount, type, note, dateMillis)
        if (!validation.isValid) return validation.error

        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    amount = amount ?: 0.0,
                    type = type,
                    date = dateMillis.toUtcMidnight(),
                    goalId = goalId,
                    accountId = when (type) {
                        TransactionType.SAVING -> AccountEntity.DEFAULT_SAVINGS_ID
                        TransactionType.INCOME -> AccountEntity.DEFAULT_CASH_ID
                        TransactionType.EXPENSE -> AccountEntity.DEFAULT_CASH_ID
                    },
                    note = note,
                    category = category
                )
            )
            if (type == TransactionType.SAVING) {
                repository.refreshSavingStreakFromTransactions()
            }
        }
        return null
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    fun addBill(
        title: String,
        amount: Double?,
        dueDateMillis: Long,
        repeat: String? = null,
        category: String = "other"
    ): String? {
        if (title.isBlank()) return "Bill title is required."
        if (title.length > 60) return "Bill title is too long."
        if (amount == null || amount <= 0.0) return "Bill amount must be greater than 0."

        val normalizedRepeat = BillRepeat.normalize(repeat)
        viewModelScope.launch {
            repository.addBill(
                BillEntity(
                    title = title.trim(),
                    amount = amount,
                    dueDate = dueDateMillis.toUtcMidnight(),
                    repeat = normalizedRepeat,
                    category = BillCategoryUi.normalize(category),
                    createdAt = System.currentTimeMillis().toUtcMidnight()
                )
            )
        }
        return null
    }

    fun updateBill(
        bill: BillEntity,
        title: String,
        amount: Double?,
        dueDateMillis: Long,
        repeat: String? = null,
        category: String = "other"
    ): String? {
        if (title.isBlank()) return "Bill title is required."
        if (title.length > 60) return "Bill title is too long."
        if (amount == null || amount <= 0.0) return "Bill amount must be greater than 0."

        viewModelScope.launch {
            repository.updateBill(
                bill.copy(
                    title = title.trim(),
                    amount = amount,
                    dueDate = dueDateMillis.toUtcMidnight(),
                    repeat = BillRepeat.normalize(repeat),
                    category = BillCategoryUi.normalize(category)
                )
            )
        }
        return null
    }

    fun markBillPaid(bill: BillEntity, paidAtMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch { repository.setBillPaidAndCreateExpense(bill, paidAtMillis) }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
                val database = PlannerDatabase.getInstance(application)
                val repository = PlannerRepository(
                    transactionDao = database.transactionDao(),
                    goalDao = database.goalDao(),
                    streakDao = database.streakDao(),
                    billDao = database.billDao(),
                    accountDao = database.accountDao()
                )
                return PlannerViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
