import com.google.android.material.button.MaterialButton
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.GoalEntity
import com.i2medier.financialpro.planner.domain.GoalCategoryUi
import com.i2medier.financialpro.planner.domain.daysBetweenUtc
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.roundToInt

data class GoalProgressItem(
    val goal: GoalEntity,
    val savedAmount: Double
)
class GoalProgressAdapter(
    private val onGoalClicked: (GoalEntity) -> Unit,
    private val onAddMoneyClicked: (GoalEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private sealed class Row {
        data class GoalRow(val item: GoalProgressItem) : Row()
        data class AdRow(val id: String) : Row()
    }
    private val rows = mutableListOf<Row>()
    companion object {
        private const val VIEW_TYPE_GOAL = 1
        private const val VIEW_TYPE_AD = 2
    fun submitList(items: List<GoalProgressItem>) {
        rows.clear()
        items.forEachIndexed { index, item ->
            rows.add(Row.GoalRow(item))
            if ((index + 1) % 4 == 0 && index != items.lastIndex) {
                rows.add(Row.AdRow("goal_ad_$index"))
            }
        }
        notifyDataSetChanged()
    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.GoalRow -> VIEW_TYPE_GOAL
            is Row.AdRow -> VIEW_TYPE_AD
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_inline_native_ad_goal, parent, false)
                AdViewHolder(view)
            else -> {
                    .inflate(R.layout.item_goal_progress, parent, false)
                GoalViewHolder(view)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.GoalRow -> (holder as GoalViewHolder).bind(row.item)
            is Row.AdRow -> (holder as AdViewHolder).bind()
    override fun getItemCount(): Int = rows.size
    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adFrame: FrameLayout = itemView.findViewById(R.id.nativeAdFrame)
        private var loaded = false
        fun bind() {
            if (loaded) return
            val activity = itemView.context as? Activity ?: return
            AdAdmob(activity).NativeAd(adFrame, activity)
            loaded = true
    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tvGoalTitle)
        private val descriptionView: TextView = itemView.findViewById(R.id.tvGoalDescription)
        private val createdAtView: TextView = itemView.findViewById(R.id.tvGoalCreatedAt)
        private val progressView: TextView = itemView.findViewById(R.id.tvGoalProgress)
        private val daysLeftView: TextView = itemView.findViewById(R.id.tvGoalDaysLeft)
        private val dailySuggestionView: TextView = itemView.findViewById(R.id.tvGoalDailySuggestion)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressGoal)
        private val addMoneyButton: MaterialButton = itemView.findViewById(R.id.btnAddMoney)
        fun bind(item: GoalProgressItem) {
            val target = item.goal.targetAmount
            val percent = if (target > 0.0) {
                ((item.savedAmount / target) * 100.0).coerceIn(0.0, 100.0).roundToInt()
            } else {
                0
            titleView.text = GoalCategoryUi.titleWithIcon(item.goal.category, item.goal.title)
            descriptionView.text = item.goal.description
            descriptionView.visibility = if (item.goal.description.isNullOrBlank()) View.GONE else View.VISIBLE
            createdAtView.text = itemView.context.getString(
                R.string.planner_goal_detail_created_at,
                formatDisplayDate(item.goal.createdAt)
            )
            progressView.text = "${formatCurrency(item.savedAmount)} / ${formatCurrency(target)}"
            progressBar.progress = percent
            bindDeadlineInsights(item.goal, item.savedAmount, target)
            addMoneyButton.setOnClickListener { onAddMoneyClicked(item.goal) }
            val openDetails = View.OnClickListener { onGoalClicked(item.goal) }
            itemView.setOnClickListener(openDetails)
            titleView.setOnClickListener(openDetails)
            descriptionView.setOnClickListener(openDetails)
            progressView.setOnClickListener(openDetails)
            daysLeftView.setOnClickListener(openDetails)
            dailySuggestionView.setOnClickListener(openDetails)
            progressBar.setOnClickListener(openDetails)
        private fun bindDeadlineInsights(goal: GoalEntity, savedAmount: Double, targetAmount: Double) {
            val targetDate = goal.targetDate
            if (targetDate == null) {
                daysLeftView.visibility = View.GONE
                dailySuggestionView.visibility = View.GONE
                return
            val todayUtc = System.currentTimeMillis().toUtcMidnight()
            val targetUtc = targetDate.toUtcMidnight()
            val daysLeft = daysBetweenUtc(todayUtc, targetUtc)
            val remaining = (targetAmount - savedAmount).coerceAtLeast(0.0)
            when {
                daysLeft < 0 -> {
                    daysLeftView.text = itemView.context.getString(R.string.planner_goal_days_overdue, -daysLeft)
                    daysLeftView.visibility = View.VISIBLE
                    dailySuggestionView.visibility = View.GONE
                }
                daysLeft == 0 -> {
                    daysLeftView.text = itemView.context.getString(R.string.planner_goal_due_today)
                else -> {
                    daysLeftView.text = itemView.context.getString(R.string.planner_goal_days_left, daysLeft)
                    if (remaining > 0.0) {
                        val suggestedDaily = remaining / daysLeft.toDouble()
                        dailySuggestionView.text = itemView.context.getString(
                            R.string.planner_goal_daily_suggestion,
                            formatCurrency(suggestedDaily)
                        )
                        dailySuggestionView.visibility = View.VISIBLE
                    } else {
                        dailySuggestionView.visibility = View.GONE
                    }
    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance().format(amount)
    private fun formatDisplayDate(millis: Long): String {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}
