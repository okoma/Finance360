package com.i2medier.financialpro.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import androidx.cardview.widget.CardView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AppConstant {
    public static String DISCLOSURE_DIALOG_DESC = "We would like to inform you regarding the 'Consent to Collection and Use Of Data'\n\nTo share result of calculation image, allow storage permission.\n\nWe store your data on your device only, we don’t store them on our server.";

    public static String PRIVACY_POLICY_URL = "https://www.google.com";
    public static String TERMS_OF_SERVICE_URL = "https://www.google.com";

    public static String showDatePatternDDMMYYYY = "dd-MM-yyyy";
    public static final DateFormat Date_FoRMAT_DDMMYY = new SimpleDateFormat(showDatePatternDDMMYYYY);

    @SuppressLint("WrongConstant")
    public static void openUrl(Context context, String str) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(str));
        intent.addFlags(1208483840);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(PRIVACY_POLICY_URL)));
        }
    }

    public static String getFormattedDate(long j, DateFormat dateFormat) {
        return dateFormat.format(new Date(j));
    }

    public static void hideKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            View currentFocus = activity.getCurrentFocus();
            if (currentFocus == null) {
                currentFocus = new View(activity);
            }
            inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void visibleResult(CardView cardView) {
        cardView.setVisibility(View.VISIBLE);
    }

    public static void visibleGraph(LinearLayout linearLayout) {
        linearLayout.setVisibility(View.VISIBLE);
    }
}
