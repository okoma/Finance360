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
import com.i2medier.financialpro.adapter.MonthAdapter
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.DialogUtils
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class CreditCardPayoffStatisticsActivity : AppCompatActivity() {
    private var monthModels: ArrayList<MonthModel> = ArrayList()
    private var monthAdapter: MonthAdapter? = null
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

    private var creditCardBalance = 0.0
    private var apr = 0.0
    private var monthlyPayment = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_credit_card_payoff_statistics)

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

        creditCardBalance = intent.getDoubleExtra("creditCardBalance", 0.0)
        apr = intent.getDoubleExtra("apr", 0.0) / 100.0
        monthlyPayment = intent.getDoubleExtra("monthlyPayment", 0.0)

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
        toolBar.setTitle(R.string.credit_card_payoff_calculator)
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

                val monthlyRate = apr / 12.0
                var balance = creditCardBalance
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)
                var totalPaid = 0.0

                var monthCount = 0
                while (balance > 0.01 && monthCount < 600) {
                    val interestPayment = balance * monthlyRate
                    val principalPayment = kotlin.math.min(monthlyPayment - interestPayment, balance)
                    balance -= principalPayment
                    totalPaid += monthlyPayment

                    val monthModel = MonthModel()
                    monthModel.PrincipalAmount = principalPayment
                    monthModel.Interest = interestPayment
                    monthModel.Balance = balance
                    monthModel.TotalPaid = totalPaid
                    monthModel.year = currentYear + ((currentMonth + monthCount) / 12)
                    monthModel.month = ((currentMonth + monthCount) % 12) + 1
                    calendar.set(Calendar.MONTH, currentMonth + monthCount)
                    monthModel.setDate(calendar.time)
                    monthModels.add(monthModel)
                    monthCount++
                }

                if (monthModels.isNotEmpty()) {
                    var year = monthModels[0].year
                    var yearPrincipal = 0.0
                    var yearInterest = 0.0
                    var yearTotal = 0.0

                    for (model in monthModels) {
                        if (model.year == year) {
                            yearPrincipal += model.PrincipalAmount
                            yearInterest += model.Interest
                            yearTotal += model.PrincipalAmount + model.Interest
                        } else {
                            val yearModel = MonthModel()
                            yearModel.PrincipalAmount = yearPrincipal
                            yearModel.Interest = yearInterest
                            yearModel.TotalPaid = yearTotal
                            yearModel.year = year
                            yearModel.Balance = model.Balance
                            yearModels.add(yearModel)

                            year = model.year
                            yearPrincipal = model.PrincipalAmount
                            yearInterest = model.Interest
                            yearTotal = model.PrincipalAmount + model.Interest
                        }
                    }

                    if (yearPrincipal > 0) {
                        val yearModel = MonthModel()
                        yearModel.PrincipalAmount = yearPrincipal
                        yearModel.Interest = yearInterest
                        yearModel.TotalPaid = yearTotal
                        yearModel.year = year
                        yearModel.Balance = monthModels[monthModels.size - 1].Balance
                        yearModels.add(yearModel)
                    }
                }
            }

            progressBar.visibility = View.GONE

            if (isMonthly) {
                monthAdapter = MonthAdapter(this@CreditCardPayoffStatisticsActivity, monthModels, true)
                sameModel = ArrayList(monthModels)
            } else {
                monthAdapter = MonthAdapter(this@CreditCardPayoffStatisticsActivity, yearModels, false)
                sameModel = ArrayList(yearModels)
            }
            rvList.layoutManager = LinearLayoutManager(this@CreditCardPayoffStatisticsActivity)
            rvList.adapter = monthAdapter

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
