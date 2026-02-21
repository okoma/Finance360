package com.i2medier.financialpro.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerAddRequest
import com.i2medier.financialpro.planner.integration.PlannerIntegrationContract
import com.i2medier.financialpro.ui.CalculatorRegistry
import com.i2medier.financialpro.ui.search.RecentCalculatorStore
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone

class SearchActivity : AppCompatActivity() {

    private enum class ScopeFilter { ALL, CALCULATORS, PLANNER }
    private enum class PlannerFilter { ALL, GOALS, BILLS, TRANSACTIONS }

    private lateinit var etQuery: EditText
    private lateinit var chipAll: TextView
    private lateinit var chipCalculators: TextView
    private lateinit var chipPlanner: TextView
    private lateinit var plannerFilterRow: View
    private lateinit var chipPlannerAll: TextView
    private lateinit var chipGoals: TextView
    private lateinit var chipBills: TextView
    private lateinit var chipTransactions: TextView
    private lateinit var adapter: SearchAdapter

    private var scopeFilter = ScopeFilter.ALL
    private var plannerFilter = PlannerFilter.ALL
    private var calculators: List<CalculatorRegistry.Item> = emptyList()
    private var goals: List<GoalEntity> = emptyList()
    private var bills: List<BillEntity> = emptyList()
    private var transactions: List<TransactionEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        etQuery = findViewById(R.id.etSearch)
        chipAll = findViewById(R.id.chipScopeAll)
        chipCalculators = findViewById(R.id.chipScopeCalculators)
        chipPlanner = findViewById(R.id.chipScopePlanner)
        plannerFilterRow = findViewById(R.id.layoutPlannerSubFilters)
        chipPlannerAll = findViewById(R.id.chipPlannerAll)
        chipGoals = findViewById(R.id.chipPlannerGoals)
        chipBills = findViewById(R.id.chipPlannerBills)
        chipTransactions = findViewById(R.id.chipPlannerTransactions)
        val rv = findViewById<RecyclerView>(R.id.rvSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SearchAdapter(
            onActionClicked = { handleAction(it) },
            onCalculatorClicked = { item ->
                RecentCalculatorStore.record(this, item.activityClass.name)
                startActivity(Intent(this, item.activityClass))
            },
            onPlannerItemClicked = { plannerItem ->
                when (plannerItem) {
                    is SearchRow.PlannerItem.Goal -> {
                        startActivity(
                            Intent(this, GoalDetailActivity::class.java)
                                .putExtra(GoalDetailActivity.EXTRA_GOAL_ID, plannerItem.goal.id)
                        )
                    }

                    is SearchRow.PlannerItem.Bill -> {
                        startActivity(
                            Intent(this, BillDetailActivity::class.java)
                                .putExtra(BillDetailActivity.EXTRA_BILL_ID, plannerItem.bill.id)
                        )
                    }

                    is SearchRow.PlannerItem.Transaction -> {
                        startActivity(
                            PlannerIntegrationContract.createOpenPlannerDeepLinkIntent(
                                this,
                                PlannerIntegrationContract.TAB_HISTORY
                            )
                        )
                    }
                }
            }
        )
        rv.adapter = adapter

        bindFilters()
        etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshResults()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        CoroutineScope(Dispatchers.Main).launch {
            loadData()
            refreshResults()
        }
    }

    private fun bindFilters() {
        chipAll.setOnClickListener { scopeFilter = ScopeFilter.ALL; updateFilterUi(); refreshResults() }
        chipCalculators.setOnClickListener { scopeFilter = ScopeFilter.CALCULATORS; updateFilterUi(); refreshResults() }
        chipPlanner.setOnClickListener { scopeFilter = ScopeFilter.PLANNER; updateFilterUi(); refreshResults() }
        chipPlannerAll.setOnClickListener { plannerFilter = PlannerFilter.ALL; updateFilterUi(); refreshResults() }
        chipGoals.setOnClickListener { plannerFilter = PlannerFilter.GOALS; updateFilterUi(); refreshResults() }
        chipBills.setOnClickListener { plannerFilter = PlannerFilter.BILLS; updateFilterUi(); refreshResults() }
        chipTransactions.setOnClickListener { plannerFilter = PlannerFilter.TRANSACTIONS; updateFilterUi(); refreshResults() }
        updateFilterUi()
    }

    private fun updateFilterUi() {
        setChip(chipAll, scopeFilter == ScopeFilter.ALL)
        setChip(chipCalculators, scopeFilter == ScopeFilter.CALCULATORS)
        setChip(chipPlanner, scopeFilter == ScopeFilter.PLANNER)
        plannerFilterRow.visibility = if (scopeFilter == ScopeFilter.PLANNER || scopeFilter == ScopeFilter.ALL) View.VISIBLE else View.GONE
        setChip(chipPlannerAll, plannerFilter == PlannerFilter.ALL)
        setChip(chipGoals, plannerFilter == PlannerFilter.GOALS)
        setChip(chipBills, plannerFilter == PlannerFilter.BILLS)
        setChip(chipTransactions, plannerFilter == PlannerFilter.TRANSACTIONS)
    }

    private fun setChip(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_unselected)
        view.setTextColor(resources.getColor(if (selected) android.R.color.white else R.color.colorDark, theme))
    }

    private suspend fun loadData() {
        calculators = CalculatorRegistry.all(this)
        withContext(Dispatchers.IO) {
            val db = PlannerDatabase.getInstance(this@SearchActivity)
            goals = db.goalDao().getAll()
            bills = db.billDao().getAll()
            transactions = db.transactionDao().getAll()
        }
    }

    private fun refreshResults() {
        val query = etQuery.text.toString().trim()
        if (query.isBlank()) {
            adapter.submit(buildQuickRows())
            return
        }

        val rows = mutableListOf<SearchRow>()
        if (scopeFilter != ScopeFilter.PLANNER) {
            val calcMatches = calculators.filter { it.title.contains(query, true) || it.subtitle.contains(query, true) }
            if (calcMatches.isNotEmpty()) {
                rows += SearchRow.Header(getString(R.string.search_section_calculators))
                rows += calcMatches.map { SearchRow.CalculatorItem(it) }
            }
        }

        if (scopeFilter != ScopeFilter.CALCULATORS) {
            if (plannerFilter == PlannerFilter.ALL || plannerFilter == PlannerFilter.GOALS) {
                val goalMatches = goals.filter { it.title.contains(query, true) || (it.description?.contains(query, true) == true) }
                if (goalMatches.isNotEmpty()) {
                    rows += SearchRow.Header(getString(R.string.search_section_goals))
                    rows += goalMatches.map { SearchRow.PlannerItem.Goal(it) }
                }
            }
            if (plannerFilter == PlannerFilter.ALL || plannerFilter == PlannerFilter.BILLS) {
                val billMatches = bills.filter { it.title.contains(query, true) }
                if (billMatches.isNotEmpty()) {
                    rows += SearchRow.Header(getString(R.string.search_section_bills))
                    rows += billMatches.map { SearchRow.PlannerItem.Bill(it) }
                }
            }
            if (plannerFilter == PlannerFilter.ALL || plannerFilter == PlannerFilter.TRANSACTIONS) {
                val txMatches = transactions.filter {
                    (it.note?.contains(query, true) == true) ||
                        it.type.name.contains(query, true)
                }
                if (txMatches.isNotEmpty()) {
                    rows += SearchRow.Header(getString(R.string.search_section_transactions))
                    rows += txMatches.map { SearchRow.PlannerItem.Transaction(it) }
                }
            }
        }

        if (rows.isEmpty()) {
            rows += SearchRow.Header(getString(R.string.search_no_results))
        }
        adapter.submit(rows)
    }

    private fun buildQuickRows(): List<SearchRow> {
        val rows = mutableListOf<SearchRow>()
        rows += SearchRow.Header(getString(R.string.search_quick_actions))
        rows += SearchRow.ActionItem.AddSaving
        rows += SearchRow.ActionItem.OpenPlanner
        rows += SearchRow.ActionItem.CreateGoal

        val recentClassNames = RecentCalculatorStore.get(this)
        val calculatorByClass = calculators.associateBy { it.activityClass.name }
        val recent = recentClassNames.mapNotNull { calculatorByClass[it] }.ifEmpty { CalculatorRegistry.featured(this) }
        if (recent.isNotEmpty()) {
            rows += SearchRow.Header(getString(R.string.search_recent_calculators))
            rows += recent.map { SearchRow.CalculatorItem(it) }
        }
        return rows
    }

    private fun handleAction(action: SearchRow.ActionItem) {
        when (action) {
            SearchRow.ActionItem.AddSaving -> {
                val intent = PlannerIntegrationContract.createOpenPlannerIntent(
                    this,
                    PlannerAddRequest(
                        amount = 500.0,
                        suggestedType = TransactionType.SAVING,
                        note = getString(R.string.planner_notification_default_saving_note),
                        title = getString(R.string.planner_action_add_saving),
                        calculatorCategory = CalculatorRegistry.CATEGORY_SAVINGS
                    )
                )
                startActivity(intent)
            }

            SearchRow.ActionItem.OpenPlanner -> {
                startActivity(PlannerIntegrationContract.createOpenPlannerDeepLinkIntent(this))
            }

            SearchRow.ActionItem.CreateGoal -> {
                startActivity(
                    PlannerIntegrationContract.createOpenPlannerDeepLinkIntent(
                        this,
                        PlannerIntegrationContract.TAB_GOALS
                    )
                )
            }
        }
    }
}

private sealed class SearchRow {
    data class Header(val title: String) : SearchRow()
    data class CalculatorItem(val item: CalculatorRegistry.Item) : SearchRow()

    sealed class PlannerItem : SearchRow() {
        data class Goal(val goal: GoalEntity) : PlannerItem()
        data class Bill(val bill: BillEntity) : PlannerItem()
        data class Transaction(val transaction: TransactionEntity) : PlannerItem()
    }

    sealed class ActionItem : SearchRow() {
        data object AddSaving : ActionItem()
        data object OpenPlanner : ActionItem()
        data object CreateGoal : ActionItem()
    }
}

private class SearchAdapter(
    private val onActionClicked: (SearchRow.ActionItem) -> Unit,
    private val onCalculatorClicked: (CalculatorRegistry.Item) -> Unit,
    private val onPlannerItemClicked: (SearchRow.PlannerItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<SearchRow>()

    fun submit(newRows: List<SearchRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is SearchRow.Header -> 0
        is SearchRow.CalculatorItem -> 1
        is SearchRow.PlannerItem -> 2
        is SearchRow.ActionItem -> 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderVh(inflater.inflate(R.layout.item_search_header, parent, false))
            else -> ItemVh(inflater.inflate(R.layout.item_search_row, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        if (holder is HeaderVh && row is SearchRow.Header) {
            holder.title.text = row.title
            return
        }
        if (holder is ItemVh) {
            when (row) {
                is SearchRow.CalculatorItem -> {
                    holder.title.text = row.item.title
                    holder.subtitle.text = row.item.subtitle
                    holder.itemView.setOnClickListener { onCalculatorClicked(row.item) }
                }

                is SearchRow.ActionItem -> {
                    holder.title.text = when (row) {
                        SearchRow.ActionItem.AddSaving -> holder.itemView.context.getString(R.string.search_action_add_saving)
                        SearchRow.ActionItem.OpenPlanner -> holder.itemView.context.getString(R.string.search_action_open_planner)
                        SearchRow.ActionItem.CreateGoal -> holder.itemView.context.getString(R.string.search_action_create_goal)
                    }
                    holder.subtitle.text = when (row) {
                        SearchRow.ActionItem.AddSaving -> holder.itemView.context.getString(R.string.search_action_add_saving_subtitle)
                        SearchRow.ActionItem.OpenPlanner -> holder.itemView.context.getString(R.string.search_action_open_planner_subtitle)
                        SearchRow.ActionItem.CreateGoal -> holder.itemView.context.getString(R.string.search_action_create_goal_subtitle)
                    }
                    holder.itemView.setOnClickListener { onActionClicked(row) }
                }

                is SearchRow.PlannerItem.Goal -> {
                    holder.title.text = row.goal.title
                    holder.subtitle.text = row.goal.description ?: holder.itemView.context.getString(R.string.search_section_goals)
                    holder.itemView.setOnClickListener { onPlannerItemClicked(row) }
                }

                is SearchRow.PlannerItem.Bill -> {
                    holder.title.text = row.bill.title
                    holder.subtitle.text = if (row.bill.isPaid) {
                        holder.itemView.context.getString(R.string.planner_paid)
                    } else {
                        val df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                        df.timeZone = TimeZone.getTimeZone("UTC")
                        holder.itemView.context.getString(R.string.planner_bill_due_label, df.format(row.bill.dueDate))
                    }
                    holder.itemView.setOnClickListener { onPlannerItemClicked(row) }
                }

                is SearchRow.PlannerItem.Transaction -> {
                    holder.title.text = row.transaction.note ?: row.transaction.type.name
                    holder.subtitle.text = CurrencyManager.format(holder.itemView.context, row.transaction.amount)
                    holder.itemView.setOnClickListener { onPlannerItemClicked(row) }
                }

                else -> Unit
            }
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderVh(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSearchHeader)
    }

    private class ItemVh(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvSearchTitle)
        val subtitle: TextView = view.findViewById(R.id.tvSearchSubtitle)
    }
}
