package com.byronworkshop.utils;

import android.content.Context;

import com.byronworkshop.R;

import java.text.DecimalFormat;

public class DecimalFormatterUtils {

    public static String formatCurrency(Context context, int amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedValue = formatter.format(amount);

        return context.getString(R.string.dialog_edit_cost_sheet_cost, formattedValue);
    }
}
