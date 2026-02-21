package com.i2medier.financialpro.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.RefinanceAdapter
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RefinanceStatisticsActivity : AppCompatActivity() {
    private lateinit var commonModel: CommonModel
    private lateinit var monthModels: ArrayList<MonthModel>
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbLoan1: RadioButton
    private lateinit var rbLoan2: RadioButton
    private lateinit var refinanceAdapter: RefinanceAdapter
    private lateinit var rvList: RecyclerView
    private lateinit var sameModel: ArrayList<MonthModel>
    private lateinit var toolBar: Toolbar
    private lateinit var yearModels: ArrayList<MonthModel>
    private var isLoan1 = true
    private var isLoanComparision = false
    private var POS = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refinance_statistics)
        init()
        setUpToolbar()

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner), this)
        adAdmob.FullscreenAd(this)
    }

    private fun init() {
        commonModel = CommonModel()
        monthModels = ArrayList()
        yearModels = ArrayList()
        sameModel = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        rvList = findViewById(R.id.rvList)
        radioGroup = findViewById(R.id.radioGroup)
        rbLoan1 = findViewById(R.id.rbLoan1)
        rbLoan2 = findViewById(R.id.rbLoan2)
        progressBar = findViewById(R.id.progressBar)

        if (intent.hasExtra("Refinance")) {
            rbLoan1.text = getString(R.string.current_loan)
            rbLoan2.text = getString(R.string.refinanced_loan)
            isLoanComparision = false
        } else {
            rbLoan1.text = getString(R.string.loan_1)
            rbLoan2.text = getString(R.string.loan_2)
            isLoanComparision = true
        }

        rbLoan1.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            if (!checked || POS == 0) return@setOnCheckedChangeListener
            POS = 0
            isLoan1 = true
            setData(monthModels)
        }

        rbLoan2.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            if (!checked || POS == 1) return@setOnCheckedChangeListener
            POS = 1
            isLoan1 = false
            setData(yearModels)
        }

        rvList.layoutManager = LinearLayoutManager(this)
        refinanceAdapter = RefinanceAdapter(this, monthModels)
        rvList.adapter = refinanceAdapter
        setValue()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.statistics)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setValue() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.Default) {
                if (intent.hasExtra("Refinance")) {
                    commonModel = intent.serializableExtraCompat<CommonModel>("Refinance") ?: CommonModel()
                    monthModels = Utils.getYearlyRefinanceAmount(
                        commonModel.principalAmount,
                        commonModel.terms,
                        commonModel.interestRate,
                        commonModel.monthlyPayment,
                        commonModel.year
                    )
                } else if (intent.hasExtra("LoanComparison")) {
                    commonModel = intent.serializableExtraCompat<CommonModel>("LoanComparison") ?: CommonModel()
                    monthModels = Utils.getYearlyLoanCompare(
                        commonModel.principalAmount,
                        commonModel.terms,
                        commonModel.interestRate,
                        commonModel.monthlyPayment
                    )
                    yearModels = Utils.getYearlyLoanCompare(
                        commonModel.principalAmount2,
                        commonModel.terms2,
                        commonModel.interestRate2,
                        commonModel.monthlyPayment2
                    )
                }
                monthModels
            }
            progressBar.visibility = View.GONE
            setData(result)
        }
    }

    private fun setData(arrayList: ArrayList<MonthModel>) {
        refinanceAdapter.setList(arrayList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            setupDialog(refinanceAdapter.monthModels, false, isLoan1, GraphUtils.COMON_GRAPH)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDialog(arrayList: ArrayList<MonthModel>, z: Boolean, z2: Boolean, i: Int) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme2)
        val inflate = LayoutInflater.from(this).inflate(R.layout.dialog_chart, null as ViewGroup?)
        bottomSheetDialog.setContentView(inflate)
        if (bottomSheetDialog.isShowing) {
            bottomSheetDialog.dismiss()
        }
        val barChart = inflate.findViewById<BarChart>(R.id.barchart)
        val textView = inflate.findViewById<TextView>(R.id.txtLabel)
        val imageView = inflate.findViewById<ImageView>(R.id.ivClose)
        val linearLayout = inflate.findViewById<LinearLayout>(R.id.llTaxINSPMI)

        linearLayout.visibility = if (i == GraphUtils.MORTGAGE_GRAPH) View.VISIBLE else View.GONE

        GraphUtils(barChart, arrayList, this, z, i).setupBarChartData()

        if (isLoanComparision) {
            textView.text = if (z2) getString(R.string.loan_1) else getString(R.string.loan_2)
        } else {
            textView.text = if (z2) getString(R.string.current_loan) else getString(R.string.refinanced_loan)
        }

        imageView.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }
}
