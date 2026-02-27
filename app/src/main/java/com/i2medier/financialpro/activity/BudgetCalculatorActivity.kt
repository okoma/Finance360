package com.i2medier.financialpro.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.ui.widgets.BudgetDonutChartView
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import kotlin.math.abs

class BudgetCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var etMonthlyIncome: EditText
    private lateinit var etHousing: EditText
    private lateinit var etUtilities: EditText
    private lateinit var etFood: EditText
    private lateinit var etTransportation: EditText
    private lateinit var etEntertainment: EditText
    private lateinit var etOtherExpenses: EditText
    private lateinit var txtTotalExpenses: TextView
    private lateinit var txtRemainingBalance: TextView
    private lateinit var txtSavingsRate: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var budgetExplanationCard: CardView
    private lateinit var budgetExplanation: TextView
    private lateinit var budgetVisualizationCard: CardView
    private lateinit var budgetBreakdownChart: BudgetDonutChartView
    private lateinit var txtHousingPercent: TextView
    private lateinit var txtUtilitiesPercent: TextView
    private lateinit var txtFoodPercent: TextView
    private lateinit var txtTransportPercent: TextView
    private lateinit var txtEntertainmentPercent: TextView
    private lateinit var txtOtherPercent: TextView
    private lateinit var txtRemainingPercent: TextView
    private var mMyDialog: AlertDialog? = null
    private var latestRemainingBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.budget_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etMonthlyIncome = findViewById(R.id.etMonthlyIncome)
        etHousing = findViewById(R.id.etHousing)
        etUtilities = findViewById(R.id.etUtilities)
        etFood = findViewById(R.id.etFood)
        etTransportation = findViewById(R.id.etTransportation)
        etEntertainment = findViewById(R.id.etEntertainment)
        etOtherExpenses = findViewById(R.id.etOtherExpenses)
        txtTotalExpenses = findViewById(R.id.txtTotalExpenses)
        txtRemainingBalance = findViewById(R.id.txtRemainingBalance)
        txtSavingsRate = findViewById(R.id.txtSavingsRate)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyIncome)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolHousing)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolUtilities)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolFood)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolTransportation)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolEntertainment)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolOtherExpenses)?.text = currencySymbol

        val budgetText = findViewById<TextView>(R.id.budgetText)
        val formattedText = String.format(getString(R.string.budget_description_html), currencySymbol)
        budgetText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        budgetExplanationCard = findViewById(R.id.budgetExplanationCard)
        budgetExplanation = findViewById(R.id.budgetExplanation)
        budgetVisualizationCard = findViewById(R.id.budgetVisualizationCard)
        budgetBreakdownChart = findViewById(R.id.budgetBreakdownChart)
        txtHousingPercent = findViewById(R.id.txtHousingPercent)
        txtUtilitiesPercent = findViewById(R.id.txtUtilitiesPercent)
        txtFoodPercent = findViewById(R.id.txtFoodPercent)
        txtTransportPercent = findViewById(R.id.txtTransportPercent)
        txtEntertainmentPercent = findViewById(R.id.txtEntertainmentPercent)
        txtOtherPercent = findViewById(R.id.txtOtherPercent)
        txtRemainingPercent = findViewById(R.id.txtRemainingPercent)

        clickListeners()
    }

    private fun clickListeners() {
        btnCalculate.setOnClickListener {
            com.i2medier.financialpro.util.AnalyticsTracker.logCalculatorCalculated(this, javaClass.simpleName)
            if (calculate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                btnAddToPlanner.visibility = View.VISIBLE
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }
    }

    private fun calculate(): Boolean {
        return try {
            if (etMonthlyIncome.text.toString().isEmpty() || etMonthlyIncome.text.toString().toDouble() <= 0) {
                etMonthlyIncome.error = "Please enter valid income"
                return false
            }

            val monthlyIncome = etMonthlyIncome.text.toString().toDouble()
            val housing = getDoubleValue(etHousing)
            val utilities = getDoubleValue(etUtilities)
            val food = getDoubleValue(etFood)
            val transportation = getDoubleValue(etTransportation)
            val entertainment = getDoubleValue(etEntertainment)
            val otherExpenses = getDoubleValue(etOtherExpenses)

            val totalExpenses = housing + utilities + food + transportation + entertainment + otherExpenses
            val remainingBalance = monthlyIncome - totalExpenses
            val savingsRate = (remainingBalance / monthlyIncome) * 100
            latestRemainingBalance = remainingBalance

            txtTotalExpenses.text = CurrencyManager.format(this, totalExpenses)
            txtRemainingBalance.text = CurrencyManager.format(this, remainingBalance)
            txtSavingsRate.text = Utils.decimalFormat.format(savingsRate) + "%"

            val housingPercent = (housing / monthlyIncome) * 100
            val utilitiesPercent = (utilities / monthlyIncome) * 100
            val foodPercent = (food / monthlyIncome) * 100
            val transportationPercent = (transportation / monthlyIncome) * 100
            val entertainmentPercent = (entertainment / monthlyIncome) * 100
            val otherPercent = (otherExpenses / monthlyIncome) * 100

            // Update donut chart with budget breakdown - all categories shown separately
            updateBudgetVisualization(
                housingPercent,
                utilitiesPercent,
                foodPercent,
                transportationPercent,
                entertainmentPercent,
                otherPercent,
                savingsRate.coerceAtLeast(0.0)
            )

            val explanation = getString(
                R.string.budget_explain_html,
                CurrencyManager.format(this, monthlyIncome),
                CurrencyManager.format(this, totalExpenses),
                CurrencyManager.format(this, remainingBalance),
                Utils.decimalFormat.format(savingsRate),
                CurrencyManager.format(this, housing),
                Utils.decimalFormat.format(housingPercent),
                CurrencyManager.format(this, utilities),
                Utils.decimalFormat.format(utilitiesPercent),
                CurrencyManager.format(this, food),
                Utils.decimalFormat.format(foodPercent),
                CurrencyManager.format(this, transportation),
                Utils.decimalFormat.format(transportationPercent),
                CurrencyManager.format(this, entertainment),
                Utils.decimalFormat.format(entertainmentPercent),
                CurrencyManager.format(this, otherExpenses),
                Utils.decimalFormat.format(otherPercent)
            )
            budgetExplanationCard.visibility = View.VISIBLE
            budgetExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            true
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun addResultToPlanner() {
        if (llResult.visibility != View.VISIBLE) {
            Toast.makeText(this, "Calculate first", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = abs(latestRemainingBalance)
        if (amount <= 0.0) {
            Toast.makeText(this, "No amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (latestRemainingBalance >= 0.0) {
            TransactionType.SAVING
        } else {
            TransactionType.EXPENSE
        }
        val note = if (latestRemainingBalance >= 0.0) {
            "Budget surplus from Budget Calculator"
        } else {
            "Budget deficit from Budget Calculator"
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = amount,
            type = type,
            note = note,
            title = getString(R.string.budget_calculator)
        )
    }

    private fun getDoubleValue(editText: EditText): Double {
        val text = editText.text.toString()
        return if (text.isEmpty()) 0.0 else text.toDouble()
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.budget_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.budget_calculator))
                return
            }
            for (i in permissions.indices) {
                if (grantResults[i] == -1 && !shouldShowRequestPermissionRationale(permissions[i])) {
                    showAlert()
                    return
                }
            }
        }
    }

    private fun updateBudgetVisualization(
        housingPercent: Double,
        utilitiesPercent: Double,
        foodPercent: Double,
        transportPercent: Double,
        entertainmentPercent: Double,
        otherPercent: Double,
        remainingPercent: Double
    ) {
        // Show visualization card
        budgetVisualizationCard.visibility = View.VISIBLE

        // Update legend percentages
        txtHousingPercent.text = "${Utils.decimalFormat.format(housingPercent)}%"
        txtUtilitiesPercent.text = "${Utils.decimalFormat.format(utilitiesPercent)}%"
        txtFoodPercent.text = "${Utils.decimalFormat.format(foodPercent)}%"
        txtTransportPercent.text = "${Utils.decimalFormat.format(transportPercent)}%"
        txtEntertainmentPercent.text = "${Utils.decimalFormat.format(entertainmentPercent)}%"
        txtOtherPercent.text = "${Utils.decimalFormat.format(otherPercent)}%"
        txtRemainingPercent.text = "${Utils.decimalFormat.format(remainingPercent)}%"

        // Prepare data for donut chart - all expense categories shown separately
        val segments = listOf(
            BudgetDonutChartView.ChartSegment(
                percentage = housingPercent.toFloat(),
                color = 0xFF4DB6AC.toInt() // Teal - Housing
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = utilitiesPercent.toFloat(),
                color = 0xFF26A69A.toInt() // Dark Teal - Utilities
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = foodPercent.toFloat(),
                color = 0xFF81C784.toInt() // Light Green - Food
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = transportPercent.toFloat(),
                color = 0xFFFFD54F.toInt() // Yellow - Transportation
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = entertainmentPercent.toFloat(),
                color = 0xFFFFB74D.toInt() // Light Orange - Entertainment
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = otherPercent.toFloat(),
                color = 0xFFFF9800.toInt() // Orange - Other Expenses
            ),
            BudgetDonutChartView.ChartSegment(
                percentage = remainingPercent.toFloat(),
                color = 0xFFFF8A65.toInt() // Red/Salmon - Remaining/Savings
            )
        )

        budgetBreakdownChart.setData(segments)
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires permission. Please ensure that this is enabled in settings, then press the back button to continue ")
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { _, _ ->
            val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
            intent.data = Uri.fromParts("package", packageName, null)
            launchForResult(intent, 112)
            mMyDialog?.dismiss()
        }
        mMyDialog = builder.show()
    }
}
