package com.i2medier.financialpro.util

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.GraphModel
import com.i2medier.financialpro.model.MonthModel
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class GraphUtils {
    private var pieEntryLabels: ArrayList<String>? = null
    private var barChart: BarChart? = null
    private var context: Context
    private var dataSets: ArrayList<IBarDataSet>? = null
    private var entries: ArrayList<PieEntry>? = null
    private var graphModelArrayList: ArrayList<GraphModel>? = null
    private var graphType: Int = 0
    private var isMonthWise: Boolean = false
    private var monthModels: ArrayList<MonthModel>? = null
    private var pieChart: PieChart? = null
    private var pieData: PieData? = null
    private var pieDataSet: PieDataSet? = null
    private val MONTH_FORMATE = "MMM-yyyy"

    constructor(pieChart: PieChart, arrayList: ArrayList<GraphModel>, context: Context) {
        this.pieChart = pieChart
        this.graphModelArrayList = arrayList
        this.context = context
    }

    constructor(barChart: BarChart, arrayList: ArrayList<MonthModel>, context: Context, z: Boolean, i: Int) {
        this.barChart = barChart
        this.monthModels = arrayList
        this.context = context
        this.isMonthWise = z
        this.graphType = i
    }

    fun setupPieData() {
        val font = ResourcesCompat.getFont(context, R.font.regular)
        entries = ArrayList()
        pieEntryLabels = ArrayList()
        pieDataSet = PieDataSet(entries, "projects")

        val localPieChart = pieChart ?: return
        localPieChart.setUsePercentValues(true)
        localPieChart.setDrawHoleEnabled(true)
        localPieChart.setHoleColor(ContextCompat.getColor(context, R.color.colorWhite))
        localPieChart.setTransparentCircleAlpha(110)
        localPieChart.holeRadius = 58.0f
        localPieChart.setEntryLabelColor(ContextCompat.getColor(context, R.color.colorDark))
        localPieChart.setEntryLabelTypeface(font)

        val legend = localPieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.isEnabled = false

        val arrayList = ArrayList<Int>()
        val graphList = graphModelArrayList.orEmpty()
        for (i in graphList.indices) {
            entries?.add(PieEntry(graphList[i].value.toFloat(), graphList[i].label))
            arrayList.add(graphList[i].color)
        }

        pieDataSet?.setColors(arrayList)
        pieDataSet?.valueLinePart1OffsetPercentage = 80.0f
        pieDataSet?.valueLinePart1Length = 0.2f
        pieDataSet?.valueLinePart2Length = 0.4f
        pieDataSet?.sliceSpace = 3.0f
        pieDataSet?.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        pieData = PieData(pieDataSet)
        pieData?.setValueFormatter(PercentFormatter())
        pieData?.setValueTextSize(12.0f)
        pieData?.setValueTextColor(ContextCompat.getColor(context, R.color.colorWhite))

        localPieChart.data = pieData
        localPieChart.description.isEnabled = false
        (localPieChart.data as PieData).setDrawValues(true)
        localPieChart.animateY(3000)
        localPieChart.invalidate()
    }

    fun setupPieData(centerText: String?) {
        setupPieData()
        if (centerText != null) {
            pieChart?.centerText = centerText
        }
    }

    fun setupBarChartData() {
        val localBarChart = barChart ?: return
        val localMonthModels = monthModels ?: return

        localBarChart.axisRight.removeAllLimitLines()
        localBarChart.axisLeft.removeAllLimitLines()
        localBarChart.clear()
        localBarChart.axisLeft.resetAxisMaximum()
        localBarChart.axisLeft.resetAxisMinimum()
        localBarChart.axisRight.resetAxisMaximum()
        localBarChart.axisRight.resetAxisMinimum()

        val arrayList = ArrayList<BarEntry>()
        val arrayList2 = ArrayList<Int>()

        if (graphType == COMON_GRAPH) {
            for (i in localMonthModels.indices) {
                val fArr = floatArrayOf(
                    localMonthModels[i].PrincipalAmount.toFloat(),
                    localMonthModels[i].Interest.toFloat()
                )
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor1))
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor2))
                arrayList.add(BarEntry(i.toFloat(), fArr, arrayList2))
            }
        } else if (graphType == SIMPLE_INTEREST_GRAPH) {
            for (i in localMonthModels.indices) {
                val fArr = floatArrayOf(
                    localMonthModels[i].TotalPrincipal.toFloat(),
                    localMonthModels[i].TotalInterest.toFloat()
                )
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor1))
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor2))
                arrayList.add(BarEntry(i.toFloat(), fArr, arrayList2))
            }
        } else if (graphType == MORTGAGE_GRAPH) {
            for (i in localMonthModels.indices) {
                val fArr = floatArrayOf(
                    localMonthModels[i].TotalPrincipal.toFloat(),
                    localMonthModels[i].TotalInterest.toFloat(),
                    localMonthModels[i].TotalTax.toFloat()
                )
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor1))
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor2))
                arrayList2.add(ContextCompat.getColor(context, R.color.graphcolor3))
                arrayList.add(BarEntry(i.toFloat(), fArr, arrayList2))
            }
        }

        if (localBarChart.data != null && (localBarChart.data as BarData).dataSetCount > 0) {
            ((localBarChart.data as BarData).getDataSetByIndex(0) as BarDataSet).values = arrayList
            (localBarChart.data as BarData).notifyDataChanged()
            localBarChart.notifyDataSetChanged()
            return
        }

        val barDataSet = BarDataSet(arrayList, "Statistic")
        barDataSet.setDrawIcons(false)
        barDataSet.setColors(arrayList2)
        dataSets = ArrayList()
        dataSets?.add(barDataSet)

        val barData = BarData(dataSets)
        barData.barWidth = 0.5f
        barData.setValueFormatter(MyValueFormatter())
        barData.setValueTextColor(0)

        localBarChart.data = barData
        localBarChart.setScaleEnabled(false)
        localBarChart.axisLeft.setDrawGridLines(false)
        localBarChart.xAxis.setDrawGridLines(false)
        localBarChart.axisLeft.setDrawLabels(true)
        localBarChart.setDrawBorders(false)
        localBarChart.setDrawGridBackground(false)

        val xAxis = localBarChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.isGranularityEnabled = true
        xAxis.isEnabled = true
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = CustomXAxis(localMonthModels)
        xAxis.setDrawAxisLine(true)

        val axisLeft: YAxis = localBarChart.axisLeft
        axisLeft.isEnabled = false
        axisLeft.setDrawGridLines(false)
        axisLeft.setDrawAxisLine(false)

        val axisRight: YAxis = localBarChart.axisRight
        axisRight.isEnabled = true
        axisRight.setDrawGridLines(false)
        axisRight.setDrawAxisLine(false)

        axisLeft.axisMinimum = 0.0f
        axisRight.axisMinimum = 0.0f

        localBarChart.description.isEnabled = false
        localBarChart.legend.isEnabled = true
        localBarChart.notifyDataSetChanged()
        localBarChart.setFitBars(true)
        localBarChart.legend.isEnabled = false
        localBarChart.invalidate()
        (localBarChart.data as BarData).notifyDataChanged()
        localBarChart.notifyDataSetChanged()
        localBarChart.setVisibleXRangeMaximum(10.0f)
        localBarChart.moveViewToX(localMonthModels.size.toFloat())
    }

    fun getDate(date: Date, str: String): String {
        return SimpleDateFormat(str, Locale.getDefault()).format(date)
    }

    inner class CustomXAxis(private val monthModels: List<MonthModel>) : ValueFormatter() {
        override fun getAxisLabel(f: Float, axisBase: AxisBase?): String {
            val i = f.toInt()
            if (i < 0 || i >= monthModels.size) {
                return ""
            }
            return if (isMonthWise) {
                getDate(monthModels[i].getDateofGraph(), MONTH_FORMATE)
            } else {
                monthModels[i].year.toString()
            }
        }
    }

    inner class MyValueFormatter : ValueFormatter() {
        override fun getBarLabel(barEntry: BarEntry): String {
            val f = barEntry.y
            return if (f <= com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                ""
            } else {
                "${f.toInt()} "
            }
        }
    }

    companion object {
        @JvmField
        var COMON_GRAPH: Int = 1

        @JvmField
        var SIMPLE_INTEREST_GRAPH: Int = 2

        @JvmField
        var MORTGAGE_GRAPH: Int = 3
    }
}
