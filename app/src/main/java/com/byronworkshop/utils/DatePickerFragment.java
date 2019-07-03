package com.byronworkshop.utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private DatePickerDialog.OnDateSetListener mListener;
    private Calendar mDefaultDate;

    public static DatePickerFragment newInstance(Calendar defaultDate, DatePickerDialog.OnDateSetListener listener) {
        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setListener(listener);
        fragment.setDefaultDate(defaultDate);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int year;
        int month;
        int day;

        if (this.mDefaultDate != null) {
            year = this.mDefaultDate.get(Calendar.YEAR);
            month = this.mDefaultDate.get(Calendar.MONTH);
            day = this.mDefaultDate.get(Calendar.DAY_OF_MONTH);
        } else {
            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(requireContext(), this.mListener, year, month, day);
    }

    private void setDefaultDate(Calendar defaultDate) {
        this.mDefaultDate = defaultDate;
    }

    private void setListener(DatePickerDialog.OnDateSetListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        this.mListener.onDateSet(view, year, month, day);
    }
}