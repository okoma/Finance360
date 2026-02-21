package com.i2medier.financialpro.planner.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionHistoryAdapter(
    private val onDeleteClicked: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionHistoryAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_history, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.tvTransactionType)
        private val amount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        private val date: TextView = itemView.findViewById(R.id.tvTransactionDate)
        private val note: TextView = itemView.findViewById(R.id.tvTransactionNote)
        private val delete: View = itemView.findViewById(R.id.btnDeleteTransaction)

        fun bind(item: TransactionEntity) {
            typeText.text = when (item.type) {
                TransactionType.INCOME -> "Income"
                TransactionType.EXPENSE -> "Expense"
                TransactionType.SAVING -> "Saving"
            }
            amount.text = CurrencyManager.format(itemView.context, item.amount)
            date.text = DATE_FORMAT.format(Date(item.date))
            note.text = item.note.orEmpty()
            delete.setOnClickListener { onDeleteClicked(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean = oldItem == newItem
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }
}
