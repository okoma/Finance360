package com.i2medier.financialpro.planner.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillAdapter(
    private val onBillClicked: (BillEntity) -> Unit,
    private val onMarkPaid: (BillEntity) -> Unit,
    private val onEdit: (BillEntity) -> Unit,
    private val onDelete: (BillEntity) -> Unit
) : ListAdapter<BillEntity, BillAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bill, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvBillTitle)
        private val amount: TextView = itemView.findViewById(R.id.tvBillAmount)
        private val dueDate: TextView = itemView.findViewById(R.id.tvBillDueDate)
        private val status: TextView = itemView.findViewById(R.id.tvBillStatus)
        private val btnMarkPaid: TextView = itemView.findViewById(R.id.btnMarkPaid)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditBill)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteBill)

        fun bind(bill: BillEntity) {
            title.text = bill.title
            amount.text = CurrencyManager.format(itemView.context, bill.amount)
            dueDate.text = DATE_FORMAT.format(Date(bill.dueDate))
            status.text = if (bill.isPaid) "Paid" else "Unpaid"
            btnMarkPaid.visibility = if (bill.isPaid) View.GONE else View.VISIBLE
            btnMarkPaid.setOnClickListener { onMarkPaid(bill) }
            btnEdit.setOnClickListener { onEdit(bill) }
            btnDelete.setOnClickListener { onDelete(bill) }
            itemView.setOnClickListener { onBillClicked(bill) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<BillEntity>() {
        override fun areItemsTheSame(oldItem: BillEntity, newItem: BillEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BillEntity, newItem: BillEntity): Boolean = oldItem == newItem
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }
}
