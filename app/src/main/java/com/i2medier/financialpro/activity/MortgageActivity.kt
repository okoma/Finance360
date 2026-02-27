package com.i2medier.financialpro.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
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

class MortgageActivity : AppCompatActivity() {
    private var PMI = 0.0
    private var TaxIns = 0.0
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnAddToPlanner: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private var downPayment = 0.0
    private lateinit var etDate: TextView
    private lateinit var etDownPayment: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etPMI: EditText
    private lateinit var etPropertyInsurance: EditText
    private lateinit var etPropertyTax: EditText
    private lateinit var etPurchasePrice: EditText
    private lateinit var etTerms: EditText
    private var interestRate = 0.0
    private lateinit var llResult: CardView
    private var mDay = 0
    private var mMonth = 0
    private var mYear = 0
    private var mMyDialog: AlertDialog? = null
    private lateinit var monthModels: ArrayList<MonthModel>
    private var monthlyPayment = 0.0
    private var propertyInsurance = 0.0
    private var propertyTax = 0.0
    private var purchasePrice = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtLoanAmount: TextView
    private lateinit var txtMonthlyPayment: TextView
    private lateinit var txtOverPayments: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var currencySymbol: String
    private lateinit var mortgageExplanationCard: CardView
    private lateinit var mortgageExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mortgage)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.mortgage_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        monthModels = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        etPurchasePrice = findViewById(R.id.etPurchasePrice)
        etDownPayment = findViewById(R.id.etDownPayment)
        etTerms = findViewById(R.id.etTerms)
        etInterestRate = findViewById(R.id.etInterestRate)
        etPropertyTax = findViewById(R.id.etPropertyTax)
        etPMI = findViewById(R.id.etPMI)
        etPropertyInsurance = findViewById(R.id.etPropertyInsurance)
        etDate = findViewById(R.id.etDate)
        txtMonthlyPayment = findViewById(R.id.txtMonthlyPayment)
        txtLoanAmount = findViewById(R.id.txtLoanAmount)
        txtOverPayments = findViewById(R.id.txtOverPayments)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        llResult = findViewById(R.id.llResult)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm = findViewById(R.id.spTerm)
        calendar = Calendar.getInstance()

        findViewById<TextView>(R.id.txtCurrencyMortgage1).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyMortgage2).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyMortgage3).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyMortgage4).text = currencySymbol

        val mortgageText = findViewById<TextView>(R.id.mortgageText)
        val formattedText = String.format(getString(R.string.mortgage_description_html), currencySymbol)
        mortgageText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        mortgageExplanationCard = findViewById(R.id.mortgageExplanationCard)
        mortgageExplanation = findViewById(R.id.mortgageExplanation)

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
                Log.e("obj", obj)
                term = if (obj == "Yr") 12 else 1
            }
        }

        btnCalculate.setOnClickListener {
            com.i2medier.financialpro.util.AnalyticsTracker.logCalculatorCalculated(this, javaClass.simpleName)
            if (setValue()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                btnAddToPlanner.visibility = View.VISIBLE

                val totalMonthlyPayment = monthlyPayment + TaxIns
                txtMonthlyPayment.text = CurrencyManager.format(this, totalMonthlyPayment)
                txtLoanAmount.text = CurrencyManager.format(this, Utils.Principal)
                txtOverPayments.text = CurrencyManager.format(this, Utils.Paid)
                txtTotalInterest.text = CurrencyManager.format(this, Utils.Interest)

                val termText = if (term == 12) "years" else "months"
                val explanation = getString(
                    R.string.mortgage_explain_html,
                    CurrencyManager.format(this, purchasePrice),
                    CurrencyManager.format(this, downPayment),
                    Utils.decimalFormat.format(interestRate),
                    (terms / term).toInt().toString() + " " + termText,
                    CurrencyManager.format(this, totalMonthlyPayment),
                    CurrencyManager.format(this, Utils.Principal),
                    CurrencyManager.format(this, Utils.Paid),
                    CurrencyManager.format(this, Utils.Interest)
                )
                mortgageExplanationCard.visibility = View.VISIBLE
                mortgageExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            }
        }

        btnStatistics.setOnClickListener {
            if (setValue()) {
                commonModel.principalAmount = purchasePrice
                commonModel.terms = terms
                commonModel.interestRate = interestRate
                commonModel.monthlyPayment = monthlyPayment
                commonModel.TaxIns = TaxIns
                commonModel.PMI = PMI
                commonModel.date = calendar.timeInMillis
                commonModel.downPayment = downPayment
                val intent = Intent(this, MortgageStatisticsActivity::class.java)
                intent.putExtra("Mortgage", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }
    }

    private fun setValue(): Boolean {
        return try {
            downPayment = if (etDownPayment.text.toString().trim().isNotEmpty()) etDownPayment.text.toString().toDouble() else com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            propertyTax = if (etPropertyTax.text.toString().trim().isNotEmpty()) etPropertyTax.text.toString().toDouble() / 12.0 else com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            propertyInsurance = if (etPropertyInsurance.text.toString().trim().isNotEmpty()) etPropertyInsurance.text.toString().toDouble() / 12.0 else com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

            if (etPurchasePrice.text.toString().trim().isNotEmpty() && etPurchasePrice.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                purchasePrice = etPurchasePrice.text.toString().toDouble()
                if (etTerms.text.toString().trim().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    terms = etTerms.text.toString().toDouble() * term
                    if (etInterestRate.text.toString().trim().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        interestRate = etInterestRate.text.toString().toDouble()
                        PMI = if (etPMI.text.toString().trim().isNotEmpty()) {
                            (etPMI.text.toString().toDouble() * (purchasePrice - downPayment)) / 1200.0
                        } else {
                            com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                        }

                        TaxIns = propertyTax + propertyInsurance
                        monthlyPayment = Utils.getMonthlyPayment(purchasePrice - downPayment, interestRate, terms)
                        Log.d("Month", ">>>" + Utils.getMonthlyPayment(purchasePrice, interestRate, terms))

                        if ((Utils.Principal != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON && Utils.Interest != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON && Utils.Paid != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) || Utils.mTaxInsPMI != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            Utils.Principal = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                            Utils.Interest = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                            Utils.Paid = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                            Utils.mTaxInsPMI = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                        }

                        if (monthModels.isNotEmpty()) {
                            monthModels.clear()
                        }
                        monthModels = Utils.getMonthlyMortgage(
                            purchasePrice,
                            terms,
                            interestRate,
                            monthlyPayment,
                            TaxIns,
                            PMI,
                            Utils.CALDATE(etDate.text.toString()),
                            downPayment
                        )
                        true
                    } else {
                        etInterestRate.error = "Please fill out this field"
                        false
                    }
                } else {
                    etTerms.error = "Please fill out this field"
                    false
                }
            } else {
                etPurchasePrice.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.mortgage_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.mortgage_calculator))
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
        val totalMonthlyPayment = monthlyPayment + TaxIns
        if (totalMonthlyPayment <= 0.0) {
            Toast.makeText(this, "No amount to add", Toast.LENGTH_SHORT).show()
            return
        }

        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = totalMonthlyPayment,
            type = TransactionType.EXPENSE,
            note = "Estimated monthly mortgage payment",
            title = getString(R.string.mortgage_calculator)
        )
    }
}
