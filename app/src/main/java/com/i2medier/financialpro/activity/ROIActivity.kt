package com.i2medier.financialpro.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.ScrollView
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

class ROIActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var etInvest: EditText
    private lateinit var etReturn: EditText
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var txtRoi: TextView
    private lateinit var currencySymbol: String
    private lateinit var roiExplanationCard: CardView
    private lateinit var roiExplanation: TextView
    private var lastProfit: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roi)
        init()
        setUpToolbar()

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.return_on_investment_calculator)
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
        etInvest = findViewById(R.id.etInvest)
        etReturn = findViewById(R.id.etReturn)
        txtRoi = findViewById(R.id.txtRoi)
        toolBar = findViewById(R.id.toolBar)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencySymbolInvest)?.text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolReturn)?.text = currencySymbol

        val roiText = findViewById<TextView>(R.id.roiText)
        val formattedText = String.format(getString(R.string.roi_description_html), currencySymbol)
        roiText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        roiExplanationCard = findViewById(R.id.roiExplanationCard)
        roiExplanation = findViewById(R.id.roiExplanation)

        btnCalculate.setOnClickListener { calculate() }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val type = if (lastProfit >= 0) TransactionType.SAVING else TransactionType.EXPENSE
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = kotlin.math.abs(lastProfit),
            type = type,
            title = getString(R.string.return_on_investment_calculator),
            note = getString(R.string.roi_calculator)
        )
    }

    private fun calculate() {
        try {
            if (validationDetails()) {
                val invest = etInvest.text.toString().toDouble()
                val returns = etReturn.text.toString().toDouble()
                val profit = returns - invest
                lastProfit = profit
                val roiValue = if (returns > invest) (profit / invest) * 100.0 else (profit * 100.0) / invest
                txtRoi.text = Utils.decimalFormat.format(roiValue) + "%"

                val profitLoss = if (profit >= 0) "profit" else "loss"
                val interpretation = when {
                    roiValue > 0 -> "This is a profitable investment!"
                    roiValue < 0 -> "This investment resulted in a loss."
                    else -> "This investment broke even."
                }
                val explanation = getString(
                    R.string.roi_explain_html,
                    CurrencyManager.format(this, invest),
                    CurrencyManager.format(this, returns),
                    Utils.decimalFormat.format(roiValue) + "%",
                    CurrencyManager.format(this, kotlin.math.abs(profit)) + " " + profitLoss,
                    interpretation
                )
                roiExplanationCard.visibility = android.view.View.VISIBLE
                roiExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)

                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = android.view.View.VISIBLE
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun validationDetails(): Boolean {
        return if (etInvest.text.toString().isEmpty() || etInvest.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            etInvest.error = "Please fill out this field"
            false
        } else if (etReturn.text.toString().isEmpty() || etReturn.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            etReturn.error = "Please fill out this field"
            false
        } else {
            true
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.return_on_investment_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.return_on_investment_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.return_on_investment_calculator))
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
