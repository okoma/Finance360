package com.i2medier.financialpro.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MonthModel implements Parcelable {
    public static final Creator<MonthModel> CREATOR = new Creator<MonthModel>() {

        @Override
        public MonthModel createFromParcel(Parcel parcel) {
            return new MonthModel(parcel);
        }


        @Override
        public MonthModel[] newArray(int i) {
            return new MonthModel[i];
        }
    };
    public double Balance;
    public double Interest;
    public double PrincipalAmount;
    public double TaxInsPMI;
    public double TotalInterest;
    public double TotalPaid;
    public double TotalPrincipal;
    public double TotalTax;
    public Date date;
    public int month;
    public int year;

    @Override
    public int describeContents() {
        return 0;
    }

    public MonthModel() {
        this.date = new Date();
    }

    public MonthModel(double d, double d2, double d3, double d4, double d5, double d6, double d7, double d8, int i, int i2, Date date) {
        this.date = new Date();
        this.PrincipalAmount = d;
        this.Interest = d2;
        this.Balance = d3;
        this.TotalPaid = d4;
        this.TaxInsPMI = d5;
        this.TotalInterest = d6;
        this.TotalTax = d7;
        this.TotalPrincipal = d8;
        this.month = i;
        this.year = i2;
        this.date = date;
    }

    protected MonthModel(Parcel parcel) {
        this.date = new Date();
        this.PrincipalAmount = parcel.readDouble();
        this.Interest = parcel.readDouble();
        this.Balance = parcel.readDouble();
        this.TotalPaid = parcel.readDouble();
        this.TaxInsPMI = parcel.readDouble();
        this.TotalInterest = parcel.readDouble();
        this.TotalTax = parcel.readDouble();
        this.TotalPrincipal = parcel.readDouble();
        this.month = parcel.readInt();
        this.year = parcel.readInt();
    }

    public double getPrincipalAmount() {
        return this.PrincipalAmount;
    }

    public void setPrincipalAmount(double d) {
        this.PrincipalAmount = d;
    }

    public double getInterest() {
        return this.Interest;
    }

    public void setInterest(double d) {
        this.Interest = d;
    }

    public double getBalance() {
        return this.Balance;
    }

    public void setBalance(double d) {
        this.Balance = d;
    }

    public double getTotalPaid() {
        return this.TotalPaid;
    }

    public void setTotalPaid(double d) {
        this.TotalPaid = d;
    }

    public double getTaxInsPMI() {
        return this.TaxInsPMI;
    }

    public void setTaxInsPMI(double d) {
        this.TaxInsPMI = d;
    }

    public double getTotalInterest() {
        return this.TotalInterest;
    }

    public void setTotalInterest(double d) {
        this.TotalInterest = d;
    }

    public double getTotalTax() {
        return this.TotalTax;
    }

    public void setTotalTax(double d) {
        this.TotalTax = d;
    }

    public double getTotalPrincipal() {
        return this.TotalPrincipal;
    }

    public void setTotalPrincipal(double d) {
        this.TotalPrincipal = d;
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

    public String getDate() {
        return new SimpleDateFormat("MMM-yyyy").format(this.date);
    }

    public Date getDateofGraph() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(this.PrincipalAmount);
        parcel.writeDouble(this.Interest);
        parcel.writeDouble(this.Balance);
        parcel.writeDouble(this.TotalPaid);
        parcel.writeDouble(this.TaxInsPMI);
        parcel.writeDouble(this.TotalInterest);
        parcel.writeDouble(this.TotalTax);
        parcel.writeDouble(this.TotalPrincipal);
        parcel.writeInt(this.month);
        parcel.writeInt(this.year);
    }
}
