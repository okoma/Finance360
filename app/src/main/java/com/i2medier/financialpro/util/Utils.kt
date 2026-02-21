package com.i2medier.financialpro.util

import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import com.i2medier.financialpro.model.MonthModel
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow

object Utils {
    @JvmField
    var Interest: Double = 0.0

    @JvmField
    var Paid: Double = 0.0

    @JvmField
    var Principal: Double = 0.0

    const val REQUEST: Int = 112

    @JvmField
    var isMonthly: Boolean = true

    @JvmField
    var isYearly: Boolean = false

    @JvmField
    var mTaxInsPMI: Double = 0.0

    @JvmField
    val sdf: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    @JvmField
    val decimalFormat: DecimalFormat = DecimalFormat("#.##")

    @JvmField
    val DOUBLE_EPSILON: Double = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

    @JvmStatic
    fun getInterestOnly(d: Double, d2: Double): Double {
        return ((d / 12.0) / 100.0) * d2
    }

    @JvmStatic
    fun getTotalInterest(d: Double, d2: Double, d3: Double): Double {
        return (d * d2) - d3
    }

    @JvmStatic
    fun hasPermissions(context: Context?, vararg strArr: String?): Boolean {
        if (Build.VERSION.SDK_INT < 23 || context == null || strArr.isEmpty()) {
            return true
        }
        for (str in strArr) {
            if (str != null && ActivityCompat.checkSelfPermission(context, str) != 0) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun CALDATE(str: String): Date {
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        var date = Date()
        try {
            date = simpleDateFormat.parse(str) ?: Date()
            println(simpleDateFormat.format(date))
            return date
        } catch (e: ParseException) {
            e.printStackTrace()
            return date
        }
    }

    @JvmStatic
    fun getMonthlyPayment(d: Double, d2: Double, d3: Double): Double {
        val d4 = (d2 / 100.0) / 12.0
        val pow = (d4 + 1.0).pow(d3)
        return ((d * d4) * pow) / (pow - 1.0)
    }

    @JvmStatic
    fun getYearlyAmount(arrayList: ArrayList<MonthModel>?, date: Date, date2: Date): ArrayList<MonthModel> {
        var i: Int
        val arrayList2 = ArrayList<MonthModel>()
        val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val parseInt = simpleDateFormat.format(date).toInt()
        var parseInt2 = simpleDateFormat.format(date2).toInt()
        val simpleDateFormat2 = SimpleDateFormat("MM", Locale.getDefault())
        var parseInt3 = simpleDateFormat2.format(date).toInt()
        var parseInt4 = simpleDateFormat2.format(date2).toInt()

        if (arrayList != null && arrayList.size > 0) {
            var i2 = parseInt
            var i3 = 0
            var d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            var d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

            while (i2 <= parseInt2 && i3 < arrayList.size) {
                if (parseInt == parseInt2) {
                    i = arrayList.size
                    i3 = 0
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1
                    i3 = 0
                } else {
                    i = if (i2 == parseInt2) (i3 + parseInt4) - 1 else i3 + 12
                }

                val i4 = parseInt3
                val i5 = parseInt4
                var d3 = d
                var d4 = d2
                var d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                var d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

                while (i3 < i) {
                    d5 += arrayList[i3].PrincipalAmount
                    d6 += arrayList[i3].Interest
                    d3 += d6
                    d4 += d5
                    i3++
                }

                val monthModel = MonthModel()
                monthModel.setDate(Date())
                monthModel.PrincipalAmount = d5
                monthModel.Interest = d6
                monthModel.TotalPaid = d5 + d6
                monthModel.TotalPrincipal = d4
                monthModel.TotalInterest = d3
                monthModel.year = i2
                monthModel.Balance = arrayList[i - 1].Balance
                arrayList2.add(monthModel)
                i2++
                d = d3
                d2 = d4
                parseInt3 = i4
                i3 = i
                parseInt2 = parseInt2
                parseInt4 = i5
            }
        }

        return arrayList2
    }

    @JvmStatic
    fun getMonthlyAmount(d: Double, d2: Double, d3: Double, d4: Double, date: Date): ArrayList<MonthModel> {
        val arrayList = ArrayList<MonthModel>()
        var d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        var d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        var d7 = d

        for (i in 0 until d2.toInt()) {
            val monthModel = MonthModel()
            val d8 = ((d3 / 12.0) * d7) / 100.0
            val d9 = d4 - d8
            d7 -= d9
            val d10 = d9 + d8
            d5 += d8
            d6 += d9
            if (d7 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d7 = 0.0
            }

            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MONTH, i)

            monthModel.PrincipalAmount = d9
            monthModel.Interest = d8
            monthModel.Balance = d7
            monthModel.TotalPaid = d10
            monthModel.TotalPrincipal = d6
            monthModel.TotalInterest = d5
            monthModel.setDate(calendar.time)
            arrayList.add(monthModel)
            Principal += d9
            Interest += d8
            Paid += d10
        }

        return arrayList
    }

    @JvmStatic
    fun getYearlyMortgage(arrayList: ArrayList<MonthModel>?, date: Date, date2: Date): ArrayList<MonthModel> {
        var i: Int
        val arrayList2 = ArrayList<MonthModel>()
        val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        var parseInt = simpleDateFormat.format(date).toInt()
        var parseInt2 = simpleDateFormat.format(date2).toInt()
        val simpleDateFormat2 = SimpleDateFormat("MM", Locale.getDefault())
        var parseInt3 = simpleDateFormat2.format(date).toInt()
        var parseInt4 = simpleDateFormat2.format(date2).toInt()

        if (arrayList != null && arrayList.size > 0) {
            var i2 = parseInt
            var i3 = 0
            var d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            var d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

            while (i2 <= parseInt2 && i3 < arrayList.size) {
                if (parseInt == parseInt2) {
                    i = arrayList.size
                    i3 = 0
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1
                    i3 = 0
                } else {
                    i = if (i2 == parseInt2) (i3 + parseInt4) - 1 else i3 + 12
                }

                val i4 = parseInt3
                val i5 = parseInt4
                val i6 = parseInt2
                val i7 = parseInt
                var d3 = d
                var d4 = d2
                var d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                var d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                var d7 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

                while (i3 < i) {
                    d6 += arrayList[i3].PrincipalAmount
                    d7 += arrayList[i3].Interest
                    d5 += arrayList[i3].TaxInsPMI
                    d3 += d7
                    d4 += d6
                    i3++
                }

                val monthModel = MonthModel()
                monthModel.setDate(Date())
                monthModel.PrincipalAmount = d6
                monthModel.Interest = d7
                monthModel.TaxInsPMI = d5
                monthModel.TotalTax = d5
                monthModel.TotalInterest = d3
                monthModel.TotalPrincipal = d4
                monthModel.TotalPaid = d6 + d7 + d5
                monthModel.year = i2
                monthModel.Balance = arrayList[i - 1].Balance
                arrayList2.add(monthModel)
                i2++
                d = d3
                d2 = d4
                i3 = i
                parseInt = i7
                parseInt2 = i6
                parseInt3 = i4
                parseInt4 = i5
            }
        }

        return arrayList2
    }

    @JvmStatic
    fun getMonthlyMortgage(
        d: Double,
        d2: Double,
        d3: Double,
        d4: Double,
        d5: Double,
        d6: Double,
        date: Date,
        d7: Double
    ): ArrayList<MonthModel> {
        var d8: Double
        var d9: Double
        var arrayList = ArrayList<MonthModel>()
        var d10 = d - d7
        var i = 0
        var d11 = d6
        var d12 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        var d13 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
        var d14 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

        while (i < d2) {
            val monthModel = MonthModel()
            if ((80.0 * d) / 100.0 > d10) {
                d11 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            }

            val d15 = ((d3 / 12.0) * d10) / 100.0
            val d16 = d4 - d15
            val d17 = d10 - d16
            val arrayList2 = arrayList
            val d18 = d16 + d15 + d5 + d11
            val d19 = d5 + d11
            val d20 = d14 + d19
            d12 += d15
            d13 += d16

            if (d17 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d8 = d11
                d9 = 0.0
            } else {
                d8 = d11
                d9 = d17
            }

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date.time
            calendar.add(Calendar.MONTH, i)

            monthModel.PrincipalAmount = d16
            monthModel.Interest = d15
            monthModel.Balance = d9
            val d21 = d9
            monthModel.TotalPaid = d18
            monthModel.TotalPrincipal = d13
            monthModel.TaxInsPMI = d19
            monthModel.TotalInterest = d12
            monthModel.TotalTax = d20
            monthModel.setDate(calendar.time)
            arrayList2.add(monthModel)
            Principal += d16
            Interest += d15
            mTaxInsPMI += d5
            Paid += d18
            i++
            d11 = d8
            d14 = d20
            arrayList = arrayList2
            d10 = d21
        }

        return arrayList
    }

    @JvmStatic
    fun getYearlyCompound(arrayList: ArrayList<MonthModel>?, date: Date, date2: Date): ArrayList<MonthModel> {
        var i: Int
        val arrayList2 = ArrayList<MonthModel>()
        val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val parseInt = simpleDateFormat.format(date).toInt()
        var parseInt2 = simpleDateFormat.format(date2).toInt()
        val simpleDateFormat2 = SimpleDateFormat("MM", Locale.getDefault())
        var parseInt3 = simpleDateFormat2.format(date).toInt()
        var parseInt4 = simpleDateFormat2.format(date2).toInt()

        if (arrayList != null && arrayList.size > 0) {
            var i2 = parseInt
            var i3 = 0
            var d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
            var d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

            while (i2 <= parseInt2 && i3 < arrayList.size) {
                if (parseInt == parseInt2) {
                    i = arrayList.size
                    i3 = 1
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1
                    i3 = 0
                } else {
                    i = if (i2 == parseInt2) (i3 + parseInt4) - 1 else i3 + 12
                }

                val i4 = parseInt3
                val i5 = parseInt4
                var d3 = d
                var d4 = d2
                var d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                var d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON

                while (i3 < i) {
                    d5 += arrayList[i3].PrincipalAmount
                    d6 += arrayList[i3].Interest
                    d3 += d6
                    d4 += d5
                    i3++
                }

                val monthModel = MonthModel()
                monthModel.setDate(Date())
                monthModel.PrincipalAmount = d5
                monthModel.Interest = d6
                monthModel.TotalPrincipal = d4
                monthModel.TotalPaid = d5 + d6
                monthModel.TotalInterest = d3
                monthModel.year = i2
                monthModel.Balance = arrayList[i - 1].Balance
                arrayList2.add(monthModel)
                i2++
                d = d3
                d2 = d4
                parseInt3 = i4
                i3 = i
                parseInt2 = parseInt2
                parseInt4 = i5
            }
        }

        return arrayList2
    }

    @JvmStatic
    fun getMonthlyCompound(d: Double, d2: Double, d3: Double, d4: Double, date: Date): ArrayList<MonthModel> {
        val arrayList = ArrayList<MonthModel>()
        var i = 0
        var d5 = 0.0
        var d6 = 0.0
        var d7 = d

        while (i < d2) {
            val monthModel = MonthModel()
            val d8 = if (i == 0) d4 + d else d4
            val d9 = ((d3 / 12.0) / 100.0) * (d7 + d4)
            d7 = if (i == 0) d9 + d + d4 else d9 + d8 + d7
            d5 += d9
            d6 += d8

            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MONTH, i)

            monthModel.PrincipalAmount = d8
            monthModel.Interest = d9
            monthModel.TotalInterest = d5
            monthModel.TotalPrincipal = d6
            monthModel.Balance = d7
            monthModel.setDate(calendar.time)
            arrayList.add(monthModel)
            Principal += d8
            Interest += d9
            i++
        }

        Paid = d7
        return arrayList
    }

    @JvmStatic
    fun getMonthlyInterest(d: Double, d2: Double, d3: Double, d4: Double, d5: Double, date: Date): ArrayList<MonthModel> {
        val arrayList = ArrayList<MonthModel>()
        var i = 0
        var d6 = d
        var d7 = 0.0

        while (true) {
            val d8 = i.toDouble()
            if (d8 >= d2) {
                return arrayList
            }

            val monthModel = MonthModel()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = date.time
            calendar.add(Calendar.MONTH, i)
            calendar.time

            if (d8 < d5) {
                val d9 = ((d3 / 12.0) * d6) / 100.0
                val d10 = d9 + com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                d7 += com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                if (d6 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    d6 = 0.0
                }
                monthModel.PrincipalAmount = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                monthModel.Interest = d9
                monthModel.Balance = d6
                monthModel.TotalPaid = d10
                monthModel.TotalPrincipal = d7
                monthModel.setDate(calendar.time)
                arrayList.add(monthModel)
                Principal += com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON
                Interest += d9
                Paid += d10
            } else {
                val d11 = ((d3 / 12.0) * d6) / 100.0
                val d12 = d4 - d11
                d6 -= d12
                val d13 = d12 + d11
                d7 += d12
                if (d6 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    d6 = 0.0
                }
                monthModel.PrincipalAmount = d12
                monthModel.Interest = d11
                monthModel.Balance = d6
                monthModel.TotalPrincipal = d7
                monthModel.TotalPaid = d13
                monthModel.setDate(calendar.time)
                arrayList.add(monthModel)
                Principal += d12
                Interest += d11
                Paid += d13
            }
            i++
        }
    }

    @JvmStatic
    fun getYearlyRefinanceAmount(d: Double, d2: Double, d3: Double, d4: Double, i: Int): ArrayList<MonthModel> {
        val arrayList = ArrayList<MonthModel>()
        var d5 = d
        var i2 = i

        for (i3 in 0 until d2.toInt()) {
            val monthModel = MonthModel()
            val d6 = ((d3 / 12.0) * d5) / 100.0
            val d7 = d4 - d6
            d5 -= d7
            val d8 = d7 + d6
            if (d5 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d5 = 0.0
            }
            monthModel.PrincipalAmount = d7
            monthModel.Interest = d6
            monthModel.Balance = d5
            monthModel.TotalPaid = d8
            monthModel.year = i2
            arrayList.add(monthModel)
            i2++
            Principal += d7
            Interest += d6
            Paid += d8
        }

        return arrayList
    }

    @JvmStatic
    fun getYearlySimpleInterest(d: Double, d2: Double, d3: Double, i: Int): ArrayList<MonthModel> {
        val arrayList = ArrayList<MonthModel>()
        var d4 = 0.0
        var d5 = d
        var year = i

        for (i2 in 0 until d2.toInt()) {
            val monthModel = MonthModel()
            d5 += d3
            d4 += d3
            monthModel.PrincipalAmount = d
            monthModel.Interest = d3
            monthModel.TotalInterest = d4
            monthModel.TotalPrincipal = d
            monthModel.Balance = d5
            monthModel.year = year
            arrayList.add(monthModel)
            year++
        }

        return arrayList
    }

    @JvmStatic
    fun getYearlyLoanCompare(d: Double, d2: Double, d3: Double, d4: Double): ArrayList<MonthModel> {
        var d5: Double
        val arrayList = ArrayList<MonthModel>()
        val d6 = 12.0
        val i = (d2 * 12.0).toInt()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        var i2 = calendar.get(Calendar.YEAR)
        var i3 = 0
        var d7 = d
        var d8 = 0.0
        var d9 = 0.0
        var d10 = 0.0

        while (i3 < i) {
            val d11 = ((d3 / d6) * d7) / 100.0
            val d12 = d4 - d11
            d7 -= d12
            val d13 = d12 + d11
            if (d7 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d7 = 0.0
            }
            if (i2 != calendar.get(Calendar.YEAR)) {
                val monthModel = MonthModel()
                monthModel.PrincipalAmount = d8
                monthModel.Interest = d9
                monthModel.Balance = d7
                monthModel.TotalPaid = d10
                monthModel.year = i2
                arrayList.add(monthModel)
                i2 = calendar.get(Calendar.YEAR)
                d5 = 0.0
                d8 = 0.0
                d9 = 0.0
            } else {
                d5 = d10
            }
            d8 += d12
            d9 += d11
            d10 = d5 + d13
            Principal += d12
            Interest += d11
            Paid += d13
            calendar.add(Calendar.MONTH, 1)
            i3++
        }

        val monthModel2 = MonthModel()
        monthModel2.PrincipalAmount = d8
        monthModel2.Interest = d9
        monthModel2.Balance = d7
        monthModel2.TotalPaid = d10
        monthModel2.year = calendar.get(Calendar.YEAR)
        arrayList.add(monthModel2)
        return arrayList
    }
}
