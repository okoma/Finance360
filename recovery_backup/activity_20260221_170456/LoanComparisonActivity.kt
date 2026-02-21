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
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.planner.data.local.TransactionType
import com.i2medier.financialpro.planner.integration.PlannerCalculatorBridge
import com.i2medier.financialpro.util.AppConstant
import com.i2medier.financialpro.util.CurrencyManager
import com.i2medier.financialpro.util.ShareUtil
import com.i2medier.financialpro.util.Utils

class LoanComparisonActivity : AppCompatActivity() {
    private lateinit var btnCalculate: Button
    private lateinit var btnReset: Button
    private lateinit var btnShare: Button
    private lateinit var btnStatistics: Button
    private lateinit var commonModel: CommonModel
    private lateinit var etInterestRate1: EditText
    private lateinit var etInterestRate2: EditText
    private lateinit var etLoanAmount1: EditText
    private lateinit var etLoanAmount2: EditText
    private lateinit var etProcessingFee1: EditText
    private lateinit var etProcessingFee2: EditText
    private lateinit var etTerm1: EditText
    private lateinit var etTerm2: EditText
    private var interestRate1 = 0.0
    private var interestRate2 = 0.0
    private lateinit var llResult: CardView
    private var loanAmount1 = 0.0
    private var loanAmount2 = 0.0
    private var mMonth = 0
    private var mYear = 0
    private var mMyDialog: AlertDialog? = null
    private var monthlyPayment1 = 0.0
    private var monthlyPayment2 = 0.0
    private var processingFee1 = 0.0
    private var processingFee2 = 0.0
    private lateinit var rootLayout: ScrollView
    private lateinit var spTerm1: Spinner
    private lateinit var spTerm2: Spinner
    private var term1 = 1
    private var term2 = 1
    private var terms1 = 0.0
    private var terms2 = 0.0
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest1: TextView
    private lateinit var txtInterest2: TextView
    private lateinit var txtMonthlyPayment1: TextView
    private lateinit var txtMonthlyPayment2: TextView
    private lateinit var txtProcessingFee1: TextView
    private lateinit var txtProcessingFee2: TextView
    private lateinit var txtTotalPayments1: TextView
    private lateinit var txtTotalPayments2: TextView
    private lateinit var currencySymbol: String
    private lateinit var loanComparisonExplanationCard: CardView
    private lateinit var loanComparisonExplanation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_comparison)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)

        init()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.loan_comparison_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun init() {
        currencySymbol = CurrencyManager.getCurrencySymbol(this)
        commonModel = CommonModel()
        toolBar = findViewById(R.id.toolBar)
        etLoanAmount1 = findViewById(R.id.etLoanAmount1)
        etLoanAmount2 = findViewById(R.id.etLoanAmount2)
        etInterestRate1 = findViewById(R.id.etInterestRate1)
        etInterestRate2 = findViewById(R.id.etInterestRate2)
        etTerm1 = findViewById(R.id.etTerm1)
        etTerm2 = findViewById(R.id.etTerm2)
        etProcessingFee1 = findViewById(R.id.etProcessingFee1)
        etProcessingFee2 = findViewById(R.id.etProcessingFee2)
        txtMonthlyPayment1 = findViewById(R.id.txtMonthlyPayment1)
        txtMonthlyPayment2 = findViewById(R.id.txtMonthlyPayment2)
        txtTotalPayments1 = findViewById(R.id.txtTotalPayments1)
        txtTotalPayments2 = findViewById(R.id.txtTotalPayments2)
        txtInterest1 = findViewById(R.id.txtInterest1)
        txtInterest2 = findViewById(R.id.txtInterest2)
        txtProcessingFee1 = findViewById(R.id.txtProcessingFee1)
        txtProcessingFee2 = findViewById(R.id.txtProcessingFee2)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnReset = findViewById(R.id.btnReset)
        btnShare = findViewById(R.id.btnShare)
        rootLayout = findViewById(R.id.rootLayout)
        spTerm1 = findViewById(R.id.spTerm1)
        spTerm2 = findViewById(R.id.spTerm2)
        llResult = findViewById(R.id.llResult)

        findViewById<TextView>(R.id.txtCurrencyLoanComparison1).text = currencySymbol
        findViewById<TextView>(R.id.txtCurrencyLoanComparison2).text = currencySymbol

        val loanComparisonText = findViewById<TextView>(R.id.loanComparisonText)
        val formattedText = String.format(getString(R.string.loan_comparison_description_html), currencySymbol)
        loanComparisonText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)

        loanComparisonExplanationCard = findViewById(R.id.loanComparisonExplanationCard)
        loanComparisonExplanation = findViewById(R.id.loanComparisonExplanation)

        val arrayAdapter = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm1.adapter = arrayAdapter
        spTerm1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                term1 = if (obj == "Yr") 12 else 1
            }
        }

        val arrayAdapter2 = ArrayAdapter(this, R.layout.item_spinner, resources.getStringArray(R.array.terms_array))
        arrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTerm2.adapter = arrayAdapter2
        spTerm2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val obj = parent?.getItemAtPosition(position).toString()
                term2 = if (obj == "Yr") 12 else 1
            }
        }

        click()
    }

    private fun click() {
        btnReset.setOnClickListener {
            if (etLoanAmount1.text.toString().trim().isNotEmpty()) etLoanAmount1.setText("")
            if (etLoanAmount2.text.toString().trim().isNotEmpty()) etLoanAmount2.setText("")
            if (etInterestRate1.text.toString().trim().isNotEmpty()) etInterestRate1.setText("")
            if (etInterestRate2.text.toString().trim().isNotEmpty()) etInterestRate2.setText("")
            if (etTerm1.text.toString().trim().isNotEmpty()) etTerm1.setText("")
            if (etTerm2.text.toString().trim().isNotEmpty()) etTerm2.setText("")
            if (etProcessingFee1.text.toString().trim().isNotEmpty()) etProcessingFee1.setText("")
            if (etProcessingFee2.text.toString().trim().isNotEmpty()) etProcessingFee2.setText("")
            txtMonthlyPayment1.text = ""
            txtMonthlyPayment2.text = ""
            txtTotalPayments1.text = ""
            txtTotalPayments2.text = ""
            txtInterest1.text = ""
            txtInterest2.text = ""
            txtProcessingFee1.text = ""
            txtProcessingFee2.text = ""
        }

        btnCalculate.setOnClickListener {
            if (setValue()) {
                AppConstant.hideKeyboard(this)
                AppConstant.visibleResult(llResult)
                findViewById<Button>(R.id.btnAddToPlanner).visibility = View.VISIBLE
            }
            txtMonthlyPayment1.text = CurrencyManager.format(this, monthlyPayment1)
            txtMonthlyPayment2.text = CurrencyManager.format(this, monthlyPayment2)
            txtTotalPayments1.text = CurrencyManager.format(this, monthlyPayment1 * terms1 * term1)
            txtTotalPayments2.text = CurrencyManager.format(this, monthlyPayment2 * terms2 * term2)
            txtInterest1.text = CurrencyManager.format(this, Utils.getTotalInterest(monthlyPayment1, terms1 * term1, loanAmount1))
            txtInterest2.text = CurrencyManager.format(this, Utils.getTotalInterest(monthlyPayment2, terms2 * term2, loanAmount2))
            txtProcessingFee1.text = CurrencyManager.format(this, processingFee1)
            txtProcessingFee2.text = CurrencyManager.format(this, processingFee2)

            val totalPayments1 = monthlyPayment1 * terms1 * term1
            val totalPayments2 = monthlyPayment2 * terms2 * term2
            val totalInterest1 = Utils.getTotalInterest(monthlyPayment1, terms1 * term1, loanAmount1)
            val totalInterest2 = Utils.getTotalInterest(monthlyPayment2, terms2 * term2, loanAmount2)

            val betterLoan = if (totalPayments1 < totalPayments2) {
                "Loan 1 offers better overall value with lower total payments."
            } else if (totalPayments2 < totalPayments1) {
                "Loan 2 offers better overall value with lower total payments."
            } else {
                "Both loans have similar total costs."
            }

            val explanation = getString(
                R.string.loan_comparison_explain_html,
                CurrencyManager.format(this, monthlyPayment1),
                CurrencyManager.format(this, totalPayments1),
                CurrencyManager.format(this, totalInterest1),
                CurrencyManager.format(this, processingFee1),
                CurrencyManager.format(this, monthlyPayment2),
                CurrencyManager.format(this, totalPayments2),
                CurrencyManager.format(this, totalInterest2),
                CurrencyManager.format(this, processingFee2),
                betterLoan
            )
            loanComparisonExplanationCard.visibility = View.VISIBLE
            loanComparisonExplanation.text = Html.fromHtml(explanation, Html.FROM_HTML_MODE_LEGACY)
        }

        btnStatistics.setOnClickListener {
            if (setValue()) {
                commonModel.principalAmount = loanAmount1
                commonModel.interestRate = interestRate1
                commonModel.terms = if (term1 == 1) terms1 / 12.0 else terms1
                commonModel.monthlyPayment = monthlyPayment1
                commonModel.principalAmount2 = loanAmount2
                commonModel.interestRate2 = interestRate2
                commonModel.terms2 = if (term2 == 1) terms2 / 12.0 else terms2
                commonModel.monthlyPayment2 = monthlyPayment2
                commonModel.month = mMonth
                commonModel.year = mYear
                commonModel.date = System.currentTimeMillis()
                val intent = Intent(this, RefinanceStatisticsActivity::class.java)
                intent.putExtra("LoanComparison", commonModel)
                startActivity(intent)
            }
        }

        btnShare.setOnClickListener { checkPermission() }
        findViewById<Button>(R.id.btnAddToPlanner).setOnClickListener { addResultToPlanner() }
    }

    private fun addResultToPlanner() {
        val recommendedMonthlyPayment = minOf(monthlyPayment1, monthlyPayment2)
        PlannerCalculatorBridge.openPlannerWithResult(
            context = this,
            amount = recommendedMonthlyPayment,
            type = TransactionType.EXPENSE,
            title = getString(R.string.loan_comparison_calculator),
            note = getString(R.string.monthly_payment)
        )
    }

    private fun setValue(): Boolean {
        return try {
            if (etLoanAmount1.text.toString().trim().isNotEmpty() && etLoanAmount1.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                loanAmount1 = etLoanAmount1.text.toString().toDouble()
                if (etLoanAmount2.text.toString().trim().isNotEmpty() && etLoanAmount2.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    loanAmount2 = etLoanAmount2.text.toString().toDouble()
                    if (etInterestRate1.text.toString().trim().isNotEmpty() && etInterestRate1.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                        interestRate1 = etInterestRate1.text.toString().toDouble()
                        if (etInterestRate2.text.toString().trim().isNotEmpty() && etInterestRate2.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                            interestRate2 = etInterestRate2.text.toString().toDouble()
                            if (etTerm1.text.toString().trim().isNotEmpty() && etTerm1.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                                terms1 = etTerm1.text.toString().toDouble()
                                if (etTerm2.text.toString().trim().isNotEmpty() && etTerm2.text.toString().toDouble() != com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                                    terms2 = etTerm2.text.toString().toDouble()
                                    if (etProcessingFee1.text.toString().trim().isNotEmpty()) {
                                        processingFee1 = (loanAmount1 * etProcessingFee1.text.toString().toDouble()) / 100.0
                                    }
                                    if (etProcessingFee2.text.toString().trim().isNotEmpty()) {
                                        processingFee2 = (loanAmount2 * etProcessingFee2.text.toString().toDouble()) / 100.0
                                    }
                                    monthlyPayment1 = Utils.getMonthlyPayment(loanAmount1, interestRate1, terms1 * term1)
                                    monthlyPayment2 = Utils.getMonthlyPayment(loanAmount2, interestRate2, terms2 * term2)
                                    true
                                } else {
                                    etTerm2.error = "Please fill out this field"
                                    false
                                }
                            } else {
                                etTerm1.error = "Please fill out this field"
                                false
                            }
                        } else {
                            etInterestRate2.error = "Please fill out this field"
                            false
                        }
                    } else {
                        etInterestRate1.error = "Please fill out this field"
                        false
                    }
                } else {
                    etLoanAmount2.error = "Please fill out this field"
                    false
                }
            } else {
                etLoanAmount1.error = "Please fill out this field"
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid decimal value", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            false
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf("android.permission.WRITE_EXTERNAL_STORAGE")
            if (!Utils.hasPermissions(this, *permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 112)
                return
            }
            ShareUtil.print(this, rootLayout, getString(R.string.loan_comparison_calculator))
            return
        }
        ShareUtil.print(this, rootLayout, getString(R.string.loan_comparison_calculator))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 112 && grantResults.isNotEmpty()) {
            if (grantResults[0] == 0) {
                ShareUtil.print(this, rootLayout, getString(R.string.loan_comparison_calculator))
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
