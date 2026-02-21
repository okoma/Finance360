package com.i2medier.financialpro.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.adapter.SimpleInterestAdapter
import com.i2medier.financialpro.model.CommonModel
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.DialogUtils
import com.i2medier.financialpro.util.GraphUtils
import com.i2medier.financialpro.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimpleInterestStatisticsActivity : AppCompatActivity() {
    private var commonModel: CommonModel = CommonModel()
    private var dialogUtils: DialogUtils? = null
    private var monthModels: ArrayList<MonthModel> = ArrayList()
    private lateinit var rvList: RecyclerView
    private var simpleInterestAdapter: SimpleInterestAdapter? = null
    private lateinit var toolBar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_interest_statistics)

        val adAdmob = AdAdmob(this)
        adAdmob.BannerAd(findViewById(R.id.banner), this)
        adAdmob.FullscreenAd(this)

        init()
        setUpToolbar()
        setValue()
    }

    private fun init() {
        monthModels = ArrayList()
        toolBar = findViewById(R.id.toolBar)
        rvList = findViewById(R.id.rvList)
        rvList.layoutManager = LinearLayoutManager(this)
    }

    private fun setUpToolbar() {
        toolBar.title = getString(R.string.simple_interest_calculator)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setValue() {
        lifecycleScope.launch {
            if (!intent.hasExtra("SimpleInterest")) {
                return@launch
            }
            commonModel = intent.serializableExtraCompat<CommonModel>("SimpleInterest") ?: CommonModel()
            monthModels = withContext(Dispatchers.Default) {
                Utils.getYearlySimpleInterest(
                    commonModel.principalAmount,
                    commonModel.terms,
                    commonModel.interestAmount,
                    commonModel.year
                )
            }
            simpleInterestAdapter = SimpleInterestAdapter(this@SimpleInterestStatisticsActivity, monthModels)
            rvList.adapter = simpleInterestAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.statasic_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.statistics) {
            dialogUtils = DialogUtils(this, monthModels, false, GraphUtils.SIMPLE_INTEREST_GRAPH)
            dialogUtils?.setupDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
