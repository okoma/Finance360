package com.i2medier.financialpro.activity

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.BillEntity
import com.i2medier.financialpro.planner.data.local.BillRepeat
import com.i2medier.financialpro.planner.data.local.TransactionEntity
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.domain.BillCategoryUi
import com.i2medier.financialpro.planner.domain.toUtcMidnight
import com.i2medier.financialpro.planner.presentation.PlannerViewModel
import com.i2medier.financialpro.planner.presentation.TransactionHistoryAdapter
import com.i2medier.financialpro.util.CurrencyManager
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class BillDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BILL_ID = "extra_bill_id"
    }

    private lateinit var plannerViewModel: PlannerViewModel
    private var billId: Long = 0L
    private var currentBill: BillEntity? = null
    private var billTimelineTransactions: List<TransactionEntity> = emptyList()
    private var hasLoadedBills = false

    private lateinit var tvTitle: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var tvRepeat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var billTimelineAdapter: TransactionHistoryAdapter
    private lateinit var tvTimelineEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_bill_detail)

        billId = intent.getLongExtra(EXTRA_BILL_ID, 0L)
        if (billId <= 0L) {
            finish()
            return
        }

        plannerViewModel = ViewModelProvider(
            this,
            PlannerViewModel.Factory(application)
        )[PlannerViewModel::class.java]

        tvTitle = findViewById(R.id.tvBillTitle)
        tvAmount = findViewById(R.id.tvBillAmount)
        tvDueDate = findViewById(R.id.tvBillDueDate)
        tvRepeat = findViewById(R.id.tvBillRepeat)
        tvStatus = findViewById(R.id.tvBillStatus)
        tvCreatedAt = findViewById(R.id.tvBillCreatedAt)
        tvTimelineEmpty = findViewById(R.id.tvBillTimelineEmpty)

        billTimelineAdapter = TransactionHistoryAdapter(
            onDeleteClicked = { transaction -> confirmDeleteTransaction(transaction) }
        )
        findViewById<RecyclerView>(R.id.rvBillTimeline).apply {
            layoutManager = LinearLayoutManager(this@BillDetailActivity)
            adapter = billTimelineAdapter
            isNestedScrollingEnabled = true
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnMarkPaid).setOnClickListener {
            currentBill?.let { bill -> confirmMarkBillPaid(bill) }
        }
        findViewById<Button>(R.id.btnEditBill).setOnClickListener {
            currentBill?.let { bill -> showEditBillDialog(bill) }
        }
        findViewById<Button>(R.id.btnDeleteBill).setOnClickListener {
            currentBill?.let { bill -> confirmDeleteBill(bill) }
        }

        val root = findViewById<View>(R.id.rootBillDetail)
        val header = findViewById<View>(R.id.layoutBillDetailHeader)
        val scroll = findViewById<View>(R.id.scrollBillDetail)
        val headerTop = header.paddingTop
        val headerLeft = header.paddingLeft
        val headerRight = header.paddingRight
        val headerBottom = header.paddingBottom
        val scrollBottom = scroll.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(
                left = headerLeft + bars.left,
                top = headerTop + bars.top,
                right = headerRight + bars.right,
                bottom = headerBottom
            )
            scroll.updatePadding(bottom = scrollBottom + bars.bottom)
            insets
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    plannerViewModel.bills.collect { bills ->
                        hasLoadedBills = true
                        val matched = bills.find { it.id == billId }
                        when {
                            matched != null -> {
                                currentBill = matched
                                renderBill()
                                submitBillTimeline()
                            }
                            currentBill != null -> {
                                // Bill was deleted while viewing details.
                                finish()
                            }
                            bills.isNotEmpty() -> {
                                // Bills are loaded and requested id doesn't exist.
                                finish()
                            }
                            else -> Unit // Wait for DB emission instead of closing on initial empty state.
                        }
                    }
                }
                launch {
                    plannerViewModel.transactions.collect { transactions ->
                        billTimelineTransactions = transactions
                        if (currentBill != null || hasLoadedBills) {
                            submitBillTimeline()
                        }
                    }
                }
            }
        }
    }

    private fun renderBill() {
        val bill = currentBill ?: return
        tvTitle.text = bill.title
        tvAmount.text = formatCurrency(bill.amount)
        tvDueDate.text = getString(R.string.planner_bill_due_label, formatDisplayDate(bill.dueDate))

        val repeatLabel = when (bill.repeat) {
            BillRepeat.WEEKLY -> getString(R.string.planner_bill_repeat_weekly)
            BillRepeat.MONTHLY -> getString(R.string.planner_bill_repeat_monthly)
            else -> getString(R.string.planner_bill_repeat_none)
        }
        tvRepeat.text = getString(R.string.planner_bill_repeat_label, repeatLabel)
        tvCreatedAt.text = getString(R.string.planner_bill_detail_created_at, formatDisplayDate(bill.createdAt))

        tvStatus.text = if (bill.isPaid) {
            getString(R.string.planner_paid)
        } else {
            getString(R.string.planner_bill_detail_unpaid)
        }
        tvStatus.setTextColor(
            resources.getColor(
                if (bill.isPaid) android.R.color.holo_green_dark else android.R.color.holo_red_dark,
                theme
            )
        )

        findViewById<Button>(R.id.btnMarkPaid).apply {
            isEnabled = !bill.isPaid
            alpha = if (bill.isPaid) 0.55f else 1f
            text = if (bill.isPaid) getString(R.string.planner_paid) else getString(R.string.planner_mark_paid)
        }
        findViewById<Button>(R.id.btnEditBill).visibility = if (bill.isPaid) View.GONE else View.VISIBLE
    }

    private fun submitBillTimeline() {
        val bill = currentBill ?: return
        val items = billTimelineTransactions
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.billId == bill.id }
            .take(30)
            .toList()
        billTimelineAdapter.submitList(items)
        tvTimelineEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmMarkBillPaid(bill: BillEntity) {
        if (bill.isPaid) return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_mark_paid))
            .setMessage(getString(R.string.planner_bill_mark_paid_message, bill.title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_mark_paid)) { _, _ ->
                plannerViewModel.markBillPaid(bill)
            }
            .show()
    }

    private fun confirmDeleteBill(bill: BillEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_delete))
            .setMessage(getString(R.string.planner_bill_delete_message, bill.title))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                plannerViewModel.deleteBill(bill)
                finish()
            }
            .show()
    }

    private fun showEditBillDialog(bill: BillEntity) {
        var dueDateMillis = bill.dueDate.toUtcMidnight()
        var selectedBillCategory = BillCategoryUi.normalize(bill.category)
        val dialog = BottomSheetDialog(this, R.style.PlannerMaterial3BottomSheetDialog)
        val content = dialog.layoutInflater.inflate(R.layout.bottom_sheet_bill_form, null, false)
        dialog.setContentView(content)

        val btnClose = content.findViewById<android.widget.ImageView>(R.id.btnClose)
        val sheetTitle = content.findViewById<TextView>(R.id.tvSheetTitle)
        val titleInput = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBillTitle)
        val amountInput = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBillAmount)
        val amountLayout = content.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilBillAmount)
        val categoryCard = content.findViewById<View>(R.id.cardBillCategory)
        val categoryEmoji = content.findViewById<TextView>(R.id.tvBillCategoryEmoji)
        val categoryName = content.findViewById<TextView>(R.id.tvBillCategoryName)
        val repeatInput = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBillRepeat)
        val repeatLayout = content.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilBillRepeat)
        val dueDateInput = content.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etBillDueDate)
        val dueDateLayout = content.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilBillDueDate)
        val saveButton = content.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBillSave)

        sheetTitle.text = getString(R.string.planner_edit_bill_title)
        saveButton.text = getString(R.string.planner_save)
        titleInput.setText(bill.title)
        amountLayout.hint = amountPlaceholder()
        amountInput.setText(formatCurrencyInput(bill.amount))
        amountInput.enableCurrencyFormatting()
        categoryEmoji.text = BillCategoryUi.emojiFor(selectedBillCategory)
        categoryName.text = BillCategoryUi.labelFor(selectedBillCategory)

        categoryCard.setOnClickListener {
            val options = BillCategoryUi.options()
            val labels = options.map { "${it.emoji}  ${it.label}" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.planner_hint_category))
                .setItems(labels) { _, which ->
                    val selected = options[which]
                    selectedBillCategory = selected.key
                    categoryEmoji.text = selected.emoji
                    categoryName.text = selected.label
                }
                .show()
        }

        val repeatOptions = listOf(
            getString(R.string.planner_bill_repeat_none),
            getString(R.string.planner_bill_repeat_weekly),
            getString(R.string.planner_bill_repeat_monthly)
        )
        var selectedRepeatIndex = when (bill.repeat) {
            BillRepeat.WEEKLY -> 1
            BillRepeat.MONTHLY -> 2
            else -> 0
        }
        fun showRepeatPicker() {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.planner_bill_repeat_hint))
                .setItems(repeatOptions.toTypedArray()) { _, which ->
                    selectedRepeatIndex = which
                    repeatInput.setText(repeatOptions[selectedRepeatIndex])
                }
                .show()
        }
        repeatInput.setText(repeatOptions[selectedRepeatIndex])
        repeatInput.setOnClickListener { showRepeatPicker() }
        repeatLayout.setEndIconOnClickListener { showRepeatPicker() }

        fun updateDueDateText() {
            dueDateInput.setText(getString(R.string.planner_bill_due_date, formatDisplayDate(dueDateMillis)))
        }
        updateDueDateText()
        val openDatePicker = {
            showDatePicker(dueDateMillis) { millis ->
                dueDateMillis = millis.toUtcMidnight()
                updateDueDateText()
            }
        }
        dueDateInput.setOnClickListener { openDatePicker() }
        dueDateLayout.setEndIconOnClickListener { openDatePicker() }
        btnClose.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            val amount = parseCurrencyInput(amountInput.text?.toString().orEmpty())
            val repeat = when (selectedRepeatIndex) {
                1 -> BillRepeat.WEEKLY
                2 -> BillRepeat.MONTHLY
                else -> null
            }
            val error = plannerViewModel.updateBill(
                bill = bill,
                title = title,
                amount = amount,
                dueDateMillis = dueDateMillis,
                repeat = repeat,
                category = selectedBillCategory
            )
            if (error != null) {
                toast(error)
            } else {
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.applyRoundedCornersAndHeight()
    }

    private fun confirmDeleteTransaction(transaction: TransactionEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.planner_delete_transaction))
            .setMessage(getString(R.string.planner_delete_transaction_message))
            .setNegativeButton(getString(android.R.string.cancel), null)
            .setPositiveButton(getString(R.string.planner_delete)) { _, _ ->
                plannerViewModel.deleteTransaction(transaction)
            }
            .show()
    }

    private fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onDateSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addField(container: LinearLayout, view: View, topMarginDp: Int) {
        container.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (topMarginDp > 0) topMargin = dp(topMarginDp)
            }
        )
    }

    private fun EditText.styleModalField(iconRes: Int? = null) {
        background = resources.getDrawable(R.drawable.bg_modal_input_outline, theme)
        textSize = 15f
        setPadding(dp(10), dp(9), dp(10), dp(9))
        setCompoundDrawablesWithIntrinsicBounds(iconRes ?: 0, 0, 0, 0)
        compoundDrawablePadding = if (iconRes != null) dp(8) else 0
    }

    private fun EditText.enableCurrencyFormatting() {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencySymbol = NumberFormat.getCurrencyInstance(locale).currency?.symbol ?: symbols.currencySymbol
        val grouping = symbols.groupingSeparator
        val decimal = symbols.decimalSeparator
        val watcher = object : TextWatcher {
            private var editing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (editing) return
                val raw = s?.toString().orEmpty()
                if (raw.isBlank()) return
                val normalized = normalizeCurrencyInput(raw, currencySymbol, grouping, decimal)
                if (normalized.isBlank()) return
                val value = normalized.toDoubleOrNull() ?: return
                editing = true
                setText(formatCurrencyInput(value))
                setSelection(text.length)
                editing = false
            }
        }
        addTextChangedListener(watcher)
    }

    private fun parseCurrencyInput(text: String): Double? {
        val locale = Locale.getDefault()
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val currencySymbol = NumberFormat.getCurrencyInstance(locale).currency?.symbol ?: symbols.currencySymbol
        val normalized = normalizeCurrencyInput(
            value = text,
            currencySymbol = currencySymbol,
            grouping = symbols.groupingSeparator,
            decimal = symbols.decimalSeparator
        )
        return normalized.toDoubleOrNull()
    }

    private fun normalizeCurrencyInput(
        value: String,
        currencySymbol: String,
        grouping: Char,
        decimal: Char
    ): String {
        val stripped = value
            .replace(currencySymbol, "")
            .replace("\\s".toRegex(), "")
            .replace(grouping.toString(), "")
            .replace(decimal, '.')
        val builder = StringBuilder()
        var dotSeen = false
        stripped.forEach { ch ->
            when {
                ch.isDigit() -> builder.append(ch)
                ch == '.' && !dotSeen -> {
                    builder.append('.')
                    dotSeen = true
                }
            }
        }
        val normalized = builder.toString()
        return if (normalized.startsWith(".")) "0$normalized" else normalized
    }

    private fun formatCurrencyInput(value: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(value)
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance().apply {
            currency = CurrencyManager.getCurrency(this@BillDetailActivity)
        }.format(value)
    }

    private fun formatDisplayDate(millis: Long): String {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(millis)
    }

    private fun amountPlaceholder(): String {
        return "${CurrencyManager.getCurrencySymbol(this)}0"
    }

    private fun Button.stylePrimaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_primary, theme)
        setTextColor(resources.getColor(android.R.color.white, theme))
        minimumHeight = dp(46)
        elevation = 2f * resources.displayMetrics.density
    }

    private fun Button.styleSecondaryButton() {
        background = resources.getDrawable(R.drawable.bg_modal_btn_secondary, theme)
        setTextColor(resources.getColor(R.color.colorDark, theme))
        minimumHeight = dp(46)
        elevation = 2f * resources.displayMetrics.density
    }

    private fun BottomSheetDialog.applyRoundedCornersAndHeight() {
        findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundColor(Color.TRANSPARENT)
            (bottomSheet.parent as? android.view.View)?.setBackgroundColor(Color.TRANSPARENT)
            (((bottomSheet.parent as? android.view.View)?.parent) as? android.view.View)
                ?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                isFitToContents = true
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window?.navigationBarColor = Color.WHITE
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
