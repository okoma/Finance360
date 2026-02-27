package com.i2medier.financialpro.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.Interface.RemoveEditText
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.RecyclerAdapter
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.github.mikephil.charting.utils.Utils

class NPVCalculatorActivity : AppCompatActivity(), RemoveEditText {
    private lateinit var adapter: RecyclerAdapter
    private lateinit var btnAddYear: Button
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var doubleArrayList: ArrayList<String>
    private lateinit var etDiscountRate: EditText
    private lateinit var etInitialInvestment: EditText
    private lateinit var llResult: CardView
    private var mMyDialog: AlertDialog? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar
    private lateinit var txtNpv: TextView
    private lateinit var currencySymbol: String
    private lateinit var npvExplanationCard: CardView
    private lateinit var npvExplanation: TextView
    private var lastNpvValue: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_npv_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))

        init()
        setUpToolbar()
        setRecycler()
    }

    private fun setRecycler() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = RecyclerAdapter(this, doubleArrayList, this)
        recyclerView.adapter = adapter
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.net_present_value_calculator)
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
        doubleArrayList = ArrayList()
        doubleArrayList.add("")
        toolBar = findViewById(R.id.toolBar)
        recyclerView = findViewById(R.id.recyclerView)
        txtNpv = findViewById(R.id.txtNpv)
        etInitialInvestment = findViewById(R.id.etInitialInvestment)
        etDiscountRate = findViewById(R.id.etDiscountRate)
        btnAddYear = findViewById(R.id.btnAddYear)
        btnCalculate = findViewById(R.id.btnCalculate)
        llResult = findViewById(R.id.llResult)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)

        findViewById<TextView>(R.id.txtCurrencyNpv).text = currencySymbol

        val npvText = findViewById<TextView>(R.id.npvText)
        val formattedText = String.format(getString(R.string.npv_description_html), currencySymbol)
        npvText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        npvExplanationCard = findViewById(R.id.npvExplanationCard)
        npvExplanation = findViewById(R.id.npvExplanation)

        btnAddYear.setOnClickListener {
            doubleArrayList.add("")
            adapter.notifyDataSetChanged()
        }
        btnCalculate.setOnClickListener { calculate() }
        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val type = if (lastNpvValue >= 0.0) TransactionType.SAVING else TransactionType.EXPENSE
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = kotlin.math.abs(lastNpvValue),
            type = type,
            title = getString(R.string.net_present_value_calculator),
            note = getString(R.string.net_present_value_calculator)
        )
    }

    override fun removePosition(i: Int) {
        doubleArrayList.removeAt(i)
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    private fun calculate() {
        var d4 = Utils.DOUBLE_EPSILON
        try {
            val d = try {
                etInitialInvestment.text.toString().toDouble()
            } catch (e: Exception) {
                e.printStackTrace(); 0.0
            }

            val d2 = try {
                etDiscountRate.text.toString().toDouble()
            } catch (e: Exception) {
                e.printStackTrace(); 0.0
            }

            val discount = d2 / 100.0
            var pvSum = 0.0
            var i = 0
            var d5 = discount

            while (true) {
                if (i >= doubleArrayList.size) {
                    i = -1
                    break
                }
                val cashflow = try {
                    doubleArrayList[i].toDouble()
                } catch (e: Exception) {
                    e.printStackTrace(); d4
                }
                if (cashflow <= d4) break
                i++
                pvSum += cashflow / Math.pow(d5 + 1.0, i.toDouble())
                d4 = Utils.DOUBLE_EPSILON
            }

            if (i == -1) {
                val npvValue = pvSum - d
                lastNpvValue = npvValue
                txtNpv.text = CurrencyManager.format(this, npvValue)

                val interpretation = if (npvValue > 0) "is profitable and worth pursuing" else if (npvValue < 0) "will lose money and should be avoided" else "breaks even"
                val interpretationText = if (npvValue > 0) "<b>This is a good investment!</b>" else if (npvValue < 0) "<b>Consider alternative investments.</b>" else "<b>This investment breaks even.</b>"
                val explanation = getString(
                    R.string.npv_explain_html,
                    CurrencyManager.format(this, d),
                    com.i2medier.financialpro.util.Utils.decimalFormat.format(d2),
                    CurrencyManager.format(this, npvValue),
                    interpretationText,
                    interpretation
                )
                npvExplanationCard.visibility = View.VISIBLE
                npvExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Toast.makeText(this, "Year  ${i + 1}  value cannot be zero!", Toast.LENGTH_SHORT).show()
            }
            AppConstant.hideKeyboard(this)
            AppConstant.visibleResult(llResult)
            findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!com.i2medier.financialpro.util.Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.net_present_value_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.net_present_value_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.net_present_value_calculator))
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
