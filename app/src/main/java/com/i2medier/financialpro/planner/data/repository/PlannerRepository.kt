package com.i2medier.financialpro.planner.data.repository

import com.i2medier.financialpro.planner.data.local.AccountDao
import com.i2medier.financialpro.planner.data.local.BillDao
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.GoalDao
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.StreakDao
import com.i2medier.financialpro.planner.data.local.StreakEntity
import com.i2medier.financialpro.planner.data.local.TransactionDao
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.monthEndExclusiveUtc
import com.i2medier.financialpro.planner.domain.monthStartUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class MonthlyStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSaved: Double = 0.0
)

class PlannerRepository(
    private val transactionDao: TransactionDao,
    private val goalDao: GoalDao,
    private val streakDao: StreakDao,
    private val billDao: BillDao,
    private val accountDao: AccountDao
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllFlow()

    fun observeGoals(): Flow<List<GoalEntity>> = goalDao.getAllFlow()

    fun observeStreak(): Flow<StreakEntity?> = streakDao.getFlow()

    fun observeBillsByCategory(category: String): Flow<List<BillEntity>> =
        billDao.getByCategoryFlow(category)

    fun observeMonthlyStats(): Flow<MonthlyStats> {
        return transactionDao.getAllFlow().map { transactions ->
            val now = System.currentTimeMillis()
            val start = monthStartUtc(now)
            val end = monthEndExclusiveUtc(now)
            val monthly = transactions.filter { it.date in start until end }
            MonthlyStats(
                totalIncome = monthly.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                totalExpense = monthly.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                totalSaved = monthly.filter { it.type == TransactionType.SAVING }.sumOf { it.amount }
            )
        }
    }

    fun observePlannedTotal(): Flow<Double> {
        return goalDao.getAllFlow().map { goals -> goals.sumOf { it.targetAmount } }
    }

    fun observeGoalProgress(): Flow<Map<Long, Double>> {
        return combine(transactionDao.getAllFlow(), goalDao.getAllFlow()) { tx, goals ->
            goals.associate { goal ->
                goal.id to tx
                    .asSequence()
                    .filter { it.goalId == goal.id && it.type == TransactionType.SAVING }
                    .sumOf { it.amount }
            }
        }
    }

    suspend fun addGoal(goal: GoalEntity) {
        goalDao.insert(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.update(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.delete(goal)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransactionsByGoal(goalId: Long) {
        transactionDao.deleteByGoalId(goalId)
    }

    suspend fun reassignTransactionsToGoal(oldGoalId: Long, newGoalId: Long) {
        transactionDao.reassignGoal(oldGoalId, newGoalId)
    }

    suspend fun addBill(bill: BillEntity) {
        billDao.insert(bill)
    }

    suspend fun updateBill(bill: BillEntity) {
        billDao.update(bill)
    }

    suspend fun deleteBill(bill: BillEntity) {
        billDao.delete(bill)
    }

    suspend fun setBillPaidAndCreateExpense(bill: BillEntity, paidAtMillis: Long) {
        if (!bill.isPaid) {
            billDao.update(bill.copy(isPaid = true))
        }
        transactionDao.insert(
            TransactionEntity(
                amount = bill.amount,
                type = TransactionType.EXPENSE,
                date = paidAtMillis.toUtcMidnight(),
                billId = bill.id,
                note = "Bill payment: ${bill.title}",
                category = bill.category
            )
        )
    }

    suspend fun updateSavingStreakForNow() {
        updateSavingStreak(System.currentTimeMillis())
    }

    suspend fun refreshSavingStreakFromTransactions() {
        updateSavingStreak(System.currentTimeMillis())
    }

    private suspend fun updateSavingStreak(nowMillis: Long) {
        val today = nowMillis.toUtcMidnight()
        val current = streakDao.get()
        if (current == null) {
            streakDao.upsert(StreakEntity(lastSaveDate = today, currentStreak = 1))
            return
        }
        val dayDiff = ((today - current.lastSaveDate) / DAY_IN_MILLIS).toInt()
        when {
            dayDiff <= 0 -> Unit
            dayDiff == 1 -> streakDao.upsert(current.copy(lastSaveDate = today, currentStreak = current.currentStreak + 1))
            else -> streakDao.upsert(current.copy(lastSaveDate = today, currentStreak = 1))
        }
    }

    private companion object {
        const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
