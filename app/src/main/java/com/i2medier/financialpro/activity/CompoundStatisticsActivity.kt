package com.i2medier.financialpro.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.CompoundAdapter
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

class CompoundStatisticsActivity : AppCompatActivity() {
    private var commonModel: CommonModel = CommonModel()
    private var compoundAdapter: CompoundAdapter? = null
    private var dialogUtils: DialogUtils? = null
    private var endDateYear: Date? = null
    private var monthModels: ArrayList<MonthModel> = ArrayList()
    private lateinit var progressBar: ProgressBar
    private lateinit var rbMonth: RadioButton
    private lateinit var rbYear: RadioButton
    private lateinit var rvList: RecyclerView
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest: TextView
    private lateinit var txtMonthLabel: TextView
    private lateinit var txtPaid: TextView
    private lateinit var txtPrincipal: TextView
    private var yearModels: ArrayList<MonthModel> = ArrayList()
    private var pos = 0
    private var isMonthly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compound_statistics)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById(R.id.banner))
        adAdmob.FullscreenAd(this)

        init()
        setUpToolbar()
        registerBackPressHandler()
    }

    private fun setValue() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.Default) {
                if (intent.hasExtra("CompoundInterest")) {
                    commonModel = intent.serializableExtraCompat<CommonModel>("CompoundInterest") ?: CommonModel()
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = commonModel.date
                    monthModels = Utils.getMonthlyCompound(
                        commonModel.principalAmount,
                        commonModel.terms,
                        commonModel.interestRate,
                        commonModel.monthlyPayment,
                        calendar.time
                    )
                    endDateYear = Date()
                    val calendar2 = Calendar.getInstance()
                    calendar2.timeInMillis = commonModel.date
                    calendar2.add(Calendar.MONTH, Utils.decimalFormat.format(commonModel.terms).toInt())
                    endDateYear = calendar2.time
                    yearModels = Utils.getYearlyCompound(monthModels, calendar2.time, endDateYear!!)
                }
                monthModels
            }
            progressBar.visibility = View.GONE
            txtPrincipal.text = Utils.decimalFormat.format(kotlin.math.round(Utils.Principal))
            txtInterest.text = Utils.decimalFormat.format(kotlin.math.round(Utils.Interest))
            txtPaid.text = Utils.decimalFormat.format(kotlin.math.round(Utils.Paid))
            setData(result)
        }
    }

    private fun init() {
        commonModel = CommonModel()
        monthModels = ArrayList()
        yearModels = ArrayList()

        toolBar = findViewById(R.id.toolBar)
        rvList = findViewById(R.id.rvList)
        txtPrincipal = findViewById(R.id.txtPrincipal)
        txtInterest = findViewById(R.id.txtInterest)
        txtPaid = findViewById(R.id.txtPaid)
        txtMonthLabel = findViewById(R.id.txtMonthLabel)
        rbMonth = findViewById(R.id.rbMonth)
        rbYear = findViewById(R.id.rbYear)
        progressBar = findViewById(R.id.progressBar)

        rbYear.setOnCheckedChangeListener { _, checked ->
            if (checked && pos != 1) {
                pos = 1
                txtMonthLabel.text = getString(R.string.year)
                isMonthly = false
                setData(yearModels)
            }
        }
        rbMonth.setOnCheckedChangeListener { _, checked ->
            if (checked && pos != 0) {
                pos = 0
                txtMonthLabel.text = getString(R.string.month)
                isMonthly = true
                setData(monthModels)
            }
        }

        rvList.layoutManager = LinearLayoutManager(this)
        compoundAdapter = CompoundAdapter(this, monthModels, isMonthly)
        rvList.adapter = compoundAdapter
        setValue()
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.statistics)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun resetStats() {
        Utils.Principal = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        Utils.Interest = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
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

    private fun setData(list: ArrayList<MonthModel>) {
        compoundAdapter?.setList(list, isMonthly)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            dialogUtils = DialogUtils(this, compoundAdapter?.getMonthModels() ?: ArrayList(), isMonthly, GraphUtils.SIMPLE_INTEREST_GRAPH)
            dialogUtils?.setupDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
