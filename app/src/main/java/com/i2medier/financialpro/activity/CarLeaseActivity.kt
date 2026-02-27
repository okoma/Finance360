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
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.GraphModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

class CarLeaseActivity : AppCompatActivity() {
    private var monthlyLeasePayment = 0.0
    private var terms = 0.0
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private var downPayment = 0.0
    private lateinit var etDownPayment: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etOwedTrade: EditText
    private lateinit var etResidualValue: EditText
    private lateinit var etSaleTaxes: EditText
    private lateinit var etTerms: EditText
    private lateinit var etTradeAmount: EditText
    private lateinit var etVehiclePrice: EditText
    private lateinit var graphModelArrayList: ArrayList<GraphModel>
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private lateinit var pieChart: PieChart
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm: Spinner
    private var term = 1
    private lateinit var toolBar: Toolbar
    private lateinit var txtMonthlyPayment: TextView
    private lateinit var txtOverPayments: TextView
    private var vehiclePrice = 0.0
    private lateinit var currencySymbol: String
    private lateinit var currencyFormat: NumberFormat
    private lateinit var carLeaseExplanationCard: CardView
    private lateinit var carLeaseExplanation: TextView
    private lateinit var pieCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_lease)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.car_lease_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        graphModelArrayList = ArrayList()
        llResult = findViewById(R.id.llResult)
        toolBar = findViewById(R.id.toolBar)
        etVehiclePrice = findViewById(R.id.etVehiclePrice)
        etResidualValue = findViewById(R.id.etResidualValue)
        etInterestRate = findViewById(R.id.etInterestRate)
        etDownPayment = findViewById(R.id.etDownPayment)
        etTradeAmount = findViewById(R.id.etTradeAmount)
        etOwedTrade = findViewById(R.id.etOwedTrade)
        etTerms = findViewById(R.id.etTerms)
        etSaleTaxes = findViewById(R.id.etSaleTaxes)
        txtMonthlyPayment = findViewById(R.id.txtMonthlyPayment)
        txtOverPayments = findViewById(R.id.txtOverPayments)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm = findViewById(R.id.spTerm)
        pieChart = findViewById(R.id.piechart)
        pieCard = findViewById(R.id.pieCard)

        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        currencyFormat = NumberFormat.getCurrencyInstance()
        val decimalCurrencyFormat = currencyFormat as DecimalFormat
        val symbols = decimalCurrencyFormat.decimalFormatSymbols
        symbols.currencySymbol = currencySymbol
        decimalCurrencyFormat.decimalFormatSymbols = symbols
        decimalCurrencyFormat.maximumFractionDigits = 2

        findViewById<TextView>(R.id.txtCurrencyCarAmount).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyCarDownPay).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyCarResidual).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyCarPrice).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyCarOwedTrade).text = currencySymbol

        val carLeaseText = findViewById<TextView>(R.id.carLeaseText)
        val formattedText = String.format(getString(R.string.car_lease_description_html), currencySymbol)
        carLeaseText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        carLeaseExplanationCard = findViewById(R.id.carLeaseExplanationCard)
        carLeaseExplanation = findViewById(R.id.carLeaseExplanation)

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
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
                pieCard.visibility = View.VISIBLE

                txtMonthlyPayment.text = currencyFormat.format(monthlyLeasePayment)
                txtOverPayments.text = currencyFormat.format(monthlyLeasePayment * terms)

                val explanation = getString(
                    R.string.carLaese_explain_html,
                    currencyFormat.format(vehiclePrice),
                    currencyFormat.format(downPayment),
                    Utils.decimalFormat.format(etInterestRate.text.toString().toDouble()),
                    currencyFormat.format(etResidualValue.text.toString().ifEmpty { "0" }.toDouble()),
                    currencyFormat.format(monthlyLeasePayment),
                    terms.toInt().toString(),
                    currencyFormat.format(monthlyLeasePayment * terms)
                )

                carLeaseExplanationCard.visibility = View.VISIBLE
                carLeaseExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = monthlyLeasePayment,
            type = TransactionType.EXPENSE,
            title = getString(R.string.car_lease_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun setValue(): Boolean {
        return try {
            val downPaymentLocal = etDownPayment.text.toString().trim().ifEmpty { "0" }.toDouble()
            val tradeAmount = etTradeAmount.text.toString().trim().ifEmpty { "0" }.toDouble()
            val owedTrade = etOwedTrade.text.toString().trim().ifEmpty { "0" }.toDouble()
            val saleTaxes = etSaleTaxes.text.toString().trim().ifEmpty { "0" }.toDouble() / 100.0

            if (etVehiclePrice.text.toString().trim().isEmpty() || etVehiclePrice.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                etVehiclePrice.error = "Please fill out this field"
                return false
            }

            vehiclePrice = etVehiclePrice.text.toString().toDouble() - downPaymentLocal - tradeAmount + owedTrade
            val residualValue = etResidualValue.text.toString().trim().ifEmpty { "0" }.toDouble()

            if (etInterestRate.text.toString().trim().isEmpty() || etInterestRate.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                etInterestRate.error = "Please fill out this field"
                return false
            }

            val interestRate = etInterestRate.text.toString().toDouble()
            if (etTerms.text.toString().trim().isEmpty() || etTerms.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                etTerms.error = "Please fill out this field"
                return false
            }

            terms = etTerms.text.toString().toDouble() * term
            val depreciationCharge = (vehiclePrice - residualValue) / terms
            val moneyFactor = interestRate / 2400.0
            val financeCharge = (vehiclePrice + residualValue) * moneyFactor
            val basePayment = financeCharge + depreciationCharge
            val salesTaxPayment = saleTaxes * basePayment
            monthlyLeasePayment = basePayment + salesTaxPayment

            downPayment = downPaymentLocal
            setPieChart(depreciationCharge, salesTaxPayment, financeCharge)
            true
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun setPieChart(depreciation: Double, salesTax: Double, financeCharge: Double) {
        graphModelArrayList.clear()

        graphModelArrayList.add(
            GraphModel(
                getString(R.string.deprecation_charge) + "\n(" + currencyFormat.format(depreciation) + ")",
                depreciation,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)
            )
        )
        graphModelArrayList.add(
            GraphModel(
                getString(R.string.finance_charge) + "\n(" + currencyFormat.format(financeCharge) + ")",
                financeCharge,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)
            )
        )
        graphModelArrayList.add(
            GraphModel(
                getString(R.string.sales_tax) + "\n(" + currencyFormat.format(salesTax) + ")",
                salesTax,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor3)
            )
        )

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        for (model in graphModelArrayList) {
            entries.add(PieEntry(model.value.toFloat(), model.label))
            colors.add(model.color)
        }

        val pieDataSet = PieDataSet(entries, "")
        pieDataSet.colors = colors
        pieDataSet.sliceSpace = 3f
        pieDataSet.valueLinePart1OffsetPercentage = 80f
        pieDataSet.valueLinePart1Length = 0.2f
        pieDataSet.valueLinePart2Length = 0.4f
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val pieData = PieData(pieDataSet)
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorWhite))
        pieData.setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter())

        pieChart.data = pieData
        pieChart.setUsePercentValues(false)
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 45f
        pieChart.transparentCircleRadius = 50f
        pieChart.centerText = currencyFormat.format(monthlyLeasePayment)
        pieChart.setCenterTextSize(14f)
        pieChart.setEntryLabelColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorDark))
        pieChart.setEntryLabelTypeface(ResourcesCompat.getFont(this, R.font.regular))
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.isHighlightPerTapEnabled = true
        pieChart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        pieChart.invalidate()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.car_lease_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.car_lease_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.car_lease_calculator))
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
