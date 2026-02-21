package com.i2medier.financialpro.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
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
import com.i2medier.financialpro.model.GraphModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils
import java.util.ArrayList

class PresentValueCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var futureValue: EditText
    private lateinit var graphModelArrayList: ArrayList<GraphModel>
    private lateinit var graphlayot: LinearLayout
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private lateinit var periods: EditText
    private lateinit var pieChart: PieChart
    private lateinit var rate: EditText
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var txtPresentValue: TextView
    private lateinit var txtTotalInterest: TextView
    private lateinit var currencySymbol: String
    private lateinit var presentValueExplanationCard: CardView
    private lateinit var presentValueExplanation: TextView
    private var lastPresentValue: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_present_value_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.present_value_calculator)
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
        graphModelArrayList = ArrayList()
        futureValue = findViewById(R.id.futureValue)
        periods = findViewById(R.id.periods)
        llResult = findViewById(R.id.llResult)
        rate = findViewById(R.id.rate)
        pieChart = findViewById(R.id.piechart)
        txtPresentValue = findViewById(R.id.txtPresentValue)
        txtTotalInterest = findViewById(R.id.txtTotalInterest)
        graphlayot = findViewById(R.id.graphlayot)
        toolBar = findViewById(R.id.toolBar)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)

        findViewById<TextView>(R.id.txtCurrencySymbolPresent1).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencySymbolPresent2).text = currencySymbol

        val presentValueText = findViewById<TextView>(R.id.presentValueText)
        val formattedText = String.format(getString(R.string.present_value_description_html), currencySymbol)
        presentValueText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        presentValueExplanationCard = findViewById(R.id.presentValueExplanationCard)
        presentValueExplanation = findViewById(R.id.presentValueExplanation)

        btnCalculate.setOnClickListener { calculate() }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = lastPresentValue,
            type = TransactionType.SAVING,
            title = getString(R.string.present_value_calculator),
            note = getString(R.string.present_value_calculator)
        )
    }

    @SuppressLint("SetTextI18n")
    private fun calculate() {
        try {
            if (validationDetails()) {
                val future = futureValue.text.toString().toDouble()
                val rateVal = rate.text.toString().toDouble()
                val periodsVal = periods.text.toString().toDouble()
                val present = future * (1.0 / Math.pow((rateVal / 100.0) + 1.0, periodsVal))
                lastPresentValue = present
                val totalInterest = future - present

                txtPresentValue.text = CurrencyManager.format(this, present)
                txtTotalInterest.text = CurrencyManager.format(this, totalInterest)

                val explanation = getString(
                    R.string.present_value_explain_html,
                    CurrencyManager.format(this, future),
                    Utils.decimalFormat.format(rateVal),
                    periodsVal.toInt().toString(),
                    CurrencyManager.format(this, present),
                    CurrencyManager.format(this, totalInterest)
                )
                presentValueExplanationCard.visibility = android.view.View.VISIBLE
                presentValueExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)

                setPieChart(present, totalInterest)
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                AppConstant.visibleGraph(graphlayot)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = android.view.View.VISIBLE
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun validationDetails(): Boolean {
        return if (futureValue.text.toString().isEmpty() || futureValue.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            futureValue.error = "Please fill out this field"
            false
        } else if (periods.text.toString().isEmpty() || periods.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            periods.error = "Please fill out this field"
            false
        } else if (rate.text.toString().isEmpty() || rate.text.toString().toDouble() == com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
            rate.error = "Please fill out this field"
            false
        } else {
            true
        }
    }

    private fun setPieChart(present: Double, totalInterest: Double) {
        graphModelArrayList.clear()
        graphModelArrayList.add(
            GraphModel(
                getString(R.string.present) + "\n(" + Utils.decimalFormat.format(present) + ")",
                present,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)
            )
        )
        graphModelArrayList.add(
            GraphModel(
                getString(R.string.total_interest) + "\n(" + Utils.decimalFormat.format(totalInterest) + ")",
                totalInterest,
                androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)
            )
        )

        val centerText = Utils.decimalFormat.format(present + totalInterest)
        GraphUtils(pieChart, graphModelArrayList, applicationContext).setupPieData(centerText)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.present_value_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.present_value_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.present_value_calculator))
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
