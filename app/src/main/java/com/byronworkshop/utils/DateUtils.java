package com.byronworkshop.utils;

import android.content.Context;

import com.byronworkshop.R;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Weeks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static String getFormattedTime(Calendar c) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return df.format(c.getTime());
    }

    public static String getFormattedDate(Calendar c) {
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.FULL);
        return formatter.format(c.getTime());
    }

    public static String getShortFormattedDate(Calendar c) {
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG);
        return formatter.format(c.getTime());
    }

    public static String getFriendlyDateString(Context context, long dateInMillisecondsUTC) {
        DateTime last = new DateTime(dateInMillisecondsUTC);
        DateTime now = new DateTime();

        Days days = Days.daysBetween(last, now);
        Weeks weeks = Weeks.weeksBetween(last, now);
        Months months = Months.monthsBetween(last, now);

        if (days.getDays() == 0) {
            return context.getString(R.string.content_reminders_readable_date_today);
        } else if (days.getDays() < 7) {
            return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_days, days.getDays(), days.getDays());
        } else if (weeks.getWeeks() < 4) {
            return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_weeks, weeks.getWeeks(), weeks.getWeeks());
        } else {
            return context.getResources().getQuantityString(R.plurals.content_reminders_readable_date_months, months.getMonths(), months.getMonths());
        }
    }
}
