package com.i2medier.financialpro.util;

import android.content.Context;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import com.i2medier.financialpro.model.MonthModel;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class Utils {
    public static double Interest = 0.0d;
    public static double Paid = 0.0d;
    public static double Principal = 0.0d;
    public static final int REQUEST = 112;
    public static boolean isMonthly = true;
    public static boolean isYearly = false;
    public static double mTaxInsPMI;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    public static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public static double getInterestOnly(double d, double d2) {
        return ((d / 12.0d) / 100.0d) * d2;
    }

    public static double getTotalInterest(double d, double d2, double d3) {
        return (d * d2) - d3;
    }

    public static boolean hasPermissions(Context context, String... strArr) {
        if (Build.VERSION.SDK_INT < 23 || context == null || strArr == null) {
            return true;
        }
        for (String str : strArr) {
            if (ActivityCompat.checkSelfPermission(context, str) != 0) {
                return false;
            }
        }
        return true;
    }

    public static Date CALDATE(String str) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        try {
            date = simpleDateFormat.parse(str);
            System.out.println(simpleDateFormat.format(date));
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    public static double getMonthlyPayment(double d, double d2, double d3) {
        double d4 = (d2 / 100.0d) / 12.0d;
        double pow = Math.pow(d4 + 1.0d, d3);
        return ((d * d4) * pow) / (pow - 1.0d);
    }

    public static ArrayList<MonthModel> getYearlyAmount(ArrayList<MonthModel> arrayList, Date date, Date date2) {
        int i;
        ArrayList<MonthModel> arrayList2 = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        int parseInt = Integer.parseInt(simpleDateFormat.format(date));
        int parseInt2 = Integer.parseInt(simpleDateFormat.format(date2));
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM");
        int parseInt3 = Integer.parseInt(simpleDateFormat2.format(date));
        int parseInt4 = Integer.parseInt(simpleDateFormat2.format(date2));
        if (arrayList != null && arrayList.size() > 0) {
            int i2 = parseInt;
            int i3 = 0;
            double d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            double d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            while (i2 <= parseInt2 && i3 < arrayList.size()) {
                if (parseInt == parseInt2) {
                    i = arrayList.size();
                    i3 = 0;
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1;
                    i3 = 0;
                } else {
                    i = i2 == parseInt2 ? (i3 + parseInt4) - 1 : i3 + 12;
                }
                int i4 = parseInt3;
                int i5 = parseInt4;
                double d3 = d;
                double d4 = d2;
                double d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                double d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                while (i3 < i) {
                    d5 += arrayList.get(i3).getPrincipalAmount();
                    d6 += arrayList.get(i3).getInterest();
                    d3 += d6;
                    d4 += d5;
                    i3++;
                }
                MonthModel monthModel = new MonthModel();
                monthModel.setDate(new Date());
                monthModel.setPrincipalAmount(d5);
                monthModel.setInterest(d6);
                monthModel.setTotalPaid(d5 + d6);
                monthModel.setTotalPrincipal(d4);
                monthModel.setTotalInterest(d3);
                monthModel.setYear(i2);
                monthModel.setBalance(arrayList.get(i - 1).getBalance());
                arrayList2.add(monthModel);
                i2++;
                d = d3;
                d2 = d4;
                parseInt3 = i4;
                i3 = i;
                parseInt2 = parseInt2;
                parseInt4 = i5;
            }
        }
        return arrayList2;
    }

    public static ArrayList<MonthModel> getMonthlyAmount(double d, double d2, double d3, double d4, Date date) {
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        double d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
        double d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
        double d7 = d;
        for (int i = 0; i < d2; i++) {
            MonthModel monthModel = new MonthModel();
            double d8 = ((d3 / 12.0d) * d7) / 100.0d;
            double d9 = d4 - d8;
            d7 -= d9;
            double d10 = d9 + d8;
            d5 += d8;
            d6 += d9;
            if (d7 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d7 = 0.0d;
            }
            new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(2, i);
            monthModel.setPrincipalAmount(d9);
            monthModel.setInterest(d8);
            monthModel.setBalance(d7);
            monthModel.setTotalPaid(d10);
            monthModel.setTotalPrincipal(d6);
            monthModel.setTotalInterest(d5);
            monthModel.setDate(calendar.getTime());
            arrayList = arrayList;
            arrayList.add(monthModel);
            Principal += d9;
            Interest += d8;
            Paid += d10;
        }
        return arrayList;
    }

    public static ArrayList<MonthModel> getYearlyMortgage(ArrayList<MonthModel> arrayList, Date date, Date date2) {
        int i;
        ArrayList<MonthModel> arrayList2 = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        int parseInt = Integer.parseInt(simpleDateFormat.format(date));
        int parseInt2 = Integer.parseInt(simpleDateFormat.format(date2));
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM");
        int parseInt3 = Integer.parseInt(simpleDateFormat2.format(date));
        int parseInt4 = Integer.parseInt(simpleDateFormat2.format(date2));
        if (arrayList != null && arrayList.size() > 0) {
            int i2 = parseInt;
            int i3 = 0;
            double d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            double d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            while (i2 <= parseInt2 && i3 < arrayList.size()) {
                if (parseInt == parseInt2) {
                    i = arrayList.size();
                    i3 = 0;
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1;
                    i3 = 0;
                } else {
                    i = i2 == parseInt2 ? (i3 + parseInt4) - 1 : i3 + 12;
                }
                int i4 = parseInt3;
                int i5 = parseInt4;
                int i6 = parseInt2;
                int i7 = parseInt;
                double d3 = d;
                double d4 = d2;
                double d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                double d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                double d7 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                while (i3 < i) {
                    d6 += arrayList.get(i3).getPrincipalAmount();
                    d7 += arrayList.get(i3).getInterest();
                    d5 += arrayList.get(i3).getTaxInsPMI();
                    d3 += d7;
                    d4 += d6;
                    i3++;
                }
                MonthModel monthModel = new MonthModel();
                monthModel.setDate(new Date());
                monthModel.setPrincipalAmount(d6);
                monthModel.setInterest(d7);
                monthModel.setTaxInsPMI(d5);
                monthModel.setTotalTax(d5);
                monthModel.setTotalInterest(d3);
                monthModel.setTotalPrincipal(d4);
                monthModel.setTotalPaid(d6 + d7 + d5);
                monthModel.setYear(i2);
                monthModel.setBalance(arrayList.get(i - 1).getBalance());
                arrayList2.add(monthModel);
                i2++;
                d = d3;
                d2 = d4;
                i3 = i;
                parseInt = i7;
                parseInt2 = i6;
                parseInt3 = i4;
                parseInt4 = i5;
            }
        }
        return arrayList2;
    }

    public static ArrayList<MonthModel> getMonthlyMortgage(double d, double d2, double d3, double d4, double d5, double d6, Date date, double d7) {
        double d8;
        double d9;
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        double d10 = d - d7;
        int i = 0;
        double d11 = d6;
        double d12 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
        double d13 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
        double d14 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
        while (i < d2) {
            MonthModel monthModel = new MonthModel();
            if ((80.0d * d) / 100.0d > d10) {
                d11 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            }
            double d15 = ((d3 / 12.0d) * d10) / 100.0d;
            double d16 = d4 - d15;
            double d17 = d10 - d16;
            ArrayList<MonthModel> arrayList2 = arrayList;
            double d18 = d16 + d15 + d5 + d11;
            double d19 = d5 + d11;
            double d20 = d14 + d19;
            d12 += d15;
            d13 += d16;
            if (d17 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d8 = d11;
                d9 = 0.0d;
            } else {
                d8 = d11;
                d9 = d17;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date.getTime());
            calendar.add(2, i);
            monthModel.setPrincipalAmount(d16);
            monthModel.setInterest(d15);
            monthModel.setBalance(d9);
            double d21 = d9;
            monthModel.setTotalPaid(d18);
            monthModel.setTotalPrincipal(d13);
            monthModel.setTaxInsPMI(d19);
            monthModel.setTotalInterest(d12);
            monthModel.setTotalTax(d20);
            monthModel.setDate(calendar.getTime());
            arrayList2.add(monthModel);
            Principal += d16;
            Interest += d15;
            mTaxInsPMI += d5;
            Paid += d18;
            i++;
            d11 = d8;
            d14 = d20;
            arrayList = arrayList2;
            d10 = d21;
        }
        return arrayList;
    }

    public static ArrayList<MonthModel> getYearlyCompound(ArrayList<MonthModel> arrayList, Date date, Date date2) {
        int i;
        ArrayList<MonthModel> arrayList2 = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        int parseInt = Integer.parseInt(simpleDateFormat.format(date));
        int parseInt2 = Integer.parseInt(simpleDateFormat.format(date2));
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM");
        int parseInt3 = Integer.parseInt(simpleDateFormat2.format(date));
        int parseInt4 = Integer.parseInt(simpleDateFormat2.format(date2));
        if (arrayList != null && arrayList.size() > 0) {
            int i2 = parseInt;
            int i3 = 0;
            double d = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            double d2 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
            while (i2 <= parseInt2 && i3 < arrayList.size()) {
                if (parseInt == parseInt2) {
                    i = arrayList.size();
                    i3 = 1;
                } else if (i2 == parseInt) {
                    i = (12 - parseInt3) + 1;
                    i3 = 0;
                } else {
                    i = i2 == parseInt2 ? (i3 + parseInt4) - 1 : i3 + 12;
                }
                int i4 = parseInt3;
                int i5 = parseInt4;
                double d3 = d;
                double d4 = d2;
                double d5 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                double d6 = com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                while (i3 < i) {
                    d5 += arrayList.get(i3).getPrincipalAmount();
                    d6 += arrayList.get(i3).getInterest();
                    d3 += d6;
                    d4 += d5;
                    i3++;
                }
                MonthModel monthModel = new MonthModel();
                monthModel.setDate(new Date());
                monthModel.setPrincipalAmount(d5);
                monthModel.setInterest(d6);
                monthModel.setTotalPrincipal(d4);
                monthModel.setTotalPaid(d5 + d6);
                monthModel.setTotalInterest(d3);
                monthModel.setYear(i2);
                monthModel.setBalance(arrayList.get(i - 1).getBalance());
                arrayList2.add(monthModel);
                i2++;
                d = d3;
                d2 = d4;
                parseInt3 = i4;
                i3 = i;
                parseInt2 = parseInt2;
                parseInt4 = i5;
            }
        }
        return arrayList2;
    }

    public static ArrayList<MonthModel> getMonthlyCompound(double d, double d2, double d3, double d4, Date date) {
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        int i = 0;
        double d5 = 0.0d;
        double d6 = 0.0d;
        double d7 = d;
        while (i < d2) {
            MonthModel monthModel = new MonthModel();
            double d8 = i == 0 ? d4 + d : d4;
            double d9 = ((d3 / 12.0d) / 100.0d) * (d7 + d4);
            d7 = i == 0 ? d9 + d + d4 : d9 + d8 + d7;
            d5 += d9;
            d6 += d8;
            new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(2, i);
            monthModel.setPrincipalAmount(d8);
            monthModel.setInterest(d9);
            monthModel.setTotalInterest(d5);
            monthModel.setTotalPrincipal(d6);
            monthModel.setBalance(d7);
            monthModel.setDate(calendar.getTime());
            arrayList.add(monthModel);
            Principal += d8;
            Interest += d9;
            i++;
        }
        Paid = d7;
        return arrayList;
    }

    public static ArrayList<MonthModel> getMonthlyInterest(double d, double d2, double d3, double d4, double d5, Date date) {
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        int i = 0;
        double d6 = d;
        double d7 = 0.0d;
        while (true) {
            double d8 = i;
            if (d8 >= d2) {
                return arrayList;
            }
            MonthModel monthModel = new MonthModel();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date.getTime());
            calendar.add(2, i);
            calendar.getTime();
            if (d8 < d5) {
                double d9 = ((d3 / 12.0d) * d6) / 100.0d;
                double d10 = d9 + com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                d7 += com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                if (d6 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    d6 = 0.0d;
                }
                monthModel.setPrincipalAmount(com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON);
                monthModel.setInterest(d9);
                monthModel.setBalance(d6);
                monthModel.setTotalPaid(d10);
                monthModel.setTotalPrincipal(d7);
                monthModel.setDate(calendar.getTime());
                arrayList.add(monthModel);
                Principal += com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON;
                Interest += d9;
                Paid += d10;
            } else {
                double d11 = ((d3 / 12.0d) * d6) / 100.0d;
                double d12 = d4 - d11;
                d6 -= d12;
                double d13 = d12 + d11;
                d7 += d12;
                if (d6 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                    d6 = 0.0d;
                }
                monthModel.setPrincipalAmount(d12);
                monthModel.setInterest(d11);
                monthModel.setBalance(d6);
                monthModel.setTotalPrincipal(d7);
                monthModel.setTotalPaid(d13);
                monthModel.setDate(calendar.getTime());
                arrayList.add(monthModel);
                Principal += d12;
                Interest += d11;
                Paid += d13;
            }
            i++;
        }
    }

    public static ArrayList<MonthModel> getYearlyRefinanceAmount(double d, double d2, double d3, double d4, int i) {
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        double d5 = d;
        int i2 = i;
        for (int i3 = 0; i3 < d2; i3++) {
            MonthModel monthModel = new MonthModel();
            double d6 = ((d3 / 12.0d) * d5) / 100.0d;
            double d7 = d4 - d6;
            d5 -= d7;
            double d8 = d7 + d6;
            if (d5 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d5 = 0.0d;
            }
            monthModel.setPrincipalAmount(d7);
            monthModel.setInterest(d6);
            monthModel.setBalance(d5);
            monthModel.setTotalPaid(d8);
            monthModel.setYear(i2);
            arrayList.add(monthModel);
            i2++;
            Principal += d7;
            Interest += d6;
            Paid += d8;
        }
        return arrayList;
    }

    public static ArrayList<MonthModel> getYearlySimpleInterest(double d, double d2, double d3, int i) {
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        double d4 = 0.0d;
        double d5 = d;
        for (int i2 = 0; i2 < d2; i2++) {
            MonthModel monthModel = new MonthModel();
            d5 += d3;
            d4 += d3;
            monthModel.setPrincipalAmount(d);
            monthModel.setInterest(d3);
            monthModel.setTotalInterest(d4);
            monthModel.setTotalPrincipal(d);
            monthModel.setBalance(d5);
            monthModel.setYear(i);
            arrayList.add(monthModel);
            i++;
        }
        return arrayList;
    }

    public static ArrayList<MonthModel> getYearlyLoanCompare(double d, double d2, double d3, double d4) {
        double d5;
        ArrayList<MonthModel> arrayList = new ArrayList<>();
        double d6 = 12.0d;
        int i = (int) (d2 * 12.0d);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int i2 = calendar.get(1);
        int i3 = 0;
        double d7 = d;
        double d8 = 0.0d;
        double d9 = 0.0d;
        double d10 = 0.0d;
        while (i3 < i) {
            double d11 = ((d3 / d6) * d7) / 100.0d;
            double d12 = d4 - d11;
            d7 -= d12;
            double d13 = d12 + d11;
            if (d7 < com.github.mikephil.charting.utils.Utils.DOUBLE_EPSILON) {
                d7 = 0.0d;
            }
            if (i2 != calendar.get(1)) {
                MonthModel monthModel = new MonthModel();
                monthModel.setPrincipalAmount(d8);
                monthModel.setInterest(d9);
                monthModel.setBalance(d7);
                monthModel.setTotalPaid(d10);
                monthModel.setYear(i2);
                arrayList.add(monthModel);
                i2 = calendar.get(1);
                d5 = 0.0d;
                d8 = 0.0d;
                d9 = 0.0d;
            } else {
                d5 = d10;
            }
            d8 += d12;
            d9 += d11;
            d10 = d5 + d13;
            Principal += d12;
            Interest += d11;
            Paid += d13;
            calendar.add(2, 1);
            i3++;
            d6 = 12.0d;
        }
        MonthModel monthModel2 = new MonthModel();
        monthModel2.setPrincipalAmount(d8);
        monthModel2.setInterest(d9);
        monthModel2.setBalance(d7);
        monthModel2.setTotalPaid(d10);
        monthModel2.setYear(calendar.get(1));
        arrayList.add(monthModel2);
        return arrayList;
    }
}
