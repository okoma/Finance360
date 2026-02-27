package com.i2medier.financialpro.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList

class NetWorthCalculatorActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnShare: Button
    private lateinit var btnAddToPlanner: Button

    private lateinit var drop: ImageView
    private lateinit var drop1: ImageView
    private lateinit var drop2: ImageView
    private lateinit var drop3: ImageView
    private lateinit var drop4: ImageView
    private lateinit var drop5: ImageView

    private lateinit var liquidAssets: CardView
    private lateinit var investmentAssets: CardView
    private lateinit var retirementAssets: CardView
    private lateinit var personalAssets: CardView
    private lateinit var shortTerm: CardView
    private lateinit var longTerm: CardView

    private lateinit var linearLiquid: LinearLayout
    private lateinit var linearInvestment: LinearLayout
    private lateinit var linearRetirement: LinearLayout
    private lateinit var linearPersonal: LinearLayout
    private lateinit var linearShortTerm: LinearLayout
    private lateinit var linearLongTerm: LinearLayout

    private lateinit var graphLayout: CardView
    private lateinit var llResult: CardView
    private lateinit var rootLayout: ScrollView
    private lateinit var toolBar: Toolbar

    private lateinit var lTotal: TextView
    private lateinit var totalInvest: TextView
    private lateinit var totalRetire: TextView
    private lateinit var totalPersonalAsset: TextView
    private lateinit var totalShortTermLiabilities: TextView
    private lateinit var totalLongTermLiabilities: TextView
    private lateinit var txtTotalAsset: TextView
    private lateinit var txtTotalLiabilities: TextView
    private lateinit var txtNetWorth: TextView

    private lateinit var piechartAssets: PieChart
    private lateinit var piechartLiabilities: PieChart

    private lateinit var networthExplanationCard: CardView
    private lateinit var networthExplanation: TextView

    private lateinit var currencySymbol: String
    private lateinit var currencyFormat: DecimalFormat
    private var mMyDialog: AlertDialog? = null
    private var latestNetWorth: Double = 0.0

    private lateinit var liquidInputs: List<EditText>
    private lateinit var investmentInputs: List<EditText>
    private lateinit var retirementInputs: List<EditText>
    private lateinit var personalInputs: List<EditText>
    private lateinit var shortTermInputs: List<EditText>
    private lateinit var longTermInputs: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth_calculator)

        val adAdmob = AdAdmob(this)
        adAdmob.FullscreenAd(this)

        init()
        setUpToolbar()
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        currencyFormat = (java.text.NumberFormat.getCurrencyInstance() as DecimalFormat).apply {
            val symbols: DecimalFormatSymbols = decimalFormatSymbols
            symbols.currencySymbol = currencySymbol
            decimalFormatSymbols = symbols
            maximumFractionDigits = 2
        }

        toolBar = findViewById(R.id.toolBar)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnShare = findViewById(R.id.btnShare)
        btnAddToPlanner = findViewById(R.id.btnAddToPlanner)
        rootLayout = findViewById(R.id.rootLayout)

        liquidAssets = findViewById(R.id.liquidAssests)
        investmentAssets = findViewById(R.id.investmentAssests)
        retirementAssets = findViewById(R.id.retirementAssests)
        personalAssets = findViewById(R.id.personalAssests)
        shortTerm = findViewById(R.id.shortTerm)
        longTerm = findViewById(R.id.longTerm)

        llResult = findViewById(R.id.llResult)
        graphLayout = findViewById(R.id.graphlayot)

        linearLiquid = findViewById(R.id.linearLiquid)
        linearInvestment = findViewById(R.id.linearInvestment)
        linearRetirement = findViewById(R.id.linearRetirement)
        linearPersonal = findViewById(R.id.linearPersonal)
        linearShortTerm = findViewById(R.id.linearShortTerm)
        linearLongTerm = findViewById(R.id.linearLongTerm)

        lTotal = findViewById(R.id.lTotal)
        totalInvest = findViewById(R.id.totalInvest)
        totalRetire = findViewById(R.id.totalRetire)
        totalPersonalAsset = findViewById(R.id.totalPersonalAssest)
        totalShortTermLiabilities = findViewById(R.id.totalShortTermLiabilities)
        totalLongTermLiabilities = findViewById(R.id.totalLongTermLiabilities)
        txtTotalAsset = findViewById(R.id.txtTotalAssest)
        txtTotalLiabilities = findViewById(R.id.txtTotalLiabilities)
        txtNetWorth = findViewById(R.id.txtNetWorth)

        drop = findViewById(R.id.drops)
        drop1 = findViewById(R.id.drop1)
        drop2 = findViewById(R.id.drop2)
        drop3 = findViewById(R.id.drop3)
        drop4 = findViewById(R.id.drop4)
        drop5 = findViewById(R.id.drop5)

        piechartAssets = findViewById(R.id.piechartAssets)
        piechartLiabilities = findViewById(R.id.piechartLiabilities)

        networthExplanationCard = findViewById(R.id.networthExplanationCard)
        networthExplanation = findViewById(R.id.networthExplanation)

        val currencyIds = intArrayOf(
            R.id.NetWorthCurrency1, R.id.NetWorthCurrency2, R.id.NetWorthCurrency3, R.id.NetWorthCurrency4,
            R.id.NetWorthCurrency5, R.id.NetWorthCurrency6, R.id.NetWorthCurrency7, R.id.NetWorthCurrency8,
            R.id.NetWorthCurrency9, R.id.NetWorthCurrency10, R.id.NetWorthCurrency11, R.id.NetWorthCurrency12,
            R.id.NetWorthCurrency13, R.id.NetWorthCurrency14, R.id.NetWorthCurrency15, R.id.NetWorthCurrency16,
            R.id.NetWorthCurrency17, R.id.NetWorthCurrency18, R.id.NetWorthCurrency19, R.id.NetWorthCurrency20,
            R.id.NetWorthCurrency21, R.id.NetWorthCurrency22, R.id.NetWorthCurrency23, R.id.NetWorthCurrency24,
            R.id.NetWorthCurrency25, R.id.NetWorthCurrency26, R.id.NetWorthCurrency27, R.id.NetWorthCurrency28,
            R.id.NetWorthCurrency29, R.id.NetWorthCurrency30, R.id.NetWorthCurrency31, R.id.NetWorthCurrency32,
            R.id.NetWorthCurrency33, R.id.NetWorthCurrency34, R.id.NetWorthCurrency35, R.id.NetWorthCurrency36
        )
        currencyIds.forEach { id -> findViewById<TextView>(id)?.text = currencySymbol }

        val networthText = findViewById<TextView>(R.id.networthText)
        val formattedText = String.format(getString(R.string.networth_description_html), currencySymbol)
        networthText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        liquidInputs = listOf(
            findViewById(R.id.etCash),
            findViewById(R.id.etSavingsAccount),
            findViewById(R.id.etCheckingAccount),
            findViewById(R.id.etLifeInsurance),
            findViewById(R.id.etLiquidOther)
        )
        investmentInputs = listOf(
            findViewById(R.id.etRealEstate),
            findViewById(R.id.etDeposit),
            findViewById(R.id.etStocks),
            findViewById(R.id.etBonds),
            findViewById(R.id.etMutualFunds),
            findViewById(R.id.etBusiness),
            findViewById(R.id.etInvestmentOther)
        )
        retirementInputs = listOf(
            findViewById(R.id.etIras),
            findViewById(R.id.etKs),
            findViewById(R.id.etKeogh),
            findViewById(R.id.etPension),
            findViewById(R.id.etRetirementOther)
        )
        personalInputs = listOf(
            findViewById(R.id.etPrimaryResidence),
            findViewById(R.id.etVacationProperties),
            findViewById(R.id.etCars),
            findViewById(R.id.etBoats),
            findViewById(R.id.etHomeFurnishing),
            findViewById(R.id.etArt),
            findViewById(R.id.etJewelry),
            findViewById(R.id.etCollectibles),
            findViewById(R.id.etPersonalOthers)
        )
        shortTermInputs = listOf(
            findViewById(R.id.etCreditCard),
            findViewById(R.id.etShortTermLoan),
            findViewById(R.id.etOutstandingTaxes),
            findViewById(R.id.etShortTermOther)
        )
        longTermInputs = listOf(
            findViewById(R.id.etMortgages),
            findViewById(R.id.etInvestmentLoan),
            findViewById(R.id.etPersonalLoan),
            findViewById(R.id.etStudentLoan),
            findViewById(R.id.etAutoLoan),
            findViewById(R.id.etLongTermOther)
        )

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = recalculateCategoryTotals()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        (liquidInputs + investmentInputs + retirementInputs + personalInputs + shortTermInputs + longTermInputs)
            .forEach { it.addTextChangedListener(watcher) }

        btnCalculate.setOnClickListener {
            com.i2medier.financialpro.util.AnalyticsTracker.logCalculatorCalculated(this, javaClass.simpleName)
            calculate()
            AppConstant.hideKeyboard(this)
            AppConstant.visibleResult(llResult)
            (graphLayout.getChildAt(0) as? LinearLayout)?.let { child ->
                AppConstant.visibleGraph(child)
            }
            btnAddToPlanner.visibility = View.VISIBLE
        }

        btnShare.setOnClickListener { checkPermission() }
        btnAddToPlanner.setOnClickListener { addResultToPlanner() }

        setupVisibilityToggles()
        recalculateCategoryTotals()
    }

    @SuppressLint("SetTextI18n")
    private fun calculate() {
        val totalCash = sum(liquidInputs)
        val totalInvestment = sum(investmentInputs)
        val totalRetirement = sum(retirementInputs)
        val totalPersonal = sum(personalInputs)
        val totalShortTerm = sum(shortTermInputs)
        val totalLongTerm = sum(longTermInputs)

        val totalAssets = totalCash + totalInvestment + totalRetirement + totalPersonal
        val totalLiabilities = totalShortTerm + totalLongTerm
        val netWorth = totalAssets - totalLiabilities
        latestNetWorth = netWorth

        txtTotalAsset.text = CurrencyManager.format(this, totalAssets)
        txtTotalLiabilities.text = CurrencyManager.format(this, totalLiabilities)
        txtNetWorth.text = CurrencyManager.format(this, netWorth)

        val statusText = if (netWorth >= 0) "<b>Positive Net Worth:</b>" else "<b>Negative Net Worth:</b>"
        val statusMessage = if (netWorth >= 0) {
            "Congratulations! You have a positive net worth, indicating financial health and stability."
        } else {
            "You currently have a negative net worth, meaning your liabilities exceed your assets. Focus on reducing debt and building assets."
        }
        val interpretation = if (netWorth >= 0) {
            "A positive net worth indicates you own more than you owe, which is a strong foundation for financial security."
        } else {
            "A negative net worth means you owe more than you own. Consider creating a plan to pay down debt and increase your assets."
        }

        val explanation = getString(
            R.string.networth_explain_html,
            CurrencyManager.format(this, totalAssets),
            CurrencyManager.format(this, totalLiabilities),
            CurrencyManager.format(this, netWorth),
            statusText,
            statusMessage,
            interpretation
        )
        networthExplanationCard.visibility = View.VISIBLE
        networthExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)

        setAssetsPieChart(totalCash, totalInvestment, totalRetirement, totalPersonal)
        setLiabilitiesPieChart(totalShortTerm, totalLongTerm)
    }

    private fun addResultToPlanner() {
        if (llResult.visibility != View.VISIBLE) {
            android.widget.Toast.makeText(this, "Calculate first", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (latestNetWorth == 0.0) {
            android.widget.Toast.makeText(this, "No amount to add", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        if (latestNetWorth > 0) {
            PlannerCalculatorBridge.openPlannerWithResult(
                context = this,
                amount = latestNetWorth,
                type = TransactionType.SAVING,
                note = "Net worth snapshot from Net Worth Calculator",
                title = getString(R.string.net_worth_calculator)
            )
        } else {
            PlannerCalculatorBridge.openPlannerWithResult(
                context = this,
                amount = kotlin.math.abs(latestNetWorth),
                type = TransactionType.EXPENSE,
                note = "Net worth deficit snapshot from Net Worth Calculator",
                title = getString(R.string.net_worth_calculator)
            )
        }
    }

    private fun recalculateCategoryTotals() {
        val totalCash = sum(liquidInputs)
        val totalInvestment = sum(investmentInputs)
        val totalRetirement = sum(retirementInputs)
        val totalPersonal = sum(personalInputs)
        val totalShortTerm = sum(shortTermInputs)
        val totalLongTerm = sum(longTermInputs)

        lTotal.text = CurrencyManager.format(this, totalCash)
        totalInvest.text = CurrencyManager.format(this, totalInvestment)
        totalRetire.text = CurrencyManager.format(this, totalRetirement)
        totalPersonalAsset.text = CurrencyManager.format(this, totalPersonal)
        totalShortTermLiabilities.text = CurrencyManager.format(this, totalShortTerm)
        totalLongTermLiabilities.text = CurrencyManager.format(this, totalLongTerm)
        txtTotalAsset.text = CurrencyManager.format(this, totalCash + totalInvestment + totalRetirement + totalPersonal)
    }

    private fun setupVisibilityToggles() {
        setSectionExpanded(linearLiquid, drop, expanded = true)
        setSectionExpanded(linearInvestment, drop1, expanded = true)
        setSectionExpanded(linearRetirement, drop2, expanded = true)
        setSectionExpanded(linearPersonal, drop3, expanded = true)
        setSectionExpanded(linearShortTerm, drop4, expanded = true)
        setSectionExpanded(linearLongTerm, drop5, expanded = true)

        toggleSection(liquidAssets, linearLiquid, drop)
        toggleSection(investmentAssets, linearInvestment, drop1)
        toggleSection(retirementAssets, linearRetirement, drop2)
        toggleSection(personalAssets, linearPersonal, drop3)
        toggleSection(shortTerm, linearShortTerm, drop4)
        toggleSection(longTerm, linearLongTerm, drop5)
    }

    private fun setSectionExpanded(content: LinearLayout, arrow: ImageView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        arrow.setImageResource(if (expanded) R.drawable.ic_up_black else R.drawable.ic_down_black)
    }

    private fun toggleSection(card: CardView, content: LinearLayout, arrow: ImageView) {
        card.setOnClickListener {
            if (content.visibility == View.GONE) {
                content.visibility = View.VISIBLE
                arrow.setImageResource(R.drawable.ic_up_black)
            } else {
                content.visibility = View.GONE
                arrow.setImageResource(R.drawable.ic_down_black)
            }
        }
    }

    private fun setAssetsPieChart(liquid: Double, invest: Double, retire: Double, personal: Double) {
        val list = ArrayList<GraphModel>()
        list.add(GraphModel(getString(R.string.liquid) + "\n(" + currencyFormat.format(liquid) + ")", liquid, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)))
        list.add(GraphModel(getString(R.string.invest) + "\n(" + currencyFormat.format(invest) + ")", invest, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)))
        list.add(GraphModel(getString(R.string.retire) + "\n(" + currencyFormat.format(retire) + ")", retire, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor3)))
        list.add(GraphModel(getString(R.string.personal) + "\n(" + currencyFormat.format(personal) + ")", personal, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor4)))
        GraphUtils(piechartAssets, list, applicationContext).setupPieData(currencyFormat.format(liquid + invest + retire + personal))
    }

    private fun setLiabilitiesPieChart(shortTerm: Double, longTerm: Double) {
        val list = ArrayList<GraphModel>()
        list.add(GraphModel(getString(R.string.shortterm) + "\n(" + currencyFormat.format(shortTerm) + ")", shortTerm, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor1)))
        list.add(GraphModel(getString(R.string.longterm) + "\n(" + currencyFormat.format(longTerm) + ")", longTerm, androidx.core.content.ContextCompat.getColor(this, R.color.graphcolor2)))
        GraphUtils(piechartLiabilities, list, applicationContext).setupPieData(currencyFormat.format(shortTerm + longTerm))
    }

    private fun sum(inputs: List<EditText>): Double {
        return inputs.sumOf { input ->
            parseNumericInput(input.text?.toString().orEmpty()) ?: 0.0
        }
    }

    private fun parseNumericInput(raw: String): Double? {
        if (raw.isBlank()) return null
        val normalized = raw
            .replace(currencySymbol, "")
            .replace(",", "")
            .trim()
        return normalized.toDoubleOrNull()
    }

    private fun checkPermission() {
        ShareUtil.print(this, rootLayout, getString(R.string.net_worth_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.net_worth_calculator))
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

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.net_worth_calculator)
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }
}
