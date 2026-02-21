package com.i2medier.financialpro.model;


public class GraphModel {
    int color;
    String label;
    double value;

    public GraphModel(String str, double d, int i) {
        this.label = str;
        this.value = d;
        this.color = i;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String str) {
        this.label = str;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double d) {
        this.value = d;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int i) {
        this.color = i;
    }
}
