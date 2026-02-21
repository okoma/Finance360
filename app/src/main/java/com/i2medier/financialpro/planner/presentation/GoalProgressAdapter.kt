package com.i2medier.financialpro.planner.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.util.CurrencyManager

data class GoalProgressItem(
    val goal: GoalEntity,
    val savedAmount: Double
)

class GoalProgressAdapter(
    private val onGoalClicked: (GoalEntity) -> Unit,
    private val onAddMoneyClicked: (GoalEntity) -> Unit
) : ListAdapter<GoalProgressItem, GoalProgressAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_progress, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvGoalTitle)
        private val description: TextView = itemView.findViewById(R.id.tvGoalDescription)
        private val progressText: TextView = itemView.findViewById(R.id.tvGoalProgress)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressGoal)
        private val btnAdd: TextView = itemView.findViewById(R.id.btnAddMoney)

        fun bind(item: GoalProgressItem) {
            val target = item.goal.targetAmount.coerceAtLeast(0.0)
            val saved = item.savedAmount.coerceAtLeast(0.0)
            val ratio = if (target <= 0.0) 0.0 else (saved / target).coerceIn(0.0, 1.0)
            title.text = item.goal.title
            description.text = item.goal.description.orEmpty()
            progressText.text = "Saved: ${CurrencyManager.format(itemView.context, saved)} / ${CurrencyManager.format(itemView.context, target)}"
            progressBar.progress = (ratio * 100).toInt()
            itemView.setOnClickListener { onGoalClicked(item.goal) }
            btnAdd.setOnClickListener { onAddMoneyClicked(item.goal) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GoalProgressItem>() {
        override fun areItemsTheSame(oldItem: GoalProgressItem, newItem: GoalProgressItem): Boolean = oldItem.goal.id == newItem.goal.id
        override fun areContentsTheSame(oldItem: GoalProgressItem, newItem: GoalProgressItem): Boolean = oldItem == newItem
    }
}
