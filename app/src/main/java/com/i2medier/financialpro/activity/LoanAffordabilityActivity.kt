package com.i2medier.financialpro.activity

import android.app.DatePickerDialog
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

class LoanAffordabilityActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private lateinit var etDate: TextView
    private lateinit var etInterestRate: EditText
    private lateinit var etMaxMonthly: EditText
    private lateinit var etTerms: EditText
    private var interestRate = 0.0
    private lateinit var llResult: CardView
    private var mDay = 0
    private var mMonth = 0
    private var mYear = 0
    private var mMyDialog: AlertDialog? = null
    private var maxPayment = 0.0
    private var result = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtLoan: TextView
    private lateinit var txtMonthlyPayment: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var txtTotalPayments: TextView
    private lateinit var currencySymbol: String
    private lateinit var loanAffordabilityExplanationCard: CardView
    private lateinit var loanAffordabilityExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_affordability)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.loan_affordability_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        etMaxMonthly = findViewById(R.id.etMaxMonthly)
        etInterestRate = findViewById(R.id.etInterestRate)
        etTerms = findViewById(R.id.etTerms)
        etDate = findViewById(R.id.etDate)
        toolBar = findViewById(R.id.toolBar)
        txtLoan = findViewById(R.id.txtLoan)
        txtMonthlyPayment = findViewById(R.id.txtMonthlyPayment)
        txtTotalPayments = findViewById(R.id.txtTotalPayments)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)
        spTerm = findViewById(R.id.spTerm)
        calendar = Calendar.getInstance()

        findViewById<TextView>(R.id.txtCurrencyLoanAffordabilityA).text = currencySymbol

        val loanAffordabilityText = findViewById<TextView>(R.id.loanAffordabilityText)
        val formattedText = String.format(getString(R.string.loan_affordability_description_html), currencySymbol)
        loanAffordabilityText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        loanAffordabilityExplanationCard = findViewById(R.id.loanAffordabilityExplanationCard)
        loanAffordabilityExplanation = findViewById(R.id.loanAffordabilityExplanation)

        etDate.text = AppConstant.getFormattedDate(calendar.timeInMillis, AppConstant.Date_FoRMAT_DDMMYY)
        etDate.setOnClickListener { startDateDialog() }

        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm.adapter = arrayAdapter
        term = 1
        spTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                term = if (obj == "Yr") 12 else 1
            }
        }

        btnCalculate.setOnClickListener {
            if (calculate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                btnAddToPlanner.visibility = View.VISIBLE
            }
            val totalPayments = maxPayment * terms
            val totalInterest = Utils.getTotalInterest(maxPayment, terms, result)
            txtMonthlyPayment.text = CurrencyManager.format(this, maxPayment)
            txtLoan.text = CurrencyManager.format(this, result)
            txtTotalPayments.text = CurrencyManager.format(this, totalPayments)
            txtTotalInterest.text = CurrencyManager.format(this, totalInterest)

            val termText = if (term == 12) "years" else "months"
            val explanation = getString(
                R.string.loan_affordability_explain_html,
                CurrencyManager.format(this, maxPayment),
                Utils.decimalFormat.format(interestRate),
                (terms / term).toInt().toString() + " " + termText,
                CurrencyManager.format(this, result),
                CurrencyManager.format(this, totalPayments),
                CurrencyManager.format(this, totalInterest)
            )
            loanAffordabilityExplanationCard.visibility = View.VISIBLE
            loanAffordabilityExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        }

        btnStatistics.setOnClickListener {
            if (calculate()) {
                commonModel.principalAmount = result
                commonModel.terms = terms
                commonModel.interestRate = interestRate
                commonModel.monthlyPayment = maxPayment
                commonModel.date = calendar.timeInMillis
                val intent = Intent(this, StatisticsActivity::class.java)
                intent.putExtra("AutoLoan", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }
    }

    private fun calculate(): Boolean {
        return try {
            if (etMaxMonthly.text.toString().isNotEmpty() && etMaxMonthly.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                if (etInterestRate.text.toString().trim().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    if (etTerms.text.toString().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        maxPayment = etMaxMonthly.text.toString().toDouble()
                        interestRate = etInterestRate.text.toString().toDouble()
                        terms = etTerms.text.toString().toDouble() * term
                        val monthlyRate = (interestRate / 100.0) / 12.0
                        result = if (interestRate == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            maxPayment * terms
                        } else {
                            val d3 = monthlyRate + 1.0
                            (maxPayment * (Math.pow(d3, terms) - 1.0)) / (monthlyRate * Math.pow(d3, terms))
                        }
                        if (terms == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            maxPayment = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                        }
                        true
                    } else {
                        etTerms.error = "Please fill out this field"
                        false
                    }
                } else {
                    etMaxMonthly.error = "Please fill out this field"
                    false
                }
            } else {
                etMaxMonthly.error = "Please fill out this field"
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
            ShareUtil.print(this, rootLayout, getString(R.string.loan_affordability_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.loan_affordability_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.loan_affordability_calculator))
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

    private fun startDateDialog() {
        mYear = calendar[Calendar.YEAR]
        mMonth = calendar[Calendar.MONTH]
        mDay = calendar[Calendar.DAY_OF_MONTH]
        DatePickerDialog(this, { _, year, month, day ->
            calendar[Calendar.DAY_OF_MONTH] = day
            calendar[Calendar.MONTH] = month
            calendar[Calendar.YEAR] = year
            etDate.text = AppConstant.getFormattedDate(calendar.timeInMillis, AppConstant.Date_FoRMAT_DDMMYY)
        }, mYear, mMonth, mDay).show()
    }

    private fun addResultToPlanner() {
        if (llResult.visibility != View.VISIBLE) {
            Toast.makeText(this, "Calculate first", Toast.LENGTH_SHORT).show()
            return
        }
        if (result <= 0.0) {
            Toast.makeText(this, "No amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = result,
            type = TransactionType.EXPENSE,
            note = "Affordable loan amount estimate",
            title = getString(R.string.loan_affordability_calculator)
        )
    }
}
