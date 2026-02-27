package com.i2medier.financialpro.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.MortgageAdapter
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.DialogUtils
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class MortgageStatisticsActivity : AppCompatActivity() {
    private lateinit var commonModel: CommonModel
    private var dialogUtils: DialogUtils? = null
    private var endDateYear: Date? = null
    private lateinit var monthModels: ArrayList<MonthModel>
    private lateinit var mortgageAdapter: MortgageAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var rbMonth: RadioButton
    private lateinit var rbYear: RadioButton
    private lateinit var rvList: RecyclerView
    private lateinit var sameModel: ArrayList<MonthModel>
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest: TextView
    private lateinit var txtMonthLabel: TextView
    private lateinit var txtPaid: TextView
    private lateinit var txtPrincipal: TextView
    private lateinit var txtTaxInsPMI: TextView
    private lateinit var yearModels: ArrayList<MonthModel>
    private var POS = 0
    private var isMonthly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mortgage_statistics)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById<RelativeLayout>(R.id.banner))
        adAdmob.FullscreenAd(this)
        registerBackPressHandler()

        commonModel = CommonModel()
        monthModels = ArrayList()
        yearModels = ArrayList()
        sameModel = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        setUpToolbar()
        rvList = findViewById(R.id.rvList)
        txtPrincipal = findViewById(R.id.txtPrincipal)
        txtInterest = findViewById(R.id.txtInterest)
        txtTaxInsPMI = findViewById(R.id.txtTaxInsPMI)
        txtPaid = findViewById(R.id.txtPaid)
        txtMonthLabel = findViewById(R.id.txtMonthLabel)
        rbMonth = findViewById(R.id.rbMonth)
        rbYear = findViewById(R.id.rbYear)
        progressBar = findViewById(R.id.progressBar)

        rbYear.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            if (!checked || POS == 1) return@setOnCheckedChangeListener
            POS = 1
            txtMonthLabel.text = getString(R.string.year)
            isMonthly = false
            setData(yearModels)
        }

        rbMonth.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            if (!checked || POS == 0) return@setOnCheckedChangeListener
            POS = 0
            txtMonthLabel.text = getString(R.string.month)
            isMonthly = true
            setData(monthModels)
        }

        txtPrincipal.text = Utils.decimalFormat.format(Utils.Principal)
        txtInterest.text = Utils.decimalFormat.format(Utils.Interest)
        txtTaxInsPMI.text = Utils.decimalFormat.format(Utils.mTaxInsPMI)
        txtPaid.text = Utils.decimalFormat.format(Utils.Paid)

        rvList.layoutManager = LinearLayoutManager(this)
        mortgageAdapter = MortgageAdapter(this, monthModels, isMonthly)
        rvList.adapter = mortgageAdapter
        setValue()
    }

    private fun setValue() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.Default) {
                if (intent.hasExtra("Mortgage")) {
                    commonModel = intent.serializableExtraCompat<CommonModel>("Mortgage") ?: CommonModel()
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = commonModel.date
                    calendar.add(Calendar.MONTH, 1)
                    monthModels = Utils.getMonthlyMortgage(
                        commonModel.principalAmount,
                        commonModel.terms,
                        commonModel.interestRate,
                        commonModel.monthlyPayment,
                        commonModel.TaxIns,
                        commonModel.PMI,
                        calendar.time,
                        commonModel.downPayment
                    )
                    endDateYear = Date()
                    val calendar2 = Calendar.getInstance()
                    calendar2.time = calendar.time
                    calendar2.add(Calendar.MONTH, Integer.parseInt(Utils.decimalFormat.format(commonModel.terms)))
                    endDateYear = calendar2.time
                    yearModels = Utils.getYearlyMortgage(monthModels, calendar.time, endDateYear!!)
                }
                monthModels
            }

            progressBar.visibility = View.GONE
            txtPrincipal.text = Utils.decimalFormat.format(Utils.Principal)
            txtInterest.text = Utils.decimalFormat.format(Utils.Interest)
            txtPaid.text = Utils.decimalFormat.format(Utils.Paid)
            setData(result)
        }
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.statistics)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun resetStats() {
        Utils.Principal = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        Utils.Interest = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        Utils.mTaxInsPMI = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        Utils.Paid = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
    }

    private fun registerBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resetStats()
                finish()
            }
        })
    }

    fun setData(arrayList: ArrayList<MonthModel>) {
        mortgageAdapter.setList(arrayList, isMonthly)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            dialogUtils = DialogUtils(this, mortgageAdapter.monthModels, isMonthly, GraphUtils.MORTGAGE_GRAPH)
            dialogUtils?.setupDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
