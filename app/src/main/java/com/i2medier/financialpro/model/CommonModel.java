package com.i2medier.financialpro.model;

import java.io.Serializable;


public class CommonModel implements Serializable {
    public double PMI;
    public double TaxIns;
    long date;
    double downPayment;
    double interestAmount;
    double interestPeriod;
    double interestRate;
    double interestRate2;
    int month;
    double monthlyPayment;
    double monthlyPayment2;
    double owedTrade;
    double principalAmount;
    double principalAmount2;
    double propertyInsurance;
    double propertyTax;
    double regularInvestment;
    double residualValue;
    double saleTax;
    double startDate;
    double terms;
    double terms2;
    double tradeAmount;
    int year;

    public double getPrincipalAmount2() {
        return this.principalAmount2;
    }

    public void setPrincipalAmount2(double d) {
        this.principalAmount2 = d;
    }

    public double getMonthlyPayment2() {
        return this.monthlyPayment2;
    }

    public void setMonthlyPayment2(double d) {
        this.monthlyPayment2 = d;
    }

    public double getTerms2() {
        return this.terms2;
    }

    public void setTerms2(double d) {
        this.terms2 = d;
    }

    public double getInterestRate2() {
        return this.interestRate2;
    }

    public void setInterestRate2(double d) {
        this.interestRate2 = d;
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(long j) {
        this.date = j;
    }

    public int getMonth() {
        return this.month;
    }

    public void setMonth(int i) {
        this.month = i;
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int i) {
        this.year = i;
    }

    public double getInterestAmount() {
        return this.interestAmount;
    }

    public void setInterestAmount(double d) {
        this.interestAmount = d;
    }

    public double getTaxIns() {
        return this.TaxIns;
    }

    public void setTaxIns(double d) {
        this.TaxIns = d;
    }

    public double getPMI() {
        return this.PMI;
    }

    public void setPMI(double d) {
        this.PMI = d;
    }

    public double getPrincipalAmount() {
        return this.principalAmount;
    }

    public void setPrincipalAmount(double d) {
        this.principalAmount = d;
    }

    public double getDownPayment() {
        return this.downPayment;
    }

    public void setDownPayment(double d) {
        this.downPayment = d;
    }

    public double getTerms() {
        return this.terms;
    }

    public void setTerms(double d) {
        this.terms = d;
    }

    public double getInterestRate() {
        return this.interestRate;
    }

    public void setInterestRate(double d) {
        this.interestRate = d;
    }

    public double getPropertyTax() {
        return this.propertyTax;
    }

    public void setPropertyTax(double d) {
        this.propertyTax = d;
    }

    public double getPropertyInsurance() {
        return this.propertyInsurance;
    }

    public void setPropertyInsurance(double d) {
        this.propertyInsurance = d;
    }

    public double getStartDate() {
        return this.startDate;
    }

    public void setStartDate(double d) {
        this.startDate = d;
    }

    public double getTradeAmount() {
        return this.tradeAmount;
    }

    public void setTradeAmount(double d) {
        this.tradeAmount = d;
    }

    public double getOwedTrade() {
        return this.owedTrade;
    }

    public void setOwedTrade(double d) {
        this.owedTrade = d;
    }

    public double getSaleTax() {
        return this.saleTax;
    }

    public void setSaleTax(double d) {
        this.saleTax = d;
    }

    public double getInterestPeriod() {
        return this.interestPeriod;
    }

    public void setInterestPeriod(double d) {
        this.interestPeriod = d;
    }

    public double getResidualValue() {
        return this.residualValue;
    }

    public void setResidualValue(double d) {
        this.residualValue = d;
    }

    public double getRegularInvestment() {
        return this.regularInvestment;
    }

    public void setRegularInvestment(double d) {
        this.regularInvestment = d;
    }

    public double getMonthlyPayment() {
        return this.monthlyPayment;
    }

    public void setMonthlyPayment(double d) {
        this.monthlyPayment = d;
    }
}
