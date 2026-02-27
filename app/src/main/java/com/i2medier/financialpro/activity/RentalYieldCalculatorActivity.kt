package com.i2medier.financialpro.activity

import android.annotation.SuppressLint
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
import com.github.mikephil.charting.utils.Utils
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil

class RentalYieldCalculatorActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var etExpense: EditText
    private lateinit var etPropertyPrice: EditText
    private lateinit var etRent: EditText
    private lateinit var etVacancyRate: EditText
    private lateinit var item: String
    private lateinit var expenseItem: String
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private lateinit var rootLayout: ScrollView
    private lateinit var spinner: Spinner
    private lateinit var spinnerExpense: Spinner
    private lateinit var toolBar: Toolbar
    private lateinit var txtGrossYield: TextView
    private lateinit var txtNetYield: TextView
    private lateinit var currencySymbol: String
    private lateinit var rentalYieldExplanationCard: CardView
    private lateinit var rentalYieldExplanation: TextView
    private var lastAnnualNetIncome: Double = 0.0

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rental_yield_calculator)
        init()
        setUpToolbar()
        setSpinner()
        setExpenseSpinner()

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))
    }

    private fun setSpinner() {
        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.rent_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = this
    }

    private fun setExpenseSpinner() {
        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExpense.adapter = arrayAdapter
        spinnerExpense.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                expenseItem = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        spinnerExpense.setSelection(1)
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.rental_yield_calculator)
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        etPropertyPrice = findViewById(R.id.etPropertyPrice)
        etRent = findViewById(R.id.etRent)
        etVacancyRate = findViewById(R.id.etVacancyRate)
        etExpense = findViewById(R.id.etExpense)
        txtGrossYield = findViewById(R.id.txtGrossYield)
        txtNetYield = findViewById(R.id.txtNetYield)
        spinner = findViewById(R.id.spinner)
        spinnerExpense = findViewById(R.id.spinnerExpense)
        toolBar = findViewById(R.id.toolBar)
        btnCalculate = findViewById(R.id.btnCalculate)
        llResult = findViewById(R.id.llResult)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)

        findViewById<TextView>(R.id.txtCurrencySymbolPropertyPrice)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolRent)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolExpense)?.text = currencySymbol

        val rentalYieldText = findViewById<TextView>(R.id.rentalYieldText)
        val formattedText = String.format(getString(R.string.rental_yield_description_html), currencySymbol)
        rentalYieldText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        rentalYieldExplanationCard = findViewById(R.id.rentalYieldExplanationCard)
        rentalYieldExplanation = findViewById(R.id.rentalYieldExplanation)

        btnCalculate.setOnClickListener {
            com.i2medier.financialpro.util.AnalyticsTracker.logCalculatorCalculated(this, javaClass.simpleName)
            calculate()
        }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val type = if (lastAnnualNetIncome >= 0) TransactionType.INCOME else TransactionType.EXPENSE
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = kotlin.math.abs(lastAnnualNetIncome),
            type = type,
            title = getString(R.string.rental_yield_calculator),
            note = getString(R.string.rental_yield_calculator)
        )
    }

    @SuppressLint("SetTextI18n", "UseValueOf")
    private fun calculate() {
        var netYieldValue: Double? = null
        if (validationDetails()) {
            val propertyPrice = etPropertyPrice.text.toString().toDouble()
            val rent = etRent.text.toString().toDouble()
            val vacancyRate = etVacancyRate.text.toString().toDouble()
            val expense = etExpense.text.toString().toDouble()

            var annualExpenses = expense
            if (expenseItem == "Mo") annualExpenses = expense * 12.0

            if (propertyPrice > Utils.DOUBLE_EPSILON) {
                Log.e("str", "= $item")
                val c = when (item) {
                    "Weekly" -> 2
                    "Annually" -> 1
                    else -> 0
                }
                Log.e("c", "= $c")

                val grossYield: Double = when (c) {
                    0 -> (rent * 12.0 / propertyPrice) * 100.0
                    1 -> (rent / propertyPrice) * 100.0
                    else -> ((rent * 52.0) / propertyPrice) * 100.0
                }

                netYieldValue = when (c) {
                    0 -> ((((rent * 12.0) - ((rent * 12.0) * (vacancyRate / 100.0))) - annualExpenses) / propertyPrice) * 100.0
                    1 -> (((rent - (rent * (vacancyRate / 100.0))) - annualExpenses) / propertyPrice) * 100.0
                    else -> ((((rent * 52.0) - ((rent * 52.0) * (vacancyRate / 100.0))) - annualExpenses) / propertyPrice) * 100.0
                }

                txtGrossYield.text = com.i2medier.financialpro.util.Utils.decimalFormat.format(grossYield) + "%"
                txtNetYield.text = com.i2medier.financialpro.util.Utils.decimalFormat.format(netYieldValue) + "%"

                val annualRentalIncome = when (c) {
                    0 -> rent * 12.0
                    1 -> rent
                    else -> rent * 52.0
                }
                lastAnnualNetIncome = annualRentalIncome - annualExpenses

                val explanation = getString(
                    R.string.rental_yield_explain_html,
                    CurrencyManager.format(this, propertyPrice),
                    CurrencyManager.format(this, annualRentalIncome),
                    CurrencyManager.format(this, annualExpenses),
                    com.i2medier.financialpro.util.Utils.decimalFormat.format(grossYield) + "%",
                    com.i2medier.financialpro.util.Utils.decimalFormat.format(netYieldValue) + "%"
                )
                rentalYieldExplanationCard.visibility = View.VISIBLE
                rentalYieldExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Toast.makeText(this, "Property Price Value must be greater than or equal to 1", Toast.LENGTH_SHORT).show()
            }
            AppConstant.hideKeyboard(this)
            AppConstant.visibleResult(llResult)
            findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        item = parent?.getItemAtPosition(position).toString()
    }

    private fun validationDetails(): Boolean {
        return try {
            if (etPropertyPrice.text.toString().isNotEmpty() && etPropertyPrice.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                if (etRent.text.toString().isNotEmpty() && etRent.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                    if (etVacancyRate.text.toString().isNotEmpty() && etVacancyRate.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                        if (etExpense.text.toString().isNotEmpty() && etExpense.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                            true
                        } else {
                            etExpense.error = "Please fill out this field"
                            false
                        }
                    } else {
                        etVacancyRate.error = "Please fill out this field"
                        false
                    }
                } else {
                    etRent.error = "Please fill out this field"
                    false
                }
            } else {
                etPropertyPrice.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.rental_yield_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.rental_yield_calculator))
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
