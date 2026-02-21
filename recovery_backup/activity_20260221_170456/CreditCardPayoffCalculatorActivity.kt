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
import kotlin.math.ln
import kotlin.math.min

class CreditCardPayoffCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var etCreditCardBalance: EditText
    private lateinit var etAPR: EditText
    private lateinit var etMonthlyPayment: EditText
    private lateinit var txtPayoffTime: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var txtTotalPaid: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var creditCardPayoffExplanationCard: CardView
    private lateinit var creditCardPayoffExplanation: TextView
    private var mMyDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_card_payoff_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.credit_card_payoff_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etCreditCardBalance = findViewById(R.id.etCreditCardBalance)
        etAPR = findViewById(R.id.etAPR)
        etMonthlyPayment = findViewById(R.id.etMonthlyPayment)
        txtPayoffTime = findViewById(R.id.txtPayoffTime)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        txtTotalPaid = findViewById(R.id.txtTotalPaid)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolCreditCardBalance)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyPayment)?.text = currencySymbol

        val creditCardPayoffText = findViewById<TextView>(R.id.creditCardPayoffText)
        val formattedText = String.format(getString(R.string.credit_card_payoff_description_html), currencySymbol)
        creditCardPayoffText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        creditCardPayoffExplanationCard = findViewById(R.id.creditCardPayoffExplanationCard)
        creditCardPayoffExplanation = findViewById(R.id.creditCardPayoffExplanation)

        clickListeners()
    }

    private fun clickListeners() {
        btnCalculate.setOnClickListener {
            if (calculate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
            }
        }

        btnStatistics.setOnClickListener {
            if (calculate()) {
                val intent = Intent(this, CreditCardPayoffStatisticsActivity::class.java)
                intent.putExtra("creditCardBalance", etCreditCardBalance.text.toString().toDouble())
                intent.putExtra("apr", etAPR.text.toString().toDouble())
                intent.putExtra("monthlyPayment", etMonthlyPayment.text.toString().toDouble())
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val monthlyPayment = etMonthlyPayment.text.toString().toDoubleOrNull() ?: return
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = monthlyPayment,
            type = TransactionType.EXPENSE,
            title = getString(R.string.credit_card_payoff_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun calculate(): Boolean {
        return try {
            if (etCreditCardBalance.text.toString().isEmpty() || etCreditCardBalance.text.toString().toDouble() <= 0) {
                etCreditCardBalance.error = "Please enter valid balance"
                return false
            }
            if (etAPR.text.toString().isEmpty() || etAPR.text.toString().toDouble() < 0) {
                etAPR.error = "Please enter valid APR"
                return false
            }
            if (etMonthlyPayment.text.toString().isEmpty() || etMonthlyPayment.text.toString().toDouble() <= 0) {
                etMonthlyPayment.error = "Please enter valid payment"
                return false
            }

            val balance = etCreditCardBalance.text.toString().toDouble()
            val apr = etAPR.text.toString().toDouble() / 100.0
            val monthlyPayment = etMonthlyPayment.text.toString().toDouble()
            val monthlyRate = apr / 12.0

            var months: Int
            var totalInterest = 0.0
            var totalPaid: Double
            var currentBalance = balance

            if (monthlyRate > 0) {
                val paymentToInterestRatio = (balance * monthlyRate) / monthlyPayment
                if (paymentToInterestRatio >= 1) {
                    Toast.makeText(this, "Monthly payment must be greater than monthly interest", Toast.LENGTH_SHORT).show()
                    return false
                }
                months = ceil(-ln(1 - paymentToInterestRatio) / ln(1 + monthlyRate)).toInt()

                currentBalance = balance
                totalPaid = 0.0
                for (i in 0 until months) {
                    if (currentBalance <= 0.01) break
                    val interestPayment = currentBalance * monthlyRate
                    val principalPayment = min(monthlyPayment - interestPayment, currentBalance)
                    totalInterest += interestPayment
                    totalPaid += monthlyPayment
                    currentBalance -= principalPayment
                }
            } else {
                months = ceil(balance / monthlyPayment).toInt()
                totalInterest = 0.0
                totalPaid = balance
            }

            val years = months / 12.0
            val extraPayment = monthlyPayment * 0.1
            val savingsWithExtra = calculateSavingsWithExtra(balance, apr, monthlyPayment, extraPayment)

            txtPayoffTime.text = "${Utils.decimalFormat.format(months)} months (${Utils.decimalFormat.format(years)} years)"
            txtTotalInterest.text = CurrencyManager.format(this, totalInterest)
            txtTotalPaid.text = CurrencyManager.format(this, totalPaid)

            val explanation = getString(
                R.string.credit_card_payoff_explain_html,
                CurrencyManager.format(this, balance),
                Utils.decimalFormat.format(apr * 100),
                CurrencyManager.format(this, monthlyPayment),
                Utils.decimalFormat.format(months),
                Utils.decimalFormat.format(years),
                CurrencyManager.format(this, totalInterest),
                CurrencyManager.format(this, totalPaid),
                CurrencyManager.format(this, extraPayment),
                CurrencyManager.format(this, savingsWithExtra)
            )
            creditCardPayoffExplanationCard.visibility = View.VISIBLE
            creditCardPayoffExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            true
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun calculateSavingsWithExtra(balance: Double, apr: Double, monthlyPayment: Double, extraPayment: Double): Double {
        val monthlyRate = apr / 12.0
        val newPayment = monthlyPayment + extraPayment
        val paymentToInterestRatio = (balance * monthlyRate) / newPayment
        if (paymentToInterestRatio >= 1) return 0.0

        val monthsWithExtra = ceil(-ln(1 - paymentToInterestRatio) / ln(1 + monthlyRate)).toInt()
        var totalInterestWithExtra = 0.0
        var currentBalance = balance

        for (i in 0 until monthsWithExtra) {
            if (currentBalance <= 0.01) break
            val interestPayment = currentBalance * monthlyRate
            val principalPayment = min(newPayment - interestPayment, currentBalance)
            totalInterestWithExtra += interestPayment
            currentBalance -= principalPayment
        }

        var totalInterestOriginal = 0.0
        currentBalance = balance
        val paymentToInterestRatioOriginal = (balance * monthlyRate) / monthlyPayment
        val monthsOriginal = ceil(-ln(1 - paymentToInterestRatioOriginal) / ln(1 + monthlyRate)).toInt()

        for (i in 0 until monthsOriginal) {
            if (currentBalance <= 0.01) break
            val interestPayment = currentBalance * monthlyRate
            val principalPayment = min(monthlyPayment - interestPayment, currentBalance)
            totalInterestOriginal += interestPayment
            currentBalance -= principalPayment
        }

        return totalInterestOriginal - totalInterestWithExtra
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.credit_card_payoff_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.credit_card_payoff_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.credit_card_payoff_calculator))
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
