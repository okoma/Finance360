package com.i2medier.financialpro.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
import java.text.NumberFormat

class RetirementCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var etCurrentAge: EditText
    private lateinit var etRetirementAge: EditText
    private lateinit var etCurrentSavings: EditText
    private lateinit var etMonthlyContribution: EditText
    private lateinit var etExpectedReturn: EditText
    private lateinit var etSocialSecurity: EditText
    private lateinit var etEmployerMatchRate: EditText
    private lateinit var etMatchLimit: EditText
    private lateinit var switchSocialSecurity: SwitchCompat
    private lateinit var switchEmployerMatch: SwitchCompat
    private lateinit var layoutSocialSecurity: LinearLayout
    private lateinit var layoutEmployerMatch: LinearLayout
    private lateinit var txtTotalAtRetirement: TextView
    private lateinit var txtMonthlyIncome: TextView
    private lateinit var txtYearsOfIncome: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var retirementExplanationCard: CardView
    private lateinit var retirementExplanation: TextView
    private var mMyDialog: AlertDialog? = null
    private var latestMonthlyIncomeResult: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retirement_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.retirement_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etCurrentAge = findViewById(R.id.etCurrentAge)
        etRetirementAge = findViewById(R.id.etRetirementAge)
        etCurrentSavings = findViewById(R.id.etCurrentSavings)
        etMonthlyContribution = findViewById(R.id.etMonthlyContribution)
        etExpectedReturn = findViewById(R.id.etExpectedReturn)
        txtTotalAtRetirement = findViewById(R.id.txtTotalAtRetirement)
        txtMonthlyIncome = findViewById(R.id.txtMonthlyIncome)
        txtYearsOfIncome = findViewById(R.id.txtYearsOfIncome)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolCurrentSavings)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyContribution)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolSocialSecurity)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMatchLimit)?.text = currencySymbol

        switchSocialSecurity = findViewById(R.id.switchSocialSecurity)
        switchEmployerMatch = findViewById(R.id.switchEmployerMatch)
        layoutSocialSecurity = findViewById(R.id.layoutSocialSecurity)
        layoutEmployerMatch = findViewById(R.id.layoutEmployerMatch)
        etSocialSecurity = findViewById(R.id.etSocialSecurity)
        etEmployerMatchRate = findViewById(R.id.etEmployerMatchRate)
        etMatchLimit = findViewById(R.id.etMatchLimit)

        switchSocialSecurity.setOnCheckedChangeListener { _, checked ->
            layoutSocialSecurity.visibility = if (checked) View.VISIBLE else View.GONE
        }
        switchEmployerMatch.setOnCheckedChangeListener { _, checked ->
            layoutEmployerMatch.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val retirementText = findViewById<TextView>(R.id.retirementText)
        val formattedText = String.format(getString(R.string.retirement_description_html), currencySymbol)
        retirementText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        retirementExplanationCard = findViewById(R.id.retirementExplanationCard)
        retirementExplanation = findViewById(R.id.retirementExplanation)

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

        btnStatistics.setOnClickListener {
            if (calculate()) {
                val intent = Intent(this, RetirementStatisticsActivity::class.java)
                intent.putExtra("currentAge", etCurrentAge.text.toString().toInt())
                intent.putExtra("retirementAge", etRetirementAge.text.toString().toInt())
                intent.putExtra("currentSavings", etCurrentSavings.text.toString().toDouble())
                intent.putExtra("monthlyContribution", etMonthlyContribution.text.toString().toDouble())
                intent.putExtra("expectedReturn", etExpectedReturn.text.toString().toDouble())
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }
    }

    private fun calculate(): Boolean {
        return try {
            if (etCurrentAge.text.toString().isEmpty() || etCurrentAge.text.toString().toInt() <= 0) {
                etCurrentAge.error = "Please enter valid age"
                return false
            }
            if (etRetirementAge.text.toString().isEmpty() || etRetirementAge.text.toString().toInt() <= 0) {
                etRetirementAge.error = "Please enter valid retirement age"
                return false
            }
            if (etRetirementAge.text.toString().toInt() <= etCurrentAge.text.toString().toInt()) {
                etRetirementAge.error = "Retirement age must be greater than current age"
                return false
            }
            if (etCurrentSavings.text.toString().isEmpty() || etCurrentSavings.text.toString().toDouble() < 0) {
                etCurrentSavings.error = "Please enter valid amount"
                return false
            }
            if (etMonthlyContribution.text.toString().isEmpty() || etMonthlyContribution.text.toString().toDouble() < 0) {
                etMonthlyContribution.error = "Please enter valid amount"
                return false
            }
            if (etExpectedReturn.text.toString().isEmpty() || etExpectedReturn.text.toString().toDouble() <= 0) {
                etExpectedReturn.error = "Please enter valid rate"
                return false
            }

            val currentAge = etCurrentAge.text.toString().toInt()
            val retirementAge = etRetirementAge.text.toString().toInt()
            val currentSavings = etCurrentSavings.text.toString().toDouble()
            val monthlyContribution = etMonthlyContribution.text.toString().toDouble()
            val expectedReturn = etExpectedReturn.text.toString().toDouble() / 100.0

            val yearsToRetirement = retirementAge - currentAge
            val monthlyRate = expectedReturn / 12.0
            val monthsToRetirement = yearsToRetirement * 12

            val futureValueCurrentSavings = currentSavings * Math.pow(1 + expectedReturn, yearsToRetirement.toDouble())

            var effectiveMonthlyContribution = monthlyContribution
            if (switchEmployerMatch.isChecked) {
                try {
                    val matchRate = etEmployerMatchRate.text.toString().toDouble() / 100.0
                    var matchLimit = 0.0
                    if (etMatchLimit.text.toString().isNotEmpty()) {
                        matchLimit = etMatchLimit.text.toString().toDouble()
                    }
                    val employerMatchAmount = kotlin.math.min(monthlyContribution * matchRate, matchLimit / 12.0)
                    effectiveMonthlyContribution = monthlyContribution + employerMatchAmount
                } catch (_: NumberFormatException) {
                }
            }

            val futureValueContributions = if (monthlyRate > 0) {
                effectiveMonthlyContribution * ((Math.pow(1 + monthlyRate, monthsToRetirement.toDouble()) - 1) / monthlyRate)
            } else {
                effectiveMonthlyContribution * monthsToRetirement
            }

            val totalAtRetirement = futureValueCurrentSavings + futureValueContributions

            var socialSecurityIncome = 0.0
            if (switchSocialSecurity.isChecked) {
                try {
                    if (etSocialSecurity.text.toString().isNotEmpty()) {
                        socialSecurityIncome = etSocialSecurity.text.toString().toDouble()
                    }
                } catch (_: NumberFormatException) {
                }
            }

            val monthlyIncome = (totalAtRetirement * 0.04 / 12.0) + socialSecurityIncome
            latestMonthlyIncomeResult = monthlyIncome
            val yearsOfIncome = 25.0

            txtTotalAtRetirement.text = formatCurrency(totalAtRetirement)
            txtMonthlyIncome.text = formatCurrency(monthlyIncome)
            txtYearsOfIncome.text = Utils.decimalFormat.format(yearsOfIncome) + " years"

            var socialSecurityText = ""
            if (switchSocialSecurity.isChecked && socialSecurityIncome > 0) {
                socialSecurityText = "<br/><b> Social Security:</b> ${formatCurrency(socialSecurityIncome)} per month"
            }

            var employerMatchText = ""
            if (switchEmployerMatch.isChecked && effectiveMonthlyContribution > monthlyContribution) {
                val employerMatchAmount = effectiveMonthlyContribution - monthlyContribution
                employerMatchText = "<br/><b> Employer Match:</b> ${formatCurrency(employerMatchAmount)} per month"
            }

            val explanation = getString(
                R.string.retirement_explain_html,
                formatCurrency(totalAtRetirement),
                formatCurrency(monthlyIncome),
                Utils.decimalFormat.format(yearsOfIncome),
                formatCurrency(monthlyContribution),
                currentAge.toString(),
                retirementAge.toString()
            ) + socialSecurityText + employerMatchText
            retirementExplanationCard.visibility = View.VISIBLE
            retirementExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)

            true
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance().apply {
            currency = CurrencyManager.getCurrency(this@RetirementCalculatorActivity)
        }.format(amount)
    }

    private fun addResultToPlanner() {
        if (llResult.visibility != View.VISIBLE) {
            Toast.makeText(this, "Calculate first", Toast.LENGTH_SHORT).show()
            return
        }
        if (latestMonthlyIncomeResult <= 0.0) {
            Toast.makeText(this, "No amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = latestMonthlyIncomeResult,
            type = TransactionType.SAVING,
            note = "Projected retirement monthly income",
            title = getString(R.string.retirement_calculator)
        )
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.retirement_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.retirement_calculator))
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
