package com.i2medier.financialpro.planner.presentation

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
class TransactionHistoryAdapter(
    private val onDeleteClicked: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>() {
    private val items = mutableListOf<TransactionEntity>()
    fun submitList(items: List<TransactionEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_history, parent, false)
        return TransactionViewHolder(view)
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    override fun getItemCount(): Int = items.size
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeView: TextView = itemView.findViewById(R.id.tvTransactionType)
        private val noteView: TextView = itemView.findViewById(R.id.tvTransactionNote)
        private val amountView: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        private val dateView: TextView = itemView.findViewById(R.id.tvTransactionDate)
        private val deleteView: TextView = itemView.findViewById(R.id.btnDeleteTransaction)
        fun bind(item: TransactionEntity) {
            typeView.text = item.type.name.lowercase(Locale.getDefault())
                .replaceFirstChar { it.uppercase() }
            noteView.text = if (item.note.isNullOrBlank()) "-" else item.note
            amountView.text = formatAmount(item)
            amountView.setTextColor(amountColor(item.type))
            dateView.text = formatDate(item.date)
            deleteView.setOnClickListener { onDeleteClicked(item) }
        }
    private fun formatAmount(item: TransactionEntity): String {
        val base = NumberFormat.getCurrencyInstance().format(item.amount)
        return when (item.type) {
            TransactionType.INCOME -> "+$base"
            TransactionType.EXPENSE -> "-$base"
            TransactionType.SAVING -> base
    private fun amountColor(type: TransactionType): Int {
        return when (type) {
            TransactionType.INCOME -> Color.parseColor("#2E7D32")
            TransactionType.EXPENSE -> Color.parseColor("#C62828")
            TransactionType.SAVING -> Color.parseColor("#1565C0")
    private fun formatDate(millis: Long): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(millis))
}
