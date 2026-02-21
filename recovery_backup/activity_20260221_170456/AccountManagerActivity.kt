package com.i2medier.financialpro.activity

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.AccountEntity
import com.i2medier.financialpro.planner.data.local.AccountType
import com.i2medier.financialpro.planner.data.local.PlannerDatabase
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AccountManagerActivity : AppCompatActivity() {

    private lateinit var adapter: AccountAdapter
    private var allTransactions: List<TransactionEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_manager)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        val rvAccounts = findViewById<RecyclerView>(R.id.rvAccounts)
        rvAccounts.layoutManager = LinearLayoutManager(this)

        val db = PlannerDatabase.getInstance(applicationContext)
        adapter = AccountAdapter(
            onEdit = { showAccountDialog(existing = it) },
            onDelete = { account ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val hasUsage = db.transactionDao().countByAccount(account.id) > 0
                    if (account.isDefault) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AccountManagerActivity, "Default accounts cannot be deleted", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    if (hasUsage) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AccountManagerActivity, "Account has transactions and cannot be deleted", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    db.accountDao().delete(account)
                }
            }
        )
        rvAccounts.adapter = adapter

        findViewById<Button>(R.id.btnAddAccount).setOnClickListener {
            val preferredType = intent?.getStringExtra(EXTRA_DEFAULT_TYPE)
            showAccountDialog(existing = null, defaultType = preferredType)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                combine(
                    db.accountDao().getAllFlow(),
                    db.transactionDao().getAllFlow()
                ) { accounts, transactions ->
                    accounts to transactions
                }.collect { (accounts, transactions) ->
                    allTransactions = transactions
                    val balances = computeBalances(accounts, transactions)
                    adapter.submit(
                        accounts.map { account ->
                            AccountRow(
                                entity = account,
                                currentBalance = balances[account.id] ?: account.openingBalance
                            )
                        }
                    )
                }
            }
        }
    }

    private fun showAccountDialog(existing: AccountEntity?, defaultType: String? = null) {
        val db = PlannerDatabase.getInstance(applicationContext)

        val root = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val nameInput = EditText(this).apply {
            hint = "Account name"
            setText(existing?.name.orEmpty())
        }
        container.addView(nameInput)

        val typeSpinner = Spinner(this)
        val types = listOf("ASSET", "LIABILITY")
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        val chosenType = existing?.type?.name ?: defaultType ?: "ASSET"
        typeSpinner.setSelection(if (chosenType == "LIABILITY") 1 else 0)
        container.addView(typeSpinner)

        val openingInput = EditText(this).apply {
            hint = "Opening balance"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(existing?.openingBalance?.toString() ?: "0")
        }
        container.addView(openingInput)

        if (existing?.isDefault == true) {
            typeSpinner.isEnabled = false
            nameInput.isEnabled = false
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Account" else "Edit Account")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val opening = openingInput.text?.toString()?.trim()?.toDoubleOrNull()
                if (name.isBlank() && existing == null) {
                    Toast.makeText(this, "Account name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (opening == null) {
                    Toast.makeText(this, "Invalid opening balance", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedType = if (typeSpinner.selectedItemPosition == 1) AccountType.LIABILITY else AccountType.ASSET
                lifecycleScope.launch(Dispatchers.IO) {
                    val entity = if (existing == null) {
                        AccountEntity(
                            name = name,
                            type = selectedType,
                            openingBalance = opening,
                            isDefault = false,
                            currency = ""
                        )
                    } else {
                        existing.copy(
                            name = if (existing.isDefault) existing.name else name,
                            type = if (existing.isDefault) existing.type else selectedType,
                            openingBalance = opening
                        )
                    }
                    db.accountDao().insert(entity)
                }
            }
            .show()
    }

    private fun computeBalances(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Map<Long, Double> {
        val byId = accounts.associateBy { it.id }
        val balances = accounts.associate { it.id to it.openingBalance }.toMutableMap()

        transactions.forEach { tx ->
            val account = byId[tx.accountId] ?: return@forEach
            val signed = when (account.type) {
                AccountType.ASSET -> when (tx.type) {
                    TransactionType.INCOME, TransactionType.SAVING -> tx.amount
                    TransactionType.EXPENSE -> -tx.amount
                }
                AccountType.LIABILITY -> when (tx.type) {
                    TransactionType.EXPENSE -> tx.amount
                    TransactionType.INCOME, TransactionType.SAVING -> -tx.amount
                }
            }
            balances[account.id] = (balances[account.id] ?: 0.0) + signed
        }
        return balances
    }

    private data class AccountRow(
        val entity: AccountEntity,
        val currentBalance: Double
    )

    private inner class AccountAdapter(
        private val onEdit: (AccountEntity) -> Unit,
        private val onDelete: (AccountEntity) -> Unit
    ) : RecyclerView.Adapter<AccountAdapter.VH>() {

        private val items = mutableListOf<AccountRow>()

        fun submit(list: List<AccountRow>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = layoutInflater.inflate(R.layout.item_account_manager, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            fun bind(row: AccountRow) {
                val account = row.entity
                itemView.findViewById<TextView>(R.id.tvName).text = account.name
                itemView.findViewById<TextView>(R.id.tvType).text = account.type.name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
                itemView.findViewById<TextView>(R.id.tvCurrentBalance).text = "Current: ${CurrencyManager.format(this@AccountManagerActivity, row.currentBalance)}"
                itemView.findViewById<TextView>(R.id.tvOpeningBalance).text = "Opening: ${CurrencyManager.format(this@AccountManagerActivity, account.openingBalance)}"
                itemView.findViewById<Button>(R.id.btnEdit).setOnClickListener { onEdit(account) }
                itemView.findViewById<Button>(R.id.btnDelete).apply {
                    isEnabled = !account.isDefault
                    alpha = if (account.isDefault) 0.45f else 1f
                    setOnClickListener { onDelete(account) }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEFAULT_TYPE = "default_type"
    }
}
