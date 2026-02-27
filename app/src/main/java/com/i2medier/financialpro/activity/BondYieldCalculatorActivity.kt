package com.i2medier.financialpro.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.InputFilter
import android.text.Spanned
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
import com.github.mikephil.charting.utils.Utils
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil

class BondYieldCalculatorActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private var annualCouponPayment = 0.0
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private var couponRate = 0.0
    private var currentPrice = 0.0
    private lateinit var etCouponRate: EditText
    private lateinit var etCurrentPrice: EditText
    private lateinit var etParValue: EditText
    private lateinit var etYearsTo: EditText
    private lateinit var item: String
    private lateinit var currencySymbol: String
    private lateinit var linearCouponrate: LinearLayout
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private var parValue = 0.0
    private var result = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spinner: Spinner
    private lateinit var toolBar: Toolbar
    private lateinit var txtCurrentYield: TextView
    private lateinit var txtYieldToMaturity: TextView
    private var yearsToMaturity = 0.0
    private var yield = 0.0
    private lateinit var bondYieldExplanationCard: CardView
    private lateinit var bondYieldExplanation: TextView

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bond_yield_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
        setSpinner()
    }

    private fun setSpinner() {
        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.payment_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = this
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.bond_yield_calculator)
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
        etCurrentPrice = findViewById(R.id.etCurrentPrice)
        etParValue = findViewById(R.id.etParValue)
        etCouponRate = findViewById(R.id.etCouponRate)
        etYearsTo = findViewById(R.id.etYearsTo)
        etYearsTo.filters = arrayOf(InputFilterMinMax("1", "100"))
        txtCurrentYield = findViewById(R.id.txtCurrentYield)
        txtYieldToMaturity = findViewById(R.id.txtYieldToMaturity)
        linearCouponrate = findViewById(R.id.linearCouponrate)
        spinner = findViewById(R.id.spinner)
        toolBar = findViewById(R.id.toolBar)
        btnCalculate = findViewById(R.id.btnCalculate)
        llResult = findViewById(R.id.llResult)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)

        findViewById<TextView>(R.id.txtCurrencyBondYield1)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyBondYield2)?.text = currencySymbol

        val bondYieldText = findViewById<TextView>(R.id.bondYieldText)
        val formattedText = String.format(getString(R.string.bond_yield_description_html), currencySymbol)
        bondYieldText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        bondYieldExplanationCard = findViewById(R.id.bondYieldExplanationCard)
        bondYieldExplanation = findViewById(R.id.bondYieldExplanation)

        btnCalculate.setOnClickListener { calculate() }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = annualCouponPayment,
            type = TransactionType.INCOME,
            title = getString(R.string.bond_yield_calculator),
            note = getString(R.string.bond_yield_calculator)
        )
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        item = parent?.getItemAtPosition(position).toString()
        checkVisibility()
    }

    @SuppressLint("SetTextI18n", "UseValueOf")
    private fun calculate() {
        if (validationDetails()) {
            try {
                currentPrice = etCurrentPrice.text.toString().toDouble()
                parValue = etParValue.text.toString().toDouble()
                yearsToMaturity = etYearsTo.text.toString().toDouble()
            } catch (e: Exception) {
                e.printStackTrace()
                yearsToMaturity = Utils.DOUBLE_EPSILON
                currentPrice = Utils.DOUBLE_EPSILON
                parValue = Utils.DOUBLE_EPSILON
            }
            if (!item.equals("None", ignoreCase = true)) {
                couponRate = etCouponRate.text.toString().toDouble()
            }
            if (!item.equals("None", ignoreCase = true)) {
                if (couponRate > 1.0) couponRate /= 100.0
                annualCouponPayment = couponRate * parValue
                yield = annualCouponPayment / currentPrice
            } else {
                yield = Utils.DOUBLE_EPSILON
            }

            if (yearsToMaturity >= 1.0) {
                if (item.equals("None", ignoreCase = true)) {
                    result = if (currentPrice > 0 && parValue > 0) {
                        Math.pow(parValue / currentPrice, 1.0 / yearsToMaturity) - 1.0
                    } else {
                        Utils.DOUBLE_EPSILON
                    }
                } else {
                    result = (annualCouponPayment + ((parValue - currentPrice) / yearsToMaturity)) / ((parValue + currentPrice) / 2.0)
                }
            } else {
                Toast.makeText(this, "You must put value greater than or equal to 1!", Toast.LENGTH_SHORT).show()
                result = Utils.DOUBLE_EPSILON
            }
            txtCurrentYield.text = com.i2medier.financialpro.util.Utils.decimalFormat.format(yield * 100) + "%"
            txtYieldToMaturity.text = if (result > 0) com.i2medier.financialpro.util.Utils.decimalFormat.format(result * 100) + "%" else "N/A"

            var couponInfo = ""
            if (!item.equals("None", ignoreCase = true)) {
                couponInfo = "- Coupon Rate: " + com.i2medier.financialpro.util.Utils.decimalFormat.format(couponRate * 100) + "%<br/>"
            }
            val interpretation = "This bond offers a positive yield, making it a potentially attractive investment opportunity. Consider the bond's credit risk and compare with other investment options."
            val ytmValue = if (result > 0) com.i2medier.financialpro.util.Utils.decimalFormat.format(result * 100) else "N/A"

            val explanation = getString(
                R.string.bond_yield_explain_html,
                CurrencyManager.format(this, currentPrice),
                CurrencyManager.format(this, parValue),
                couponInfo,
                yearsToMaturity.toInt().toString(),
                com.i2medier.financialpro.util.Utils.decimalFormat.format(yield * 100),
                ytmValue,
                interpretation
            )
            bondYieldExplanationCard.visibility = View.VISIBLE
            bondYieldExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)

            AppConstant.hideKeyboard(this)
            AppConstant.visibleResult(llResult)
            findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
        }
    }

    private fun checkVisibility() {
        linearCouponrate.visibility = if (item.equals("None", ignoreCase = true)) View.GONE else View.VISIBLE
    }

    private fun validationDetails(): Boolean {
        return try {
            if (etCurrentPrice.text.toString().isNotEmpty() && etCurrentPrice.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                if (etParValue.text.toString().isNotEmpty() && etParValue.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                    if (!item.equals("None", ignoreCase = true) && (etCouponRate.text.toString().isEmpty() || etCouponRate.text.toString().toDouble() == Utils.DOUBLE_EPSILON)) {
                        etCouponRate.error = "Please fill out this field"
                        false
                    } else if (etYearsTo.text.toString().isNotEmpty() && etYearsTo.text.toString().toDouble() != Utils.DOUBLE_EPSILON) {
                        true
                    } else {
                        etYearsTo.error = "Please fill out this field"
                        false
                    }
                } else {
                    etParValue.error = "Please fill out this field"
                    false
                }
            } else {
                etCurrentPrice.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    inner class InputFilterMinMax(private val min: Int, private val max: Int) : InputFilter {
        constructor(min: String, max: String) : this(min.toInt(), max.toInt())

        private fun isInRange(a: Int, b: Int, c: Int): Boolean {
            return if (b > a) c in a..b else c in b..a
        }

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            return try {
                if (isInRange(min, max, (dest.toString() + source.toString()).toInt())) null else ""
            } catch (_: NumberFormatException) {
                ""
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!com.i2medier.financialpro.util.Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.bond_yield_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.bond_yield_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.bond_yield_calculator))
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
