package com.byronworkshop.utils;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    private TimePickerDialog.OnTimeSetListener mListener;
    private Calendar mDefaultTime;

    public static TimePickerFragment newInstance(Calendar defaultTime, TimePickerDialog.OnTimeSetListener listener) {
        TimePickerFragment fragment = new TimePickerFragment();
        fragment.setListener(listener);
        fragment.setDefaultTime(defaultTime);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int hour;
        int minute;

        if (this.mDefaultTime != null) {
            hour = this.mDefaultTime.get(Calendar.HOUR_OF_DAY);
            minute = this.mDefaultTime.get(Calendar.MINUTE);
        } else {
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }

        // Create a new instance of DatePickerDialog and return it
        return new TimePickerDialog(requireContext(), this.mListener, hour, minute, true);
    }

    private void setDefaultTime(Calendar defaultTime) {
        this.mDefaultTime = defaultTime;
    }

    private void setListener(TimePickerDialog.OnTimeSetListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        this.mListener.onTimeSet(view, hourOfDay, minute);
    }
}