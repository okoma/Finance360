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
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import kotlin.math.abs
import kotlin.math.max

class TaxCalculatorActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var etAnnualIncome: EditText
    private lateinit var etDeductions: EditText
    private lateinit var etTaxWithheld: EditText
    private lateinit var spFilingStatus: Spinner
    private lateinit var txtTaxableIncome: TextView
    private lateinit var txtEstimatedTax: TextView
    private lateinit var txtRefundOwed: TextView
    private lateinit var txtEffectiveTaxRate: TextView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var currencySymbol: String
    private var filingStatus: String = "Single"
    private lateinit var taxExplanationCard: CardView
    private lateinit var taxExplanation: TextView
    private var mMyDialog: AlertDialog? = null
    private var lastEstimatedTax: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tax_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
        setSpinner()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.tax_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setSpinner() {
        val filingStatuses = arrayOf("Single", "Married Filing Jointly", "Married Filing Separately", "Head of Household")
        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, filingStatuses)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFilingStatus.adapter = arrayAdapter
        spFilingStatus.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        filingStatus = parent?.getItemAtPosition(position).toString()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        toolBar = findViewById(R.id.toolBar)
        etAnnualIncome = findViewById(R.id.etAnnualIncome)
        etDeductions = findViewById(R.id.etDeductions)
        etTaxWithheld = findViewById(R.id.etTaxWithheld)
        spFilingStatus = findViewById(R.id.spFilingStatus)
        txtTaxableIncome = findViewById(R.id.txtTaxableIncome)
        txtEstimatedTax = findViewById(R.id.txtEstimatedTax)
        txtRefundOwed = findViewById(R.id.txtRefundOwed)
        txtEffectiveTaxRate = findViewById(R.id.txtEffectiveTaxRate)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolAnnualIncome)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolDeductions)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolTaxWithheld)?.text = currencySymbol

        val taxText = findViewById<TextView>(R.id.taxText)
        val formattedText = String.format(getString(R.string.tax_description_html), currencySymbol)
        taxText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        taxExplanationCard = findViewById(R.id.taxExplanationCard)
        taxExplanation = findViewById(R.id.taxExplanation)

        clickListeners()
    }

    private fun clickListeners() {
        btnCalculate.setOnClickListener {
            com.i2medier.financialpro.util.AnalyticsTracker.logCalculatorCalculated(this, javaClass.simpleName)
            if (calculate()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
            }
        }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = lastEstimatedTax,
            type = TransactionType.EXPENSE,
            title = getString(R.string.tax_calculator),
            note = getString(R.string.estimated_tax)
        )
    }

    private fun calculate(): Boolean {
        return try {
            if (etAnnualIncome.text.toString().isEmpty() || etAnnualIncome.text.toString().toDouble() <= 0) {
                etAnnualIncome.error = "Please enter valid income"
                return false
            }

            val annualIncome = etAnnualIncome.text.toString().toDouble()
            val deductions = getDoubleValue(etDeductions)
            val taxWithheld = getDoubleValue(etTaxWithheld)

            val standardDeduction = when (filingStatus) {
                "Single" -> 13850.0
                "Married Filing Jointly" -> 27700.0
                "Married Filing Separately" -> 13850.0
                "Head of Household" -> 20800.0
                else -> 0.0
            }

            val totalDeductions = max(deductions, standardDeduction)
            val taxableIncome = max(0.0, annualIncome - totalDeductions)
            val estimatedTax = calculateTax(taxableIncome, filingStatus)
            lastEstimatedTax = estimatedTax
            val refundOwed = taxWithheld - estimatedTax
            val effectiveTaxRate = (estimatedTax / annualIncome) * 100

            txtTaxableIncome.text = CurrencyManager.format(this, taxableIncome)
            txtEstimatedTax.text = CurrencyManager.format(this, estimatedTax)
            txtRefundOwed.text = if (refundOwed >= 0) {
                "$currencySymbol${Utils.decimalFormat.format(refundOwed)} (Refund)"
            } else {
                "$currencySymbol${Utils.decimalFormat.format(abs(refundOwed))} (Owed)"
            }
            txtEffectiveTaxRate.text = Utils.decimalFormat.format(effectiveTaxRate) + "%"

            val refundOwedText = if (refundOwed >= 0) "receive a refund of" else "owe"
            val refundOwedValue = CurrencyManager.format(this, abs(refundOwed))

            val explanation = getString(
                R.string.tax_explain_html,
                CurrencyManager.format(this, annualIncome),
                filingStatus,
                CurrencyManager.format(this, totalDeductions),
                CurrencyManager.format(this, taxableIncome),
                CurrencyManager.format(this, estimatedTax),
                CurrencyManager.format(this, taxWithheld),
                refundOwedValue,
                Utils.decimalFormat.format(effectiveTaxRate),
                refundOwedText
            )
            taxExplanationCard.visibility = View.VISIBLE
            taxExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            true
        } catch (_: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun calculateTax(taxableIncome: Double, status: String): Double {
        return when (status) {
            "Single" -> when {
                taxableIncome > 578125 -> 174238.25 + (taxableIncome - 578125) * 0.37
                taxableIncome > 231250 -> 52832.50 + (taxableIncome - 231250) * 0.35
                taxableIncome > 182050 -> 37304.00 + (taxableIncome - 182050) * 0.32
                taxableIncome > 95350 -> 16290.00 + (taxableIncome - 95350) * 0.24
                taxableIncome > 44725 -> 5147.00 + (taxableIncome - 44725) * 0.22
                taxableIncome > 11000 -> 1100.00 + (taxableIncome - 11000) * 0.12
                else -> taxableIncome * 0.10
            }
            "Married Filing Jointly" -> when {
                taxableIncome > 693750 -> 186601.50 + (taxableIncome - 693750) * 0.37
                taxableIncome > 462500 -> 105664.00 + (taxableIncome - 462500) * 0.35
                taxableIncome > 364200 -> 74208.00 + (taxableIncome - 364200) * 0.32
                taxableIncome > 190750 -> 32580.00 + (taxableIncome - 190750) * 0.24
                taxableIncome > 89450 -> 10294.00 + (taxableIncome - 89450) * 0.22
                taxableIncome > 22000 -> 2200.00 + (taxableIncome - 22000) * 0.12
                else -> taxableIncome * 0.10
            }
            "Married Filing Separately" -> when {
                taxableIncome > 346875 -> 93300.75 + (taxableIncome - 346875) * 0.37
                taxableIncome > 231250 -> 52832.00 + (taxableIncome - 231250) * 0.35
                taxableIncome > 182100 -> 37104.00 + (taxableIncome - 182100) * 0.32
                taxableIncome > 95375 -> 16290.00 + (taxableIncome - 95375) * 0.24
                taxableIncome > 44725 -> 5147.00 + (taxableIncome - 44725) * 0.22
                taxableIncome > 11000 -> 1100.00 + (taxableIncome - 11000) * 0.12
                else -> taxableIncome * 0.10
            }
            "Head of Household" -> when {
                taxableIncome > 578100 -> 172623.50 + (taxableIncome - 578100) * 0.37
                taxableIncome > 231250 -> 52222.00 + (taxableIncome - 231250) * 0.35
                taxableIncome > 190750 -> 32580.00 + (taxableIncome - 190750) * 0.32
                taxableIncome > 95350 -> 16290.00 + (taxableIncome - 95350) * 0.24
                taxableIncome > 59850 -> 6868.00 + (taxableIncome - 59850) * 0.22
                taxableIncome > 15700 -> 1570.00 + (taxableIncome - 15700) * 0.12
                else -> taxableIncome * 0.10
            }
            else -> 0.0
        }
    }

    private fun getDoubleValue(editText: EditText): Double {
        val text = editText.text.toString()
        return if (text.isEmpty()) 0.0 else text.toDouble()
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.tax_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.tax_calculator))
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
