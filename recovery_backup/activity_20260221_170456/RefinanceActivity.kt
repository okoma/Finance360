package com.i2medier.financialpro.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import java.util.Calendar

class RefinanceActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var clFinanceCharge: TextView
    private lateinit var clMonthlyPayment: TextView
    private lateinit var clTotalPayment: TextView
    private lateinit var commonModel: CommonModel
    private var d1 = 0.0
    private var d2 = 0.0
    private lateinit var etFees: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etInterestRate1: EditText
    private lateinit var etLoanBalance: EditText
    private lateinit var etMonthlyPayment: EditText
    private lateinit var etTerms: EditText
    private var fees = 0.0
    private var interestRate = 0.0
    private var interestRate1 = 0.0
    private var loanBalance = 0.0
    private var mMonth = 0
    private var mMyDialog: AlertDialog? = null
    private var mYear = 0
    private var monthlyPayment = 0.0
    private var result = 0.0
    private lateinit var rlFinanceCharge: TextView
    private lateinit var rlMonthlyPayment: TextView
    private lateinit var rlTotalPayment: TextView
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private lateinit var refinanceExplanationCard: CardView
    private lateinit var refinanceExplanation: TextView
    private lateinit var llResult: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refinance)
        init()
        setUpToolbar()

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.refinance_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        toolBar = findViewById(R.id.toolBar)
        etLoanBalance = findViewById(R.id.etLoanBalance)
        etMonthlyPayment = findViewById(R.id.etMonthlyPayment)
        etInterestRate = findViewById(R.id.etInterestRate)
        etTerms = findViewById(R.id.etTerms)
        etInterestRate1 = findViewById(R.id.etInterestRate1)
        etFees = findViewById(R.id.etFees)
        clMonthlyPayment = findViewById(R.id.clMonthlyPayment)
        clTotalPayment = findViewById(R.id.clTotalPayment)
        clFinanceCharge = findViewById(R.id.clFinanceCharge)
        rlMonthlyPayment = findViewById(R.id.rlMonthlyPayment)
        rlTotalPayment = findViewById(R.id.rlTotalPayment)
        rlFinanceCharge = findViewById(R.id.rlFinanceCharge)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)
        spTerm = findViewById(R.id.spTerm)
        term = 1

        val calendar = Calendar.getInstance()
        mYear = calendar[Calendar.YEAR]
        mMonth = calendar[Calendar.MONTH]

        findViewById<TextView>(R.id.txtCurrencySymbolLoanBalance)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolMonthlyPayment)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolRefinancedFee)?.text = currencySymbol

        val refinanceText = findViewById<TextView>(R.id.refinanceText)
        val formattedText = String.format(getString(R.string.refinance_description_html), currencySymbol)
        refinanceText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        refinanceExplanationCard = findViewById(R.id.refinanceExplanationCard)
        refinanceExplanation = findViewById(R.id.refinanceExplanation)

        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm.adapter = arrayAdapter
        spTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                term = if (obj == "Yr") 12 else 1
            }
        }

        btnCalculate.setOnClickListener {
            if (calculate()) {
                val currentTotalPayments = monthlyPayment * d2
                val currentTotalInterest = currentTotalPayments - loanBalance
                val refinancedTotalPayments = result * d2
                val refinancedTotalInterest = refinancedTotalPayments - loanBalance

                clMonthlyPayment.text = CurrencyManager.format(this, monthlyPayment)
                clTotalPayment.text = CurrencyManager.format(this, currentTotalPayments)
                clFinanceCharge.text = CurrencyManager.format(this, currentTotalInterest)

                rlMonthlyPayment.text = CurrencyManager.format(this, result)
                rlTotalPayment.text = CurrencyManager.format(this, refinancedTotalPayments)
                rlFinanceCharge.text = CurrencyManager.format(this, refinancedTotalInterest)

                val savingsText = if (refinancedTotalPayments < currentTotalPayments) {
                    "Refinancing saves you $currencySymbol${Utils.decimalFormat.format(currentTotalPayments - refinancedTotalPayments)} in total payments."
                } else {
                    "Refinancing increases total payments by $currencySymbol${Utils.decimalFormat.format(refinancedTotalPayments - currentTotalPayments)}."
                }

                val explanation = getString(
                    R.string.refinance_explain_html,
                    CurrencyManager.format(this, monthlyPayment),
                    CurrencyManager.format(this, currentTotalPayments),
                    CurrencyManager.format(this, currentTotalInterest),
                    CurrencyManager.format(this, result),
                    CurrencyManager.format(this, refinancedTotalPayments),
                    CurrencyManager.format(this, refinancedTotalInterest),
                    CurrencyManager.format(this, fees),
                    savingsText
                )
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
                refinanceExplanationCard.visibility = View.VISIBLE
                refinanceExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            }
        }

        btnStatistics.setOnClickListener {
            if (calculate()) {
                commonModel.principalAmount = loanBalance
                commonModel.interestRate = interestRate
                commonModel.terms = if (term == 1) (terms + 1.0) / 12.0 else terms + 1.0
                commonModel.year = mYear
                commonModel.month = mMonth + 1
                commonModel.monthlyPayment = monthlyPayment
                val intent = Intent(this, RefinanceStatisticsActivity::class.java)
                intent.putExtra("Refinance", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = result,
            type = TransactionType.EXPENSE,
            title = getString(R.string.refinance_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun calculate(): Boolean {
        return try {
            if (etLoanBalance.text.toString().isNotEmpty() && etLoanBalance.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                if (etMonthlyPayment.text.toString().isNotEmpty() && etMonthlyPayment.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    if (etInterestRate.text.toString().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        if (etTerms.text.toString().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            if (etInterestRate1.text.toString().isNotEmpty() && etInterestRate1.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                                if (etFees.text.toString().trim().isNotEmpty()) {
                                    fees = etFees.text.toString().toDouble()
                                }
                                loanBalance = etLoanBalance.text.toString().toDouble()
                                monthlyPayment = etMonthlyPayment.text.toString().toDouble()
                                interestRate = etInterestRate.text.toString().toDouble()
                                terms = etTerms.text.toString().toDouble()
                                interestRate1 = etInterestRate1.text.toString().toDouble()
                                d1 = (interestRate1 / 100.0) / 12.0
                                d2 = terms * term
                                result = ((loanBalance * d1) * Math.pow(d1 + 1.0, d2)) / (Math.pow(d1 + 1.0, d2) - 1.0)
                                true
                            } else {
                                etInterestRate1.error = "Please fill out this field"
                                false
                            }
                        } else {
                            etTerms.error = "Please fill out this field"
                            false
                        }
                    } else {
                        etInterestRate.error = "Please fill out this field"
                        false
                    }
                } else {
                    etMonthlyPayment.error = "Please fill out this field"
                    false
                }
            } else {
                etLoanBalance.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.refinance_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.refinance_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.refinance_calculator))
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
