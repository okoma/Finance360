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

class AmortizationActivity : AppCompatActivity() {
    private var amount = 0.0
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private lateinit var etAmount: EditText
    private lateinit var etDate: TextView
    private lateinit var etInterestRate: EditText
    private lateinit var etTerms: EditText
    private var interest = 0.0
    private lateinit var llResult: CardView
    private var mDay = 0
    private var mMonth = 0
    private var mYear = 0
    private var mMyDialog: AlertDialog? = null
    private var result = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtMonthlyPayment: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var txtTotalPayments: TextView
    private lateinit var currencySymbol: String
    private lateinit var explanationCard: CardView
    private lateinit var amortizationExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amortization)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.amortization_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        llResult = findViewById(R.id.llResult)
        toolBar = findViewById(R.id.toolBar)
        etAmount = findViewById(R.id.etAmount)
        etInterestRate = findViewById(R.id.etInterestRate)
        etTerms = findViewById(R.id.etTerms)
        etDate = findViewById(R.id.etDate)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm = findViewById(R.id.spTerm)
        txtMonthlyPayment = findViewById(R.id.txtMonthlyPayment)
        txtTotalPayments = findViewById(R.id.txtTotalPayments)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        calendar = Calendar.getInstance()

        etDate.text = AppConstant.getFormattedDate(calendar.timeInMillis, AppConstant.Date_FoRMAT_DDMMYY)

        findViewById<TextView>(R.id.txtCurrencyAmount).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyInterest).text = currencySymbol

        val amortizationText = findViewById<TextView>(R.id.amortizationText)
        val formattedText = String.format(getString(R.string.amortization_description_html), currencySymbol)
        amortizationText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        amortizationExplanation = findViewById(R.id.amortizationExplanation)
        explanationCard = findViewById(R.id.amortizationExplanationCard)

        etDate.setOnClickListener { startDateDialog() }

        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm.adapter = arrayAdapter
        term = 1
        spTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                if (obj == "Yr") {
                    term = 12
                } else if (obj == "Mo") {
                    term = 1
                }
            }
        }

        btnCalculate.setOnClickListener {
            if (calculate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
                txtMonthlyPayment.text = CurrencyManager.format(this, result)
                txtTotalPayments.text = CurrencyManager.format(this, result * terms)
                txtTotalInterest.text = String.format(
                    "%s%s",
                    currencySymbol,
                    Utils.decimalFormat.format((result * terms) - amount)
                )

                val explanation = getString(
                    R.string.amortization_explain_html,
                    CurrencyManager.format(this, result),
                    CurrencyManager.format(this, result * terms),
                    CurrencyManager.format(this, (result * terms) - amount)
                )
                explanationCard.visibility = View.VISIBLE
                amortizationExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            }
        }

        btnStatistics.setOnClickListener {
            if (calculate()) {
                commonModel.principalAmount = amount
                commonModel.terms = terms
                commonModel.interestRate = interest
                commonModel.monthlyPayment = result
                commonModel.date = calendar.timeInMillis
                val intent = Intent(this, StatisticsActivity::class.java)
                intent.putExtra("AutoLoan", commonModel)
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
            title = getString(R.string.amortization_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun calculate(): Boolean {
        return try {
            if (etAmount.text.toString().isNotEmpty() && etAmount.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                if (etInterestRate.text.toString().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    if (etTerms.text.toString().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        amount = etAmount.text.toString().toDouble()
                        interest = etInterestRate.text.toString().toDouble()
                        terms = etTerms.text.toString().toDouble() * term
                        val d2 = (interest / 100.0) / 12.0
                        if (interest == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            result = amount / terms
                            return true
                        }
                        val d3 = amount * d2
                        val d4 = d2 + 1.0
                        result = (d3 * Math.pow(d4, terms)) / (Math.pow(d4, terms) - 1.0)
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
                etAmount.error = "Please fill out this field"
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
            ShareUtil.print(this, rootLayout, getString(R.string.amortization_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.amortization_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.amortization_calculator))
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
