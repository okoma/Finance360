package com.i2medier.financialpro.model

import java.io.Serializable

class CommonModel : Serializable {
    @JvmField
    var PMI: Double = 0.0
    @JvmField
    var TaxIns: Double = 0.0
    var date: Long = 0L
    var downPayment: Double = 0.0
    var interestAmount: Double = 0.0
    var interestPeriod: Double = 0.0
    var interestRate: Double = 0.0
    var interestRate2: Double = 0.0
    var month: Int = 0
    var monthlyPayment: Double = 0.0
    var monthlyPayment2: Double = 0.0
    var owedTrade: Double = 0.0
    var principalAmount: Double = 0.0
    var principalAmount2: Double = 0.0
    var propertyInsurance: Double = 0.0
    var propertyTax: Double = 0.0
    var regularInvestment: Double = 0.0
    var residualValue: Double = 0.0
    var saleTax: Double = 0.0
    var startDate: Double = 0.0
    var terms: Double = 0.0
    var terms2: Double = 0.0
    var tradeAmount: Double = 0.0
    var year: Int = 0
}
