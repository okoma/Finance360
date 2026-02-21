import com.google.android.material.button.MaterialButton
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.BillRepeat
import com.i2medier.financialpro.planner.domain.BillCategoryUi
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.TimeZone

class BillAdapter(
    private val onBillClicked: (BillEntity) -> Unit,
    private val onMarkPaid: (BillEntity) -> Unit,
    private val onEdit: (BillEntity) -> Unit,
    private val onDelete: (BillEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private sealed class Row {
        data class BillRow(val item: BillEntity) : Row()
        data class AdRow(val id: String) : Row()
    }
    private val rows = mutableListOf<Row>()
    companion object {
        private const val VIEW_TYPE_BILL = 1
        private const val VIEW_TYPE_AD = 2
    fun submitList(items: List<BillEntity>) {
        rows.clear()
        items.forEachIndexed { index, item ->
            rows.add(Row.BillRow(item))
            if ((index + 1) % 4 == 0 && index != items.lastIndex) {
                rows.add(Row.AdRow("bill_ad_$index"))
            }
        }
        notifyDataSetChanged()
    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.BillRow -> VIEW_TYPE_BILL
            is Row.AdRow -> VIEW_TYPE_AD
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_inline_native_ad_bill, parent, false)
                AdViewHolder(view)
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bill, parent, false)
                BillViewHolder(view)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.BillRow -> (holder as BillViewHolder).bind(row.item)
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
    inner class BillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.tvBillTitle)
        private val amountView: TextView = itemView.findViewById(R.id.tvBillAmount)
        private val dueView: TextView = itemView.findViewById(R.id.tvBillDueDate)
        private val repeatView: TextView = itemView.findViewById(R.id.tvBillRepeat)
        private val paidStatusView: TextView = itemView.findViewById(R.id.tvBillStatus)
        private val markPaidButton: MaterialButton = itemView.findViewById(R.id.btnMarkPaid)
        private val editButton: MaterialButton = itemView.findViewById(R.id.btnEditBill)
        private val deleteButton: TextView = itemView.findViewById(R.id.btnDeleteBill)
        fun bind(item: BillEntity) {
            titleView.text = "${BillCategoryUi.emojiFor(item.category)} ${item.title}"
            amountView.text = formatCurrency(item.amount)
            dueView.text = itemView.context.getString(
                R.string.planner_bill_due_label,
                formatDate(item.dueDate)
            )
            val repeatLabel = when (item.repeat) {
                BillRepeat.WEEKLY -> itemView.context.getString(R.string.planner_bill_repeat_weekly)
                BillRepeat.MONTHLY -> itemView.context.getString(R.string.planner_bill_repeat_monthly)
                else -> itemView.context.getString(R.string.planner_bill_repeat_none)
            repeatView.text = itemView.context.getString(R.string.planner_bill_repeat_label, repeatLabel)
            val paid = item.isPaid
            paidStatusView.visibility = if (paid) View.VISIBLE else View.GONE
            markPaidButton.isEnabled = !paid
            markPaidButton.alpha = if (paid) 0.55f else 1f
            if (paid) {
                markPaidButton.text = itemView.context.getString(R.string.planner_paid)
            } else {
                markPaidButton.text = itemView.context.getString(R.string.planner_mark_paid)
            markPaidButton.setOnClickListener { onMarkPaid(item) }
            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
            val openDetails = View.OnClickListener { onBillClicked(item) }
            itemView.setOnClickListener(openDetails)
            titleView.setOnClickListener(openDetails)
            amountView.setOnClickListener(openDetails)
            dueView.setOnClickListener(openDetails)
            repeatView.setOnClickListener(openDetails)
            paidStatusView.setOnClickListener(openDetails)
        private fun formatCurrency(value: Double): String {
            return NumberFormat.getCurrencyInstance().format(value)
        private fun formatDate(millis: Long): String {
            val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter.format(millis)
}
