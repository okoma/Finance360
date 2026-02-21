package com.i2medier.financialpro.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


public class AppPref {
    static final String IS_RATEUS = "IS_RATEUS";
    static final String IS_TERMS_ACCEPT = "IS_TERMS_ACCEPT";
    static final String MyPref = "userPref";

    public static boolean IsTermsAccept(Context context) {
        return context.getApplicationContext().getSharedPreferences(MyPref, 0).getBoolean(IS_TERMS_ACCEPT, false);
    }

    @SuppressLint({"ApplySharedPref"})
    public static void setIsTermsAccept(Context context, boolean z) {
        SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(MyPref, 0).edit();
        edit.putBoolean(IS_TERMS_ACCEPT, z);
        edit.commit();
    }

    public static boolean IsRateUS(Context context) {
        return context.getApplicationContext().getSharedPreferences(MyPref, 0).getBoolean(IS_RATEUS, false);
    }

    public static void setIsRateUS(Context context, boolean z) {
        SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(MyPref, 0).edit();
        edit.putBoolean(IS_RATEUS, z);
        edit.commit();
    }
}
