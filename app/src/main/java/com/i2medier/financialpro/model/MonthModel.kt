package com.i2medier.financialpro.model

import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonthModel() : Parcelable {
    var PrincipalAmount: Double = 0.0
    var Interest: Double = 0.0
    var Balance: Double = 0.0
    var TotalPaid: Double = 0.0
    var TaxInsPMI: Double = 0.0
    var TotalInterest: Double = 0.0
    var TotalTax: Double = 0.0
    var TotalPrincipal: Double = 0.0
    var month: Int = 0
    var year: Int = 0
    private var dateValue: Date = Date()

    constructor(
        principalAmount: Double,
        interest: Double,
        balance: Double,
        totalPaid: Double,
        taxInsPMI: Double,
        totalInterest: Double,
        totalTax: Double,
        totalPrincipal: Double,
        month: Int,
        year: Int,
        date: Date
    ) : this() {
        PrincipalAmount = principalAmount
        Interest = interest
        Balance = balance
        TotalPaid = totalPaid
        TaxInsPMI = taxInsPMI
        TotalInterest = totalInterest
        TotalTax = totalTax
        TotalPrincipal = totalPrincipal
        this.month = month
        this.year = year
        this.dateValue = date
    }

    private constructor(parcel: Parcel) : this() {
        PrincipalAmount = parcel.readDouble()
        Interest = parcel.readDouble()
        Balance = parcel.readDouble()
        TotalPaid = parcel.readDouble()
        TaxInsPMI = parcel.readDouble()
        TotalInterest = parcel.readDouble()
        TotalTax = parcel.readDouble()
        TotalPrincipal = parcel.readDouble()
        month = parcel.readInt()
        year = parcel.readInt()
    }

    fun getDate(): String {
        return SimpleDateFormat("MMM-yyyy", Locale.getDefault()).format(dateValue)
    }

    fun getDateofGraph(): Date {
        return dateValue
    }

    fun setDate(date: Date) {
        dateValue = date
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(PrincipalAmount)
        parcel.writeDouble(Interest)
        parcel.writeDouble(Balance)
        parcel.writeDouble(TotalPaid)
        parcel.writeDouble(TaxInsPMI)
        parcel.writeDouble(TotalInterest)
        parcel.writeDouble(TotalTax)
        parcel.writeDouble(TotalPrincipal)
        parcel.writeInt(month)
        parcel.writeInt(year)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MonthModel> = object : Parcelable.Creator<MonthModel> {
            override fun createFromParcel(parcel: Parcel): MonthModel = MonthModel(parcel)
            override fun newArray(size: Int): Array<MonthModel?> = arrayOfNulls(size)
        }
    }
}
