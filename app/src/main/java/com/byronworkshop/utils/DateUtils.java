package com.byronworkshop.utils;

import android.content.Context;

import com.byronworkshop.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {

    public static String getFormattedTime(Date c) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return df.format(c);
    }

    public static String getFormattedDate(Date c) {
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.FULL);
        return formatter.format(c);
    }

    public static String getShortFormattedDate(Date c) {
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);
        return formatter.format(c);
    }

    public static String getFriendlyDateString(Context context, long dateInMillisecondsUTC) {
        Date start = new Date();
        Date end = new Date(dateInMillisecondsUTC);

        int months = (int) getDateDiff(start, end, Calendar.MONTH);
        if (months > 0) {
            return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_months, months, months);
        } else {
            int weeks = (int) getDateDiff(start, end, Calendar.WEEK_OF_YEAR);
            if (weeks > 0 && weeks < 4) {
                return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_weeks, weeks, weeks);
            } else {
                int days = (int) getDateDiff(start, end, Calendar.DATE);
                if (days > 1 && days < 7) {
                    return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_days, days, days);
                } else if (days == 1) {
                    return context.getString(R.string.content_reminders_readable_date_yesterday);
                } else {
                    return context.getString(R.string.content_reminders_readable_date_today);
                }
            }
        }
    }

    private static long getDateDiff(Date d1, Date d2, int calUnit) {
        if (d1.after(d2)) {    // make sure d1 < d2, else swap them
            Date temp = d1;
            d1 = d2;
            d2 = temp;
        }

        GregorianCalendar c1 = new GregorianCalendar();
        c1.setTime(d1);
        GregorianCalendar c2 = new GregorianCalendar();
        c2.setTime(d2);

        for (long i = 1; ; i++) {
            c1.add(calUnit, 1);   // add one day, week, year, etc.
            if (c1.after(c2))
                return i - 1;
        }
    }
}
