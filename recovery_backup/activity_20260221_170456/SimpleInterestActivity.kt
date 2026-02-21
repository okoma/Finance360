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

class SimpleInterestActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var calendar: Calendar
    private lateinit var commonModel: CommonModel
    private lateinit var etDate: TextView
    private lateinit var etInterestRate: EditText
    private lateinit var etPrincipal: EditText
    private lateinit var etTerms: EditText
    private lateinit var graphModelArrayList: ArrayList<GraphModel>
    private lateinit var graphlayot: LinearLayout
    private var interestAmount = 0.0
    private var interestRate = 0.0
    private lateinit var llResult: CardView
    private var mDay = 0
    private var mMonth = 0
    private var mMyDialog: AlertDialog? = null
    private var mYear = 0
    private lateinit var pieChart: PieChart
    private var principal = 0.0
    private var principalAmount = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private var terms = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest: TextView
    private lateinit var txtPrincipal: TextView
    private lateinit var currencySymbol: String
    private lateinit var simpleInterestExplanationCard: CardView
    private lateinit var simpleInterestExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_interest)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById(R.id.banner), this)

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.simple_interest_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        graphModelArrayList = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        etPrincipal = findViewById(R.id.etPrincipal)
        etInterestRate = findViewById(R.id.etInterestRate)
        etTerms = findViewById(R.id.etTerms)
        etDate = findViewById(R.id.etDate)
        graphlayot = findViewById(R.id.graphlayot)
        txtPrincipal = findViewById(R.id.txtPrincipal)
        txtInterest = findViewById(R.id.txtInterest)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm = findViewById(R.id.spTerm)
        pieChart = findViewById(R.id.piechart)
        llResult = findViewById(R.id.llResult)
        calendar = Calendar.getInstance()

        findViewById<TextView>(R.id.txtCurrencySymbolPrincipal).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolInterest).text = currencySymbol

        val simpleInterestText = findViewById<TextView>(R.id.simpleInterestText)
        val formattedText = String.format(getString(R.string.simple_interest_description_html), currencySymbol)
        simpleInterestText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        simpleInterestExplanationCard = findViewById(R.id.simpleInterestExplanationCard)
        simpleInterestExplanation = findViewById(R.id.simpleInterestExplanation)

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
            if (setValue()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                AppConstant.visibleGraph(graphlayot)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
            }
            val totalAmount = principal + interestAmount
            txtPrincipal.text = CurrencyManager.format(this, principal)
            txtInterest.text = CurrencyManager.format(this, interestAmount)

            val originalTerms = etTerms.text.toString().toDouble()
            val periodText = if (term == 12) {
                "${originalTerms.toInt()} months (${Utils.decimalFormat.format(terms)} years)"
            } else {
                Utils.decimalFormat.format(terms) + " years"
            }
            val explanation = getString(
                R.string.simple_interest_explain_html,
                CurrencyManager.format(this, principal),
                Utils.decimalFormat.format(interestRate * 100),
                periodText,
                CurrencyManager.format(this, interestAmount),
                CurrencyManager.format(this, totalAmount)
            )
            simpleInterestExplanationCard.visibility = View.VISIBLE
            simpleInterestExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        }

        btnStatistics.setOnClickListener {
            if (setValue()) {
                commonModel.principalAmount = principal
                commonModel.terms = terms
                commonModel.interestAmount = principalAmount
                commonModel.year = calendar.get(Calendar.YEAR)
                val intent = Intent(this, SimpleInterestStatisticsActivity::class.java)
                intent.putExtra("SimpleInterest", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val totalAmount = principal + interestAmount
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = totalAmount,
            type = TransactionType.SAVING,
            title = getString(R.string.simple_interest_calculator),
            note = getString(R.string.interest)
        )
    }

    private fun setValue(): Boolean {
        return try {
            if (etPrincipal.text.toString().trim().isNotEmpty() && etPrincipal.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                principal = etPrincipal.text.toString().toDouble()
                if (etInterestRate.text.toString().trim().isNotEmpty() && etInterestRate.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    interestRate = etInterestRate.text.toString().toDouble() / 100.0
                    if (etTerms.text.toString().trim().isNotEmpty() && etTerms.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        val doubleValue = etTerms.text.toString().toDouble()
                        terms = doubleValue / term
                        principalAmount = principal * interestRate
                        interestAmount = principalAmount * terms
                        setPieChart(principal, interestAmount)
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
                etPrincipal.error = "Please fill out this field"
                false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun setPieChart(principal: Double, interest: Double) {
        if (graphModelArrayList.isNotEmpty()) {
            graphModelArrayList.clear()
        }

        graphModelArrayList.add(
            GraphModel(
                getString(R.string.principalamount) + "\n(" + Utils.decimalFormat.format(principal) + ")",
                principal,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)
            )
        )

        graphModelArrayList.add(
            GraphModel(
                getString(R.string.interestamount) + "\n(" + Utils.decimalFormat.format(interest) + ")",
                interest,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)
            )
        )

        val total = principal + interest
        GraphUtils(pieChart, graphModelArrayList, applicationContext).setupPieData(Utils.decimalFormat.format(total))
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.simple_interest_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.simple_interest_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.simple_interest_calculator))
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
