package com.i2medier.financialpro.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.MonthModel
class DialogUtils(
    private val context: Context,
    private val list: ArrayList<MonthModel>,
    private val isMonthWise: Boolean,
    private var graphType: Int
) {
    private lateinit var barChart: BarChart
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var ivClose: ImageView
    private lateinit var llTaxINSPMI: LinearLayout
    private lateinit var txtLabel: TextView
    fun setupDialog() {
        bottomSheetDialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme2)
        val inflate = LayoutInflater.from(context).inflate(R.layout.dialog_chart, LinearLayout(context), false)
        bottomSheetDialog.setContentView(inflate)
        if (bottomSheetDialog.isShowing) {
            bottomSheetDialog.dismiss()
        }
        barChart = inflate.findViewById(R.id.barchart)
        txtLabel = inflate.findViewById(R.id.txtLabel)
        ivClose = inflate.findViewById(R.id.ivClose)
        llTaxINSPMI = inflate.findViewById(R.id.llTaxINSPMI)
        llTaxINSPMI.visibility = if (graphType == GraphUtils.MORTGAGE_GRAPH) View.VISIBLE else View.GONE
        GraphUtils(barChart, list, context, isMonthWise, graphType).setupBarChartData()
        txtLabel.text = if (isMonthWise) context.getString(R.string.month) else context.getString(R.string.year)
        ivClose.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.show()
    }
}
