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

class DebtPayoffCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var etTotalDebt: EditText
    private lateinit var etMonthlyPayment: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var txtPayoffTime: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var txtTotalPaid: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var debtPayoffExplanationCard: CardView
    private lateinit var debtPayoffExplanation: TextView
    private var mMyDialog: AlertDialog? = null
    private var latestTotalPaid: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debt_payoff_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.debt_payoff_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etTotalDebt = findViewById(R.id.etTotalDebt)
        etMonthlyPayment = findViewById(R.id.etMonthlyPayment)
        etInterestRate = findViewById(R.id.etInterestRate)
        txtPayoffTime = findViewById(R.id.txtPayoffTime)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        txtTotalPaid = findViewById(R.id.txtTotalPaid)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolTotalDebt)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyPayment)?.text = currencySymbol

        val debtPayoffText = findViewById<TextView>(R.id.debtPayoffText)
        val formattedText = String.format(getString(R.string.debt_payoff_description_html), currencySymbol)
        debtPayoffText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        debtPayoffExplanationCard = findViewById(R.id.debtPayoffExplanationCard)
        debtPayoffExplanation = findViewById(R.id.debtPayoffExplanation)

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
                val intent = Intent(this, DebtPayoffStatisticsActivity::class.java)
                intent.putExtra("totalDebt", etTotalDebt.text.toString().toDouble())
                intent.putExtra("monthlyPayment", etMonthlyPayment.text.toString().toDouble())
                intent.putExtra("interestRate", etInterestRate.text.toString().toDouble())
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }
    }

    private fun calculate(): Boolean {
        return try {
            if (etTotalDebt.text.toString().isEmpty() || etTotalDebt.text.toString().toDouble() <= 0) {
                etTotalDebt.error = "Please enter valid amount"
                return false
            }
            if (etMonthlyPayment.text.toString().isEmpty() || etMonthlyPayment.text.toString().toDouble() <= 0) {
                etMonthlyPayment.error = "Please enter valid amount"
                return false
            }
            if (etInterestRate.text.toString().isEmpty() || etInterestRate.text.toString().toDouble() < 0) {
                etInterestRate.error = "Please enter valid rate"
                return false
            }

            val totalDebt = etTotalDebt.text.toString().toDouble()
            val monthlyPayment = etMonthlyPayment.text.toString().toDouble()
            val annualRate = etInterestRate.text.toString().toDouble() / 100.0
            val monthlyRate = annualRate / 12.0

            var months: Int
            var totalInterest = 0.0
            var totalPaid: Double

            if (monthlyRate > 0) {
                val paymentToInterestRatio = (totalDebt * monthlyRate) / monthlyPayment
                if (paymentToInterestRatio >= 1) {
                    Toast.makeText(this, "Monthly payment must be greater than monthly interest", Toast.LENGTH_SHORT).show()
                    return false
                }
                months = kotlin.math.ceil(-kotlin.math.ln(1 - paymentToInterestRatio) / kotlin.math.ln(1 + monthlyRate)).toInt()

                var balance = totalDebt
                totalPaid = 0.0
                for (i in 0 until months) {
                    if (balance <= 0.01) break
                    val interestPayment = balance * monthlyRate
                    val principalPayment = kotlin.math.min(monthlyPayment - interestPayment, balance)
                    totalInterest += interestPayment
                    totalPaid += monthlyPayment
                    balance -= principalPayment
                }
            } else {
                months = kotlin.math.ceil(totalDebt / monthlyPayment).toInt()
                totalInterest = 0.0
                totalPaid = totalDebt
            }

            val years = months / 12.0
            txtPayoffTime.text = "${Utils.decimalFormat.format(months)} months (${Utils.decimalFormat.format(years)} years)"
            txtTotalInterest.text = CurrencyManager.format(this, totalInterest)
            txtTotalPaid.text = CurrencyManager.format(this, totalPaid)
            latestTotalPaid = totalPaid

            val explanation = getString(
                R.string.debt_payoff_explain_html,
                CurrencyManager.format(this, totalDebt),
                CurrencyManager.format(this, monthlyPayment),
                Utils.decimalFormat.format(annualRate * 100),
                Utils.decimalFormat.format(months),
                Utils.decimalFormat.format(years),
                CurrencyManager.format(this, totalInterest),
                CurrencyManager.format(this, totalPaid)
            )
            debtPayoffExplanationCard.visibility = View.VISIBLE
            debtPayoffExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
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
        if (latestTotalPaid <= 0.0) {
            Toast.makeText(this, "No amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = latestTotalPaid,
            type = TransactionType.EXPENSE,
            note = "Debt payoff total paid estimate",
            title = getString(R.string.debt_payoff_calculator)
        )
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.debt_payoff_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.debt_payoff_calculator))
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
