package com.i2medier.financialpro.planner.presentation

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.domain.daysBetweenUtc
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GoalProgressItem(
    val goal: GoalEntity,
    val savedAmount: Double
)

class GoalProgressAdapter(
    private val onGoalClicked: (GoalEntity) -> Unit,
    private val onAddMoneyClicked: (GoalEntity) -> Unit,
    private val activity: Activity
) : ListAdapter<GoalProgressAdapter.Item, RecyclerView.ViewHolder>(Diff) {

    sealed class Item {
        data class Goal(val item: GoalProgressItem) : Item()
        data class Ad(val id: String) : Item()
    }

    private companion object {
        private const val VIEW_TYPE_GOAL = 0
        private const val VIEW_TYPE_AD = 1
        val CREATED_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.Goal -> VIEW_TYPE_GOAL
            is Item.Ad -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GOAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_goal_progress, parent, false)
                GoalHolder(view)
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
            is Item.Goal -> (holder as GoalHolder).bind(item.item)
            is Item.Ad -> (holder as AdHolder).bind()
        }
    }

    inner class GoalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvGoalTitle)
        private val description: TextView = itemView.findViewById(R.id.tvGoalDescription)
        private val createdAt: TextView = itemView.findViewById(R.id.tvGoalCreatedAt)
        private val progressText: TextView = itemView.findViewById(R.id.tvGoalProgress)
        private val daysLeft: TextView = itemView.findViewById(R.id.tvGoalDaysLeft)
        private val dailySuggestion: TextView = itemView.findViewById(R.id.tvGoalDailySuggestion)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressGoal)
        private val btnAdd: TextView = itemView.findViewById(R.id.btnAddMoney)

        fun bind(item: GoalProgressItem) {
            val target = item.goal.targetAmount.coerceAtLeast(0.0)
            val saved = item.savedAmount.coerceAtLeast(0.0)
            val ratio = if (target <= 0.0) 0.0 else (saved / target).coerceIn(0.0, 1.0)
            title.text = GoalCategoryUi.titleWithIcon(item.goal.category, item.goal.title)
            val note = item.goal.description?.trim().orEmpty()
            description.text = note
            description.visibility = if (note.isBlank()) View.GONE else View.VISIBLE
            bindCreatedDate(item.goal)
            bindDeadlineInsights(item.goal, saved, target)
            progressText.text = "Saved: ${CurrencyManager.format(itemView.context, saved)} / ${CurrencyManager.format(itemView.context, target)}"
            progressBar.progress = (ratio * 100).toInt()
            itemView.setOnClickListener { onGoalClicked(item.goal) }
            btnAdd.setOnClickListener { onAddMoneyClicked(item.goal) }
        }

        private fun bindCreatedDate(goal: GoalEntity) {
            createdAt.visibility = View.VISIBLE
            createdAt.text = itemView.context.getString(
                R.string.planner_goal_detail_created_at,
                CREATED_DATE_FORMAT.format(Date(goal.createdAt))
            )
        }

        private fun bindDeadlineInsights(goal: GoalEntity, savedAmount: Double, targetAmount: Double) {
            val targetDate = goal.targetDate
            if (targetDate == null) {
                daysLeft.visibility = View.GONE
                dailySuggestion.visibility = View.GONE
                return
            }

            val todayUtc = System.currentTimeMillis().toUtcMidnight()
            val targetUtc = targetDate.toUtcMidnight()
            val remaining = (targetAmount - savedAmount).coerceAtLeast(0.0)
            val totalDaysLeft = daysBetweenUtc(todayUtc, targetUtc)

            when {
                totalDaysLeft < 0 -> {
                    daysLeft.text = itemView.context.getString(R.string.planner_goal_days_overdue, -totalDaysLeft)
                    daysLeft.visibility = View.VISIBLE
                    dailySuggestion.visibility = View.GONE
                }
                totalDaysLeft == 0 -> {
                    daysLeft.text = itemView.context.getString(R.string.planner_goal_due_today)
                    daysLeft.visibility = View.VISIBLE
                    dailySuggestion.visibility = View.GONE
                }
                else -> {
                    daysLeft.text = itemView.context.getString(R.string.planner_goal_days_left, totalDaysLeft)
                    daysLeft.visibility = View.VISIBLE
                    if (remaining > 0.0) {
                        val suggestedDaily = remaining / totalDaysLeft.toDouble()
                        dailySuggestion.text = itemView.context.getString(
                            R.string.planner_goal_daily_suggestion,
                            CurrencyManager.format(itemView.context, suggestedDaily)
                        )
                        dailySuggestion.visibility = View.VISIBLE
                    } else {
                        dailySuggestion.visibility = View.GONE
                    }
                }
            }
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
                oldItem is Item.Goal && newItem is Item.Goal -> oldItem.item.goal.id == newItem.item.goal.id
                oldItem is Item.Ad && newItem is Item.Ad -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
