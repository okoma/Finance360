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
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import kotlin.math.ceil
import kotlin.math.max

class EmergencyFundCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var etMonthlyExpenses: EditText
    private lateinit var etMonthsCoverage: EditText
    private lateinit var etCurrentSavings: EditText
    private lateinit var etMonthlySavings: EditText
    private lateinit var txtTargetAmount: TextView
    private lateinit var txtAmountNeeded: TextView
    private lateinit var txtTimeToGoal: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var emergencyFundExplanationCard: CardView
    private lateinit var emergencyFundExplanation: TextView
    private var mMyDialog: AlertDialog? = null
    private var latestAmountNeeded: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_fund_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.emergency_fund_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etMonthlyExpenses = findViewById(R.id.etMonthlyExpenses)
        etMonthsCoverage = findViewById(R.id.etMonthsCoverage)
        etCurrentSavings = findViewById(R.id.etCurrentSavings)
        etMonthlySavings = findViewById(R.id.etMonthlySavings)
        txtTargetAmount = findViewById(R.id.txtTargetAmount)
        txtAmountNeeded = findViewById(R.id.txtAmountNeeded)
        txtTimeToGoal = findViewById(R.id.txtTimeToGoal)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyExpenses)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolCurrentSavings)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMonthlySavings)?.text = currencySymbol

        val emergencyFundText = findViewById<TextView>(R.id.emergencyFundText)
        val formattedText = String.format(getString(R.string.emergency_fund_description_html), currencySymbol)
        emergencyFundText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        emergencyFundExplanationCard = findViewById(R.id.emergencyFundExplanationCard)
        emergencyFundExplanation = findViewById(R.id.emergencyFundExplanation)

        clickListeners()
    }

    private fun clickListeners() {
        btnCalculate.setOnClickListener {
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
            if (etMonthlyExpenses.text.toString().isEmpty() || etMonthlyExpenses.text.toString().toDouble() <= 0) {
                etMonthlyExpenses.error = "Please enter valid amount"
                return false
            }
            if (etMonthsCoverage.text.toString().isEmpty() || etMonthsCoverage.text.toString().toDouble() <= 0) {
                etMonthsCoverage.error = "Please enter valid months"
                return false
            }

            val currentSavingsStr = etCurrentSavings.text.toString()
            val currentSavings = if (currentSavingsStr.isEmpty()) 0.0 else currentSavingsStr.toDouble()
            if (currentSavings < 0) {
                etCurrentSavings.error = "Please enter valid amount"
                return false
            }

            val monthlySavingsStr = etMonthlySavings.text.toString()
            val monthlySavings = if (monthlySavingsStr.isEmpty()) 0.0 else monthlySavingsStr.toDouble()
            if (monthlySavings < 0) {
                etMonthlySavings.error = "Please enter valid amount"
                return false
            }

            val monthlyExpenses = etMonthlyExpenses.text.toString().toDouble()
            val monthsCoverage = etMonthsCoverage.text.toString().toDouble()

            val targetAmount = monthlyExpenses * monthsCoverage
            val amountNeeded = max(0.0, targetAmount - currentSavings)
            latestAmountNeeded = amountNeeded

            var timeToGoalMonths = 0.0
            var timeToGoalYears = 0.0
            if (monthlySavings > 0) {
                timeToGoalMonths = ceil(amountNeeded / monthlySavings)
                timeToGoalYears = timeToGoalMonths / 12.0
            } else if (amountNeeded > 0) {
                timeToGoalMonths = Double.POSITIVE_INFINITY
                timeToGoalYears = Double.POSITIVE_INFINITY
            }

            txtTargetAmount.text = CurrencyManager.format(this, targetAmount)
            txtAmountNeeded.text = CurrencyManager.format(this, amountNeeded)

            txtTimeToGoal.text = if (timeToGoalMonths == Double.POSITIVE_INFINITY) {
                "N/A"
            } else {
                "${Utils.decimalFormat.format(timeToGoalMonths)} months (${Utils.decimalFormat.format(timeToGoalYears)} years)"
            }

            val explanation = getString(
                R.string.emergency_fund_explain_html,
                CurrencyManager.format(this, targetAmount),
                Utils.decimalFormat.format(monthsCoverage),
                CurrencyManager.format(this, currentSavings),
                CurrencyManager.format(this, amountNeeded),
                CurrencyManager.format(this, monthlySavings),
                if (timeToGoalMonths == Double.POSITIVE_INFINITY) "N/A" else Utils.decimalFormat.format(timeToGoalMonths),
                if (timeToGoalYears == Double.POSITIVE_INFINITY) "N/A" else Utils.decimalFormat.format(timeToGoalYears),
                CurrencyManager.format(this, monthlyExpenses)
            )
            emergencyFundExplanationCard.visibility = View.VISIBLE
            emergencyFundExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
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
        if (latestAmountNeeded <= 0.0) {
            Toast.makeText(this, "No amount needed to add", Toast.LENGTH_SHORT).show()
            return
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = latestAmountNeeded,
            type = TransactionType.SAVING,
            note = "Emergency fund contribution target",
            title = getString(R.string.emergency_fund_calculator)
        )
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.emergency_fund_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.emergency_fund_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.emergency_fund_calculator))
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
