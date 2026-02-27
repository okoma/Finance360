package com.i2medier.financialpro.activity

import android.os.Bundle
import android.util.Log
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
import com.i2medier.financialpro.adapter.MonthAdapter
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

class StatisticsActivity : AppCompatActivity() {
    private var commonModel: CommonModel = CommonModel()
    private var dialogUtils: DialogUtils? = null
    private var endDateYear: Date? = null
    private var interestOnly: CommonModel = CommonModel()
    private lateinit var monthAdapter: MonthAdapter
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
        setContentView(R.layout.activity_statistics)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById(R.id.banner))
        adAdmob.FullscreenAd(this)
        registerBackPressHandler()

        commonModel = CommonModel()
        interestOnly = CommonModel()
        monthModels = ArrayList()
        yearModels = ArrayList()

        toolBar = findViewById(R.id.toolBar)
        setUpToolbar()
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
        monthAdapter = MonthAdapter(this, monthModels, isMonthly)
        rvList.adapter = monthAdapter
        setValue()
    }

    private fun setValue() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.Default) {
                if (intent.hasExtra("AutoLoan")) {
                    commonModel = intent.serializableExtraCompat<CommonModel>("AutoLoan") ?: CommonModel()
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = commonModel.date
                    calendar.add(Calendar.MONTH, 1)
                    monthModels = Utils.getMonthlyAmount(
                        commonModel.principalAmount,
                        commonModel.terms,
                        commonModel.interestRate,
                        commonModel.monthlyPayment,
                        calendar.time
                    )
                    endDateYear = Date()
                    val calendar2 = Calendar.getInstance()
                    calendar2.timeInMillis = calendar.timeInMillis
                    calendar2.add(Calendar.MONTH, Utils.decimalFormat.format(commonModel.terms).toInt())
                    Log.d("TIMEINMILLE", "" + calendar2.timeInMillis)
                    endDateYear = calendar2.time
                    yearModels = Utils.getYearlyAmount(monthModels, calendar.time, endDateYear!!)
                } else if (intent.hasExtra("InterestOnly")) {
                    interestOnly = intent.serializableExtraCompat<CommonModel>("InterestOnly") ?: CommonModel()
                    val calendar3 = Calendar.getInstance()
                    calendar3.timeInMillis = interestOnly.date
                    calendar3.add(Calendar.MONTH, 1)
                    monthModels = Utils.getMonthlyInterest(
                        interestOnly.principalAmount,
                        interestOnly.terms,
                        interestOnly.interestRate,
                        interestOnly.monthlyPayment,
                        interestOnly.interestPeriod,
                        calendar3.time
                    )
                    endDateYear = Date()
                    val calendar4 = Calendar.getInstance()
                    calendar4.timeInMillis = calendar3.timeInMillis
                    calendar4.add(Calendar.MONTH, Utils.decimalFormat.format(interestOnly.terms).toInt())
                    endDateYear = calendar4.time
                    yearModels = Utils.getYearlyAmount(monthModels, calendar3.time, endDateYear!!)
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

    private fun setData(list: ArrayList<MonthModel>) {
        monthAdapter.setList(list, isMonthly)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            dialogUtils = DialogUtils(this, monthAdapter.getMonthModels(), isMonthly, GraphUtils.COMON_GRAPH)
            dialogUtils?.setupDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
