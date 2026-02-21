package com.i2medier.financialpro.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.AccountEntity
import com.i2medier.financialpro.planner.data.local.AccountType
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.monthEndExclusiveUtc
import com.i2medier.financialpro.planner.domain.monthStartUtc
import com.i2medier.financialpro.ui.widgets.NetWorthLineChartView
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NetWorthOverviewActivity : AppCompatActivity() {

    private lateinit var lineChart: NetWorthLineChartView
    private var selectedMonthOffset: Int = 0
    private var allTransactions: List<TransactionEntity> = emptyList()
    private var allBills: List<BillEntity> = emptyList()
    private var allAccounts: List<AccountEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth_overview)

        lineChart = findViewById(R.id.lineChart)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPrevMonth).setOnClickListener {
            selectedMonthOffset = (selectedMonthOffset - 1).coerceAtLeast(-60)
            render()
        }
        findViewById<View>(R.id.btnNextMonth).setOnClickListener {
            selectedMonthOffset = (selectedMonthOffset + 1).coerceAtMost(0)
            render()
        }
        findViewById<View>(R.id.btnAddAsset).setOnClickListener {
            startActivity(
                android.content.Intent(this, AccountManagerActivity::class.java)
                    .putExtra(AccountManagerActivity.EXTRA_DEFAULT_TYPE, "ASSET")
            )
        }
        findViewById<View>(R.id.btnAddDebt).setOnClickListener {
            startActivity(
                android.content.Intent(this, AccountManagerActivity::class.java)
                    .putExtra(AccountManagerActivity.EXTRA_DEFAULT_TYPE, "LIABILITY")
            )
        }
        findViewById<View>(R.id.btnViewAll).setOnClickListener {
            startActivity(
                com.i2medier.financialpro.planner.integration.PlannerIntegrationContract
                    .createOpenPlannerDeepLinkIntent(this, com.i2medier.financialpro.planner.integration.PlannerIntegrationContract.TAB_HISTORY)
            )
            finish()
        }

        val db = PlannerDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                combine(
                    db.transactionDao().getAllFlow(),
                    db.billDao().getByCategoryFlow("all"),
                    db.accountDao().getAllFlow()
                ) { tx, bills, accounts -> Triple(tx, bills, accounts) }.collect { (tx, bills, accounts) ->
                    allTransactions = tx
                    allBills = bills
                    allAccounts = accounts
                    render()
                }
            }
        }
    }

    private fun render() {
        val snapshot = monthSnapshot(selectedMonthOffset)
        val previous = monthSnapshot(selectedMonthOffset - 1)
        val change = snapshot.netWorth - previous.netWorth
        val positive = snapshot.netWorth >= 0.0
        val monthText = monthLabel(selectedMonthOffset)

        findViewById<TextView>(R.id.tvCurrentMonth).text = monthText
        findViewById<TextView>(R.id.tvNetWorth).text = formatMoney(snapshot.netWorth)
        findViewById<TextView>(R.id.tvTotalAssets).text = formatMoney(snapshot.assets)
        findViewById<TextView>(R.id.tvTotalDebts).text = formatMoney(snapshot.debts)
        bindBreakdownRows(
            container = findViewById(R.id.layoutAssetRows),
            rows = snapshot.assetRows
        )
        bindBreakdownRows(
            container = findViewById(R.id.layoutLiabilityRows),
            rows = snapshot.liabilityRows
        )

        val changeIcon = findViewById<ImageView>(R.id.ivChangeIndicator)
        val changeText = findViewById<TextView>(R.id.tvChange)
        val floatingChangeAmount = findViewById<TextView>(R.id.tvFloatingChangeAmount)
        val floatingChangeIcon = findViewById<ImageView>(R.id.ivFloatingChangeIcon)
        if (change >= 0.0) {
            changeIcon.setImageResource(R.drawable.nw_ic_trending_up)
            changeIcon.setColorFilter(Color.parseColor("#10B981"))
            changeText.text = "Up ${formatMoney(change)} this month"
            floatingChangeAmount.text = "+${formatMoney(change)}"
            floatingChangeAmount.setTextColor(Color.parseColor("#10B981"))
            floatingChangeIcon.setImageResource(R.drawable.nw_ic_trending_up)
            floatingChangeIcon.setColorFilter(Color.parseColor("#10B981"))
        } else {
            changeIcon.setImageResource(R.drawable.nw_ic_trending_down)
            changeIcon.setColorFilter(Color.parseColor("#EF4444"))
            changeText.text = "Down ${formatMoney(kotlin.math.abs(change))} this month"
            floatingChangeAmount.text = "-${formatMoney(kotlin.math.abs(change))}"
            floatingChangeAmount.setTextColor(Color.parseColor("#EF4444"))
            floatingChangeIcon.setImageResource(R.drawable.nw_ic_trending_down)
            floatingChangeIcon.setColorFilter(Color.parseColor("#EF4444"))
        }
        val comparisonLabel = findViewById<TextView>(R.id.tvComparisonLabel)
        val comparisonAmount = findViewById<TextView>(R.id.tvComparisonAmount)
        val comparisonPct = findViewById<TextView>(R.id.tvComparisonPercentage)
        comparisonLabel.text = "vs ${monthLabel(selectedMonthOffset - 1)}"
        val signedAmount = when {
            change > 0.0 -> "+${formatMoney(change)}"
            change < 0.0 -> "-${formatMoney(kotlin.math.abs(change))}"
            else -> formatMoney(0.0)
        }
        comparisonAmount.text = signedAmount
        val percentage = if (previous.netWorth != 0.0) {
            (change / kotlin.math.abs(previous.netWorth)) * 100.0
        } else {
            0.0
        }
        val isPositiveChange = change > 0.0
        val isNoChange = change == 0.0
        comparisonAmount.setTextColor(
            Color.parseColor(
                when {
                    isNoChange -> "#64748B"
                    isPositiveChange -> "#10B981"
                    else -> "#EF4444"
                }
            )
        )
        comparisonPct.text = when {
            isNoChange -> "\u2192 0.0%"
            isPositiveChange -> "\u2191 ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(percentage))}%"
            else -> "\u2193 ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(percentage))}%"
        }
        comparisonPct.setTextColor(
            Color.parseColor(
                when {
                    isNoChange -> "#64748B"
                    isPositiveChange -> "#10B981"
                    else -> "#EF4444"
                }
            )
        )
        comparisonPct.setBackgroundResource(
            if (isNoChange || isPositiveChange) R.drawable.bg_percentage_positive
            else R.drawable.bg_percentage_negative
        )

        val netWorthView = findViewById<TextView>(R.id.tvNetWorth)
        val messageCard = findViewById<CardView>(R.id.messageCard)
        val messageText = findViewById<TextView>(R.id.tvMotivationalMessage)
        if (positive) {
            netWorthView.setTextColor(Color.parseColor("#065F46"))
            messageCard.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
            messageText.setTextColor(Color.parseColor("#065F46"))
            messageText.text = "Your Net Worth is positive! Keep up the great work! \uD83C\uDFC6"
        } else {
            netWorthView.setTextColor(Color.parseColor("#991B1B"))
            messageCard.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            messageText.setTextColor(Color.parseColor("#991B1B"))
            messageText.text = "Focus on reducing debts to increase your net worth. You can do it! \uD83D\uDCAA"
        }

        lineChart.setTrendPositive(positive)

        updateChart()
        updateTransactions()
    }

    private fun updateChart() {
        val points = (5 downTo 0).map { rel ->
            val offset = selectedMonthOffset - rel
            val label = SimpleDateFormat("MMM", Locale.getDefault()).format(monthStart(offset))
            val value = monthSnapshot(offset).netWorth.toFloat()
            NetWorthLineChartView.DataPoint(label, value)
        }
        lineChart.setData(points)
    }

    private fun updateTransactions() {
        val start = monthStart(selectedMonthOffset)
        val end = monthEndExclusiveUtc(start)
        val items = allTransactions
            .asSequence()
            .filter { it.date >= start && it.date < end }
            .sortedByDescending { it.date }
            .take(2)
            .toList()

        val emptyView = findViewById<TextView>(R.id.tvTransactionsEmpty)
        val card1 = findViewById<CardView>(R.id.cardTxn1)
        val card2 = findViewById<CardView>(R.id.cardTxn2)

        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            card1.visibility = View.GONE
            card2.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        card1.visibility = View.VISIBLE
        bindTransactionRow(
            row = 1,
            transaction = items[0]
        )

        val second = items.getOrNull(1)
        if (second != null) {
            card2.visibility = View.VISIBLE
            bindTransactionRow(
                row = 2,
                transaction = second
            )
        } else {
            card2.visibility = View.GONE
        }
    }

    private fun bindTransactionRow(
        row: Int,
        transaction: TransactionEntity
    ) {
        val dateView = findViewById<TextView>(if (row == 1) R.id.tvTxnDate1 else R.id.tvTxnDate2)
        val iconView = findViewById<ImageView>(if (row == 1) R.id.ivTxnIcon1 else R.id.ivTxnIcon2)
        val nameView = findViewById<TextView>(if (row == 1) R.id.tvTxnName1 else R.id.tvTxnName2)
        val amountView = findViewById<TextView>(if (row == 1) R.id.tvTxnAmount1 else R.id.tvTxnAmount2)
        val noteView = findViewById<TextView>(if (row == 1) R.id.tvTxnNote1 else R.id.tvTxnNote2)

        val dateFmt = SimpleDateFormat("MMM dd", Locale.getDefault())
        dateView.text = dateFmt.format(java.util.Date(transaction.date))
        amountView.text = formatMoney(transaction.amount)
        noteView.text = transaction.note?.takeIf { it.isNotBlank() } ?: "Transaction"
        when (transaction.type) {
            TransactionType.SAVING -> {
                iconView.setImageResource(R.drawable.nw_ic_savings_green)
                nameView.text = "Savings"
            }
            TransactionType.EXPENSE -> {
                iconView.setImageResource(R.drawable.nw_ic_loan_red)
                nameView.text = "Expense"
            }
            TransactionType.INCOME -> {
                iconView.setImageResource(R.drawable.nw_ic_cash)
                nameView.text = "Income"
            }
        }
    }

    private data class Snapshot(
        val assets: Double,
        val debts: Double,
        val assetRows: List<BreakdownRow>,
        val liabilityRows: List<BreakdownRow>
    ) {
        val netWorth: Double = assets - debts
    }

    private data class BreakdownRow(
        val label: String,
        val amount: Double,
        val iconRes: Int
    )

    private fun monthSnapshot(offset: Int): Snapshot {
        val start = monthStart(offset)
        val monthEnd = monthEndExclusiveUtc(start)
        val snapshotCutoff = if (offset == 0) {
            System.currentTimeMillis().coerceAtMost(monthEnd - 1) + 1
        } else {
            monthEnd
        }

        val accountsById = allAccounts.associateBy { it.id }
        val balances = allAccounts.associate { it.id to it.openingBalance }.toMutableMap()

        allTransactions
            .asSequence()
            .filter { it.date < snapshotCutoff }
            .forEach { tx ->
                val account = accountsById[tx.accountId] ?: return@forEach
                val signedAmount = when (account.type) {
                    AccountType.ASSET -> when (tx.type) {
                        TransactionType.INCOME, TransactionType.SAVING -> tx.amount
                        TransactionType.EXPENSE -> -tx.amount
                    }
                    AccountType.LIABILITY -> when (tx.type) {
                        TransactionType.EXPENSE -> tx.amount
                        TransactionType.INCOME, TransactionType.SAVING -> -tx.amount
                    }
                }
                balances[account.id] = (balances[account.id] ?: 0.0) + signedAmount
            }

        val assetBalances = allAccounts
            .filter { it.type == AccountType.ASSET }
            .associate { it.id to (balances[it.id] ?: 0.0) }
        val liabilityBalances = allAccounts
            .filter { it.type == AccountType.LIABILITY }
            .associate { it.id to (balances[it.id] ?: 0.0) }

        val assetRows = allAccounts
            .filter { it.type == AccountType.ASSET }
            .map { account ->
                BreakdownRow(
                    label = account.name,
                    amount = assetBalances[account.id] ?: 0.0,
                    iconRes = assetIconForName(account.name)
                )
            }
            .filter { it.amount != 0.0 || it.label.equals("cash", true) || it.label.equals("savings", true) || it.label.equals("investments", true) }

        val unpaidBills = allBills.filter { !it.isPaid && it.dueDate < snapshotCutoff }
        val unpaidLoanBills = unpaidBills.filterNot {
            val t = it.title.lowercase(Locale.getDefault())
            "card" in t || "credit" in t
        }.sumOf { it.amount }
        val unpaidCardBills = (unpaidBills.sumOf { it.amount } - unpaidLoanBills).coerceAtLeast(0.0)

        val loansBase = liabilityBalances[AccountEntity.DEFAULT_LOANS_ID] ?: 0.0
        val cardsBase = liabilityBalances[AccountEntity.DEFAULT_CREDIT_CARD_ID] ?: 0.0
        val otherLiabilities = liabilityBalances
            .filterKeys {
                it != AccountEntity.DEFAULT_LOANS_ID &&
                    it != AccountEntity.DEFAULT_CREDIT_CARD_ID
            }
            .values
            .sum()
        val loans = loansBase + unpaidLoanBills
        val creditCards = cardsBase + unpaidCardBills
        val liabilityTotals = liabilityBalances.toMutableMap()
        liabilityTotals[AccountEntity.DEFAULT_LOANS_ID] = loans
        liabilityTotals[AccountEntity.DEFAULT_CREDIT_CARD_ID] = creditCards

        val liabilityRows = allAccounts
            .filter { it.type == AccountType.LIABILITY }
            .map { account ->
                BreakdownRow(
                    label = account.name,
                    amount = liabilityTotals[account.id] ?: 0.0,
                    iconRes = liabilityIconForName(account.name)
                )
            }
            .filter { it.amount != 0.0 || it.label.equals("loans", true) || it.label.contains("card", true) }
        val assets = assetBalances.values.sum()
        val debts = loans + creditCards + otherLiabilities
        return Snapshot(
            assets = assets,
            debts = debts,
            assetRows = assetRows,
            liabilityRows = liabilityRows
        )
    }

    private fun bindBreakdownRows(container: LinearLayout, rows: List<BreakdownRow>) {
        container.removeAllViews()
        rows.forEachIndexed { index, row ->
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                if (index > 0) {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(10) }
                } else {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
            val icon = ImageView(this).apply {
                setImageResource(row.iconRes)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            }
            val name = TextView(this).apply {
                text = row.label
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(8)
                }
            }
            val amount = TextView(this).apply {
                text = formatMoney(row.amount)
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            rowView.addView(icon)
            rowView.addView(name)
            rowView.addView(amount)
            container.addView(rowView)
        }
    }

    private fun assetIconForName(name: String): Int {
        val lower = name.lowercase(Locale.getDefault())
        return when {
            "cash" in lower -> R.drawable.nw_ic_cash
            "saving" in lower -> R.drawable.nw_ic_savings_green
            "invest" in lower -> R.drawable.nw_ic_investments
            else -> R.drawable.nw_ic_wallet_green
        }
    }

    private fun liabilityIconForName(name: String): Int {
        val lower = name.lowercase(Locale.getDefault())
        return when {
            "loan" in lower || "debt" in lower -> R.drawable.nw_ic_loan
            "card" in lower || "credit" in lower -> R.drawable.nw_ic_credit_card_small
            else -> R.drawable.nw_ic_credit_card_orange
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun monthStart(offset: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, offset)
        }
        return monthStartUtc(cal.timeInMillis)
    }

    private fun monthLabel(offset: Int): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthStart(offset))
    }

    private fun formatMoney(value: Double): String = CurrencyManager.format(this, value)
}
