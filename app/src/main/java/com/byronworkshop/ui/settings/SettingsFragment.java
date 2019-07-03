package com.byronworkshop.ui.settings;


import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.byronworkshop.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    final private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Context context = preference.getContext();
            String stringValue = value.toString();

            if (preference instanceof EditTextPreference) {
                if (preference.getKey().equals(preference.getContext().getString(R.string.pref_max_elapsed_time_last_service_key))) {
                    Toast error = Toast.makeText(context, context.getString(R.string.pref_max_elapsed_time_last_service_error), Toast.LENGTH_SHORT);

                    try {
                        int floatValue = Integer.parseInt(stringValue);
                        if (floatValue > 1) {
                            preference.setSummary(context.getString(R.string.pref_max_elapsed_time_last_service_default_value_summary, floatValue));
                            return true;
                        }

                        error.show();
                        return false;
                    } catch (NumberFormatException e) {
                        error.show();
                        return false;
                    }
                } else {
                    preference.setSummary(stringValue);
                }
            }

            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        Context context = preference.getContext();
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(true);

        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_max_elapsed_time_last_service_key)));
    }
}
