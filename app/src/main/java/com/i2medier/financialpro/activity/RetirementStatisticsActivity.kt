package com.i2medier.financialpro.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.RetirementAdapter
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.DialogUtils
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RetirementStatisticsActivity : AppCompatActivity() {
    private var monthModels: ArrayList<MonthModel> = ArrayList()
    private var retirementAdapter: RetirementAdapter? = null
    private var dialogUtils: DialogUtils? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var rbMonth: RadioButton
    private lateinit var rbYear: RadioButton
    private lateinit var rvList: RecyclerView
    private var sameModel: ArrayList<MonthModel> = ArrayList()
    private lateinit var toolBar: Toolbar
    private lateinit var txtInterest: TextView
    private lateinit var txtPaid: TextView
    private lateinit var txtPrincipal: TextView
    private var yearModels: ArrayList<MonthModel> = ArrayList()
    private var isMonthly = true

    private var currentAge = 30
    private var retirementAge = 65
    private var currentSavings = 0.0
    private var monthlyContribution = 0.0
    private var expectedReturn = 0.07

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retirement_statistics)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById(R.id.banner))
        adAdmob.FullscreenAd(this)

        monthModels = ArrayList()
        yearModels = ArrayList()
        sameModel = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        setUpToolbar()
        rvList = findViewById(R.id.rvList)
        txtPrincipal = findViewById(R.id.txtPrincipal)
        txtInterest = findViewById(R.id.txtInterest)
        txtPaid = findViewById(R.id.txtPaid)
        rbMonth = findViewById(R.id.rbMonth)
        rbYear = findViewById(R.id.rbYear)
        progressBar = findViewById(R.id.progressBar)

        currentAge = intent.getIntExtra("currentAge", 30)
        retirementAge = intent.getIntExtra("retirementAge", 65)
        currentSavings = intent.getDoubleExtra("currentSavings", 0.0)
        monthlyContribution = intent.getDoubleExtra("monthlyContribution", 0.0)
        expectedReturn = intent.getDoubleExtra("expectedReturn", 0.07)

        rbYear.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                isMonthly = false
                setValue()
            }
        }
        rbMonth.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                isMonthly = true
                setValue()
            }
        }
        setValue()
    }

    private fun setUpToolbar() {
        toolBar.setTitle(R.string.retirement_calculator)
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun setValue() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            withContext(Dispatchers.Default) {
                monthModels.clear()
                yearModels.clear()

                val yearsToRetirement = retirementAge - currentAge
                val monthlyRate = expectedReturn / 12.0
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                var balance = currentSavings

                for (year in 0 until yearsToRetirement) {
                    var yearPrincipal = 0.0
                    var yearInterest = 0.0
                    var yearTotal = 0.0

                    for (month in 0 until 12) {
                        val interestEarned = balance * monthlyRate
                        balance += interestEarned + monthlyContribution
                        yearPrincipal += monthlyContribution
                        yearInterest += interestEarned
                        yearTotal += monthlyContribution + interestEarned

                        val monthModel = MonthModel()
                        monthModel.PrincipalAmount = monthlyContribution
                        monthModel.Interest = interestEarned
                        monthModel.Balance = balance
                        monthModel.TotalPaid = yearTotal
                        monthModel.year = currentYear + year
                        monthModel.month = month + 1
                        calendar.set(Calendar.YEAR, currentYear + year)
                        calendar.set(Calendar.MONTH, month)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        monthModel.setDate(calendar.time)
                        monthModels.add(monthModel)
                    }

                    val yearModel = MonthModel()
                    yearModel.PrincipalAmount = yearPrincipal
                    yearModel.Interest = yearInterest
                    yearModel.Balance = balance
                    yearModel.TotalPaid = yearTotal
                    yearModel.year = currentYear + year
                    yearModels.add(yearModel)
                }
            }

            progressBar.visibility = View.GONE
            if (isMonthly) {
                retirementAdapter = RetirementAdapter(this@RetirementStatisticsActivity, monthModels, true)
                sameModel = ArrayList(monthModels)
            } else {
                retirementAdapter = RetirementAdapter(this@RetirementStatisticsActivity, yearModels, false)
                sameModel = ArrayList(yearModels)
            }
            rvList.layoutManager = LinearLayoutManager(this@RetirementStatisticsActivity)
            rvList.adapter = retirementAdapter

            var totalPrincipal = 0.0
            var totalInterest = 0.0
            var totalPaid = 0.0
            for (model in sameModel) {
                totalPrincipal += model.PrincipalAmount
                totalInterest += model.Interest
                totalPaid += model.TotalPaid
            }

            txtPrincipal.text = Utils.decimalFormat.format(totalPrincipal)
            txtInterest.text = Utils.decimalFormat.format(totalInterest)
            txtPaid.text = Utils.decimalFormat.format(totalPaid)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            dialogUtils = DialogUtils(this, sameModel, isMonthly, GraphUtils.SIMPLE_INTEREST_GRAPH)
            dialogUtils?.setupDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
