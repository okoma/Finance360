package com.i2medier.financialpro.planner.presentation

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillRepeat
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.domain.BillCategoryUi
import com.i2medier.financialpro.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillAdapter(
    private val onBillClicked: (BillEntity) -> Unit,
    private val onMarkPaid: (BillEntity) -> Unit,
    private val onEdit: (BillEntity) -> Unit,
    private val onDelete: (BillEntity) -> Unit,
    private val activity: Activity
) : ListAdapter<BillAdapter.Item, RecyclerView.ViewHolder>(Diff) {

    sealed class Item {
        data class Bill(val entity: BillEntity) : Item()
        data class Ad(val id: String) : Item()
    }

    private companion object {
        private const val VIEW_TYPE_BILL = 0
        private const val VIEW_TYPE_AD = 1
        val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.Bill -> VIEW_TYPE_BILL
            is Item.Ad -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BILL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bill, parent, false)
                BillHolder(view)
            }
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_native_ad_inline, parent, false)
                AdHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.Bill -> (holder as BillHolder).bind(item.entity)
            is Item.Ad -> (holder as AdHolder).bind()
        }
    }

    inner class BillHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvBillTitle)
        private val amount: TextView = itemView.findViewById(R.id.tvBillAmount)
        private val dueDate: TextView = itemView.findViewById(R.id.tvBillDueDate)
        private val repeat: TextView = itemView.findViewById(R.id.tvBillRepeat)
        private val createdAt: TextView = itemView.findViewById(R.id.tvBillCreatedAt)
        private val status: TextView = itemView.findViewById(R.id.tvBillStatus)
        private val btnMarkPaid: TextView = itemView.findViewById(R.id.btnMarkPaid)
        private val btnEdit: View = itemView.findViewById(R.id.btnEditBill)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteBill)

        fun bind(bill: BillEntity) {
            title.text = "${BillCategoryUi.emojiFor(bill.category)} ${bill.title}"
            amount.text = CurrencyManager.format(itemView.context, bill.amount)
            dueDate.text = itemView.context.getString(
                R.string.planner_bill_due_label,
                DATE_FORMAT.format(Date(bill.dueDate))
            )
            val repeatLabel = when (bill.repeat) {
                BillRepeat.WEEKLY -> itemView.context.getString(R.string.planner_bill_repeat_weekly)
                BillRepeat.MONTHLY -> itemView.context.getString(R.string.planner_bill_repeat_monthly)
                else -> itemView.context.getString(R.string.planner_bill_repeat_none)
            }
            repeat.text = itemView.context.getString(R.string.planner_bill_repeat_label, repeatLabel)
            createdAt.text = itemView.context.getString(
                R.string.planner_bill_detail_created_at,
                DATE_FORMAT.format(Date(bill.createdAt))
            )
            status.text = if (bill.isPaid) {
                itemView.context.getString(R.string.planner_paid)
            } else {
                itemView.context.getString(R.string.planner_bill_detail_unpaid)
            }
            btnMarkPaid.visibility = if (bill.isPaid) View.GONE else View.VISIBLE
            btnMarkPaid.setOnClickListener { onMarkPaid(bill) }
            btnEdit.setOnClickListener { onEdit(bill) }
            btnDelete.setOnClickListener { onDelete(bill) }
            itemView.setOnClickListener { onBillClicked(bill) }
        }
    }

    inner class AdHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adContainer: FrameLayout = itemView.findViewById(R.id.adContainer)

        fun bind() {
            AdAdmob(activity).NativeAd(adContainer, activity)
        }
    }

    private object Diff : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when {
                oldItem is Item.Bill && newItem is Item.Bill -> oldItem.entity.id == newItem.entity.id
                oldItem is Item.Ad && newItem is Item.Ad -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
