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
import android.widget.LinearLayout
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
import com.github.mikephil.charting.charts.PieChart
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.model.GraphModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import java.util.Calendar

class CompoundInterestActivity : AppCompatActivity() {
    private var initialInvestment = 0.0
    private var interestRate = 0.0
    private var regularInvestment = 0.0
    private var regularInvestmentValue = 0.0
    private var terms = 0.0
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private var compoundValue = 12
    private lateinit var etDate: TextView
    private lateinit var etInitialInvestment: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etRegularInvestment: EditText
    private lateinit var etTerms: EditText
    private lateinit var graphModelArrayList: ArrayList<GraphModel>
    private lateinit var graphlayot: LinearLayout
    private var interestValue = 0.0
    private lateinit var llResult: CardView
    private var mDay = 0
    private var mMonth = 0
    private var mMyDialog: AlertDialog? = null
    private var mYear = 0
    private lateinit var pieChart: PieChart
    private lateinit var rootLayout: ScrollView
    private lateinit var spCompound: Spinner
    private lateinit var spTerm: Spinner
    private var term = 1
    private lateinit var toolBar: Toolbar
    private lateinit var txtInitialInvestment: TextView
    private lateinit var txtInterest: TextView
    private lateinit var txtRegularInvestment: TextView
    private lateinit var currencySymbol: String
    private var compoundExplanationCard: CardView? = null
    private var compoundExplanation: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compound_interest)

        val adAdmob = AdAdmob(this)
        findViewById<RelativeLayout?>(R.id.banner)?.let { banner ->
            adAdmob.BannerAd(banner, this)
        }

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.compound_interest_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        graphModelArrayList = ArrayList()
        commonModel = CommonModel()
        toolBar = findViewById(R.id.toolBar)
        etInitialInvestment = findViewById(R.id.etInitialInvestment)
        etInterestRate = findViewById(R.id.etInterestRate)
        etRegularInvestment = findViewById(R.id.etRegularInvestment)
        etTerms = findViewById(R.id.etTerms)
        etDate = findViewById(R.id.etDate)
        spCompound = findViewById(R.id.spCompound)
        spTerm = findViewById(R.id.spTerm)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)
        txtInitialInvestment = findViewById(R.id.txtInitialInvestment)
        txtInterest = findViewById(R.id.txtInterest)
        txtRegularInvestment = findViewById(R.id.txtRegularInvestment)
        pieChart = findViewById(R.id.piechart)
        calendar = Calendar.getInstance()

        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        findViewById<TextView?>(R.id.txtCurrencyCompound)?.text = currencySymbol
        findViewById<TextView?>(R.id.txtCurrencyRegular)?.text = currencySymbol

        findViewById<TextView?>(R.id.compoundInterestText)?.let { compoundInterestText ->
            val formattedText = String.format(getString(R.string.compound_interest_description_html), currencySymbol)
            compoundInterestText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
        }

        compoundExplanationCard = findViewById(R.id.compoundExplanationCard)
        compoundExplanation = findViewById(R.id.compoundExplanation)

        etDate.text = AppConstant.getFormattedDate(calendar.timeInMillis, AppConstant.Date_FoRMAT_DDMMYY)
        etDate.setOnClickListener { startDateDialog() }

        val compoundAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.compound_array))
        compoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCompound.adapter = compoundAdapter
        compoundValue = 12

        val termAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm.adapter = termAdapter
        term = 1

        spTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                term = if (obj == "Yr") 12 else 1
            }
        }

        spCompound.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                compoundValue = when (obj) {
                    "Monthly" -> 12
                    "Quarterly" -> 4
                    "Semi-Annually" -> 2
                    "Annually" -> 1
                    else -> 12
                }
            }
        }

        btnCalculate.setOnClickListener {
            if (getInterestRate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
                txtInitialInvestment.text = CurrencyManager.format(this, initialInvestment)
                txtRegularInvestment.text = CurrencyManager.format(this, regularInvestmentValue)
                txtInterest.text = CurrencyManager.format(this, interestValue)

                val regularInvestmentText = if (regularInvestmentValue > 0) {
                    CurrencyManager.format(this, regularInvestmentValue)
                } else {
                    ""
                }
                val regularContributionLine = if (regularInvestmentText.isEmpty()) {
                    ""
                } else {
                    "<b>Regular Contributions:</b> $regularInvestmentText<br/>"
                }
                val explanation = getString(
                    R.string.compound_interest_explain_html,
                    CurrencyManager.format(this, initialInvestment),
                    if (regularInvestmentText.isEmpty()) "" else " with regular contributions of $regularInvestmentText",
                    Utils.decimalFormat.format(interestRate),
                    regularContributionLine,
                    CurrencyManager.format(this, interestValue)
                )
                compoundExplanationCard?.visibility = View.VISIBLE
                compoundExplanation?.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            }
        }

        btnStatistics.setOnClickListener {
            if (getInterestRate()) {
                commonModel.principalAmount = initialInvestment
                commonModel.terms = terms * term
                commonModel.interestRate = interestRate
                commonModel.monthlyPayment = regularInvestment
                commonModel.date = calendar.timeInMillis
                val intent = Intent(this, CompoundStatisticsActivity::class.java)
                intent.putExtra("CompoundInterest", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val totalValue = initialInvestment + regularInvestmentValue + interestValue
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = totalValue,
            type = TransactionType.SAVING,
            title = getString(R.string.compound_interest_calculator),
            note = getString(R.string.interest)
        )
    }

    private fun getInterestRate(): Boolean {
        return try {
            if (etInitialInvestment.text.toString().trim().isNotEmpty() && etInitialInvestment.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                initialInvestment = etInitialInvestment.text.toString().toDouble()
                if (etInterestRate.text.toString().trim().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    interestRate = etInterestRate.text.toString().toDouble()
                    if (etTerms.text.toString().trim().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        terms = etTerms.text.toString().toDouble()
                        val regularInvestmentStr = etRegularInvestment.text.toString().trim()
                        regularInvestment = if (regularInvestmentStr.isNotEmpty()) {
                            try {
                                val value = regularInvestmentStr.toDouble()
                                if (value < 0) 0.0 else value
                            } catch (_: NumberFormatException) {
                                0.0
                            }
                        } else {
                            0.0
                        }

                        val annualRate = interestRate / 100.0
                        val totalMonths = terms * term
                        val years = totalMonths / 12.0
                        val compoundsPerYear = compoundValue.toDouble()
                        val contributionsPerYear = 12.0

                        val principalGrowthFactor = Math.pow(1.0 + (annualRate / compoundsPerYear), compoundsPerYear * years)
                        val principalFutureValue = initialInvestment * principalGrowthFactor

                        val contributionPeriods = years * contributionsPerYear
                        regularInvestmentValue = if (regularInvestment > com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON && contributionPeriods > 0) {
                            regularInvestment * contributionPeriods
                        } else {
                            0.0
                        }

                        val contributionsFutureValue = if (regularInvestment > com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON && contributionPeriods > 0) {
                            val effectiveContributionRate = Math.pow(
                                1.0 + (annualRate / compoundsPerYear),
                                compoundsPerYear / contributionsPerYear
                            ) - 1.0

                            if (Math.abs(effectiveContributionRate) < 1e-12) {
                                regularInvestment * contributionPeriods
                            } else {
                                regularInvestment * ((Math.pow(1.0 + effectiveContributionRate, contributionPeriods) - 1.0) / effectiveContributionRate)
                            }
                        } else {
                            0.0
                        }

                        val totalFutureValue = principalFutureValue + contributionsFutureValue
                        interestValue = totalFutureValue - initialInvestment - regularInvestmentValue

                        setPieChart(initialInvestment, regularInvestmentValue, interestValue)
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
                etInitialInvestment.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun setPieChart(initial: Double, regular: Double, interest: Double) {
        if (graphModelArrayList.isNotEmpty()) {
            graphModelArrayList.clear()
        }

        graphModelArrayList.add(
            GraphModel(
                getString(R.string.initial_investment) + "\n(" + Utils.decimalFormat.format(initial) + ")",
                initial,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)
            )
        )

        if (regular > com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            graphModelArrayList.add(
                GraphModel(
                    getString(R.string.regular_investment) + "\n(" + Utils.decimalFormat.format(regular) + ")",
                    regular,
                    androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)
                )
            )
        }

        graphModelArrayList.add(
            GraphModel(
                getString(R.string.interest) + "\n(" + Utils.decimalFormat.format(interest) + ")",
                interest,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor3)
            )
        )

        val total = initial + regular + interest
        GraphUtils(pieChart, graphModelArrayList, applicationContext).setupPieData(Utils.decimalFormat.format(total))
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.compound_interest_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.compound_interest_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.compound_interest_calculator))
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
        mYear = calendar.get(Calendar.YEAR)
        mMonth = calendar.get(Calendar.MONTH)
        mDay = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.YEAR, year)
            etDate.text = AppConstant.getFormattedDate(calendar.timeInMillis, AppConstant.Date_FoRMAT_DDMMYY)
        }, mYear, mMonth, mDay).show()
    }
}
