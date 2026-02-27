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
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import java.util.ArrayList
import java.util.Calendar

class InterestOnlyActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private lateinit var etDate: TextView
    private lateinit var etInterestPeriod: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etLoanAmount: EditText
    private lateinit var etTerms: EditText
    private var interestPeriod = 0.0
    private var interestRate = 0.0
    private lateinit var llResult: CardView
    private var loanAmount = 0.0
    private var mDay = 0
    private var mMonth = 0
    private var mYear = 0
    private var mMyDialog: AlertDialog? = null
    private lateinit var monthModels: ArrayList<MonthModel>
    private var monthlyPayment = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spInterestPeriodTerm: Spinner
    private lateinit var spTerm: Spinner
    private var term = 1
    private var termInterestOnly = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest: TextView
    private lateinit var txtInterestPayment: TextView
    private lateinit var txtOverPayments: TextView
    private lateinit var txtPrincipal: TextView
    private lateinit var currencySymbol: String
    private lateinit var interestOnlyExplanationCard: CardView
    private lateinit var interestOnlyExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interest_only)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.interest_only_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        monthModels = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        etLoanAmount = findViewById(R.id.etLoanAmount)
        etInterestRate = findViewById(R.id.etInterestRate)
        etTerms = findViewById(R.id.etTerms)
        etInterestPeriod = findViewById(R.id.etInterestPeriod)
        etDate = findViewById(R.id.etDate)
        txtInterestPayment = findViewById(R.id.txtInterestPayment)
        txtPrincipal = findViewById(R.id.txtPrincipal)
        txtOverPayments = findViewById(R.id.txtOverPayments)
        txtInterest = findViewById(R.id.txtInterest)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm = findViewById(R.id.spTerm)
        llResult = findViewById(R.id.llResult)
        spInterestPeriodTerm = findViewById(R.id.spInterestPeriodTerm)
        calendar = Calendar.getInstance()

        findViewById<TextView>(R.id.txtCurrencyIntrestOnly).text = currencySymbol

        val interestOnlyText = findViewById<TextView>(R.id.interestOnlyText)
        val formattedText = String.format(getString(R.string.interest_only_description_html), currencySymbol)
        interestOnlyText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        interestOnlyExplanationCard = findViewById(R.id.interestOnlyExplanationCard)
        interestOnlyExplanation = findViewById(R.id.interestOnlyExplanation)

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

        val arrayAdapter2 = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spInterestPeriodTerm.adapter = arrayAdapter2
        spInterestPeriodTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                termInterestOnly = if (obj == "Yr") 12 else 1
            }
        }

        btnCalculate.setOnClickListener {
            if (setValue()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
            }
            val interestOnlyPayment = Utils.getInterestOnly(interestRate, loanAmount)
            val interestOnly = (interestOnlyPayment * interestPeriod) + (monthlyPayment * terms)
            val totalInterest = interestOnly - loanAmount

            txtInterestPayment.text = CurrencyManager.format(this, interestOnlyPayment)
            txtPrincipal.text = CurrencyManager.format(this, monthlyPayment)
            txtOverPayments.text = CurrencyManager.format(this, interestOnly)
            txtInterest.text = CurrencyManager.format(this, totalInterest)

            val explanation = getString(
                R.string.interest_only_explain_html,
                CurrencyManager.format(this, loanAmount),
                Utils.decimalFormat.format(interestRate),
                CurrencyManager.format(this, interestOnlyPayment),
                CurrencyManager.format(this, monthlyPayment),
                CurrencyManager.format(this, interestOnly),
                CurrencyManager.format(this, totalInterest)
            )
            interestOnlyExplanationCard.visibility = View.VISIBLE
            interestOnlyExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        }

        btnStatistics.setOnClickListener {
            if (setValue()) {
                if (interestPeriod != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    terms += interestPeriod
                }
                commonModel.principalAmount = loanAmount
                commonModel.interestRate = interestRate
                commonModel.terms = terms
                commonModel.monthlyPayment = monthlyPayment
                commonModel.interestPeriod = interestPeriod
                commonModel.date = calendar.timeInMillis
                val intent = Intent(this, StatisticsActivity::class.java)
                intent.putExtra("InterestOnly", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = monthlyPayment,
            type = TransactionType.EXPENSE,
            title = getString(R.string.interest_only_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun setValue(): Boolean {
        return try {
            if (etLoanAmount.text.toString().trim().isNotEmpty() && etLoanAmount.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                try {
                    loanAmount = etLoanAmount.text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                if (etInterestRate.text.toString().trim().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    try {
                        interestRate = etInterestRate.text.toString().toDouble()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }

                    if (etInterestPeriod.text.toString().trim().isNotEmpty()) {
                        try {
                            interestPeriod = etInterestPeriod.text.toString().toDouble() * termInterestOnly
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    } else {
                        interestPeriod = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                    }

                    if (etTerms.text.toString().trim().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        terms = if (interestPeriod != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            (etTerms.text.toString().toDouble() * term) - interestPeriod
                        } else {
                            etTerms.text.toString().toDouble() * term
                        }
                        monthlyPayment = Utils.getMonthlyPayment(loanAmount, interestRate, terms)
                        true
                    } else {
                        etTerms.error = "Please fill out this field"
                        false
                    }
                } else {
                    etInterestRate.error = "Please fill out this field"
                    false
                }
            } else {
                etLoanAmount.error = "Please fill out this field"
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
            ShareUtil.print(this, rootLayout, getString(R.string.interest_only_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.interest_only_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.interest_only_calculator))
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
}
