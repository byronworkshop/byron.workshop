package com.byronworkshop.ui.reports.reminders;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.byronworkshop.BuildConfig;
import com.byronworkshop.R;
import com.byronworkshop.ui.detailsactivity.DetailsActivity;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.reports.reminders.adapter.MotorcycleReminderRVAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RemindersActivity extends AppCompatActivity implements MotorcycleReminderRVAdapter.ListItemClickListener {

    // firebase analytics events
    private static final String EVENT_REMINDERS_ACTIVITY_CREATION = "report_service_reminders";

    // keys
    public static final String KEY_UID = "uid";

    // firebase remote config
    public static final int DEFAULT_MAX_WO_ELAPSED_DAYS = 30;
    public static final String MAX_LAST_WO_ELAPSED_DAYS_KEY = "MAX_LAST_WO_ELAPSED_DAYS_KEY";
    private boolean isFetching;
    private int maxDays = DEFAULT_MAX_WO_ELAPSED_DAYS;

    // args
    private String mUid;

    // resources
    private RecyclerView mMotorcycleRecyclerView;
    private FirestoreRecyclerAdapter mMotorcycleAdapter;
    private View emptyView;

    // firebase
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private CollectionReference mMotorcyclesCollReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        // clear all notifications
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        // toolbar
        Toolbar toolbar = findViewById(R.id.activity_reminders_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // retrieve arguments
        if (this.getIntent().getExtras() != null
                && this.getIntent().getExtras().containsKey(KEY_UID)) {
            this.mUid = this.getIntent().getExtras().getString(KEY_UID);
        } else {
            throw new IllegalArgumentException(getString(R.string.activity_reminders_creation_error));
        }

        // firebase
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        this.mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        this.mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(MAX_LAST_WO_ELAPSED_DAYS_KEY, DEFAULT_MAX_WO_ELAPSED_DAYS);
        this.mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        this.fetchConfig();

        // firebase log
        this.logRemindersActivityCreation();

        // resources
        this.mMotorcycleRecyclerView = findViewById(R.id.content_reminders_rv_motorcycles);
        this.mMotorcycleRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        this.mMotorcycleRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.emptyView = findViewById(R.id.content_reminders_rv_empty_view);

        this.mMotorcyclesCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("motorcycles");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // attach firebase
        if (!this.isFetching) {
            this.attachMotorcycleRVAdapter();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // detach firebase
        this.detachMotorcycleRVAdapter();
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void logRemindersActivityCreation() {
        this.mFirebaseAnalytics.logEvent(EVENT_REMINDERS_ACTIVITY_CREATION, null);
    }

    private void fetchConfig() {
        long cacheExpiration = 43200; // 12 hours in seconds

        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (this.mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        this.isFetching = true;
        this.mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mFirebaseRemoteConfig.activateFetched();
                        maxDays = (int) mFirebaseRemoteConfig.getLong(MAX_LAST_WO_ELAPSED_DAYS_KEY);

                        attachMotorcycleRVAdapter();

                        isFetching = false;
                    }
                });
    }

    private void attachMotorcycleRVAdapter() {
        // setting fields
        FieldPath reminderEnabledPath = FieldPath.of("metadata", "reminderEnabled");
        FieldPath lastWorkOrderDatePath = FieldPath.of("metadata", "lastWorkOrderDate");

        Calendar nowMinusDays = Calendar.getInstance();
        nowMinusDays.add(Calendar.DATE, maxDays * -1);

        // generating recyclers options
        FirestoreRecyclerOptions<Motorcycle> options = new FirestoreRecyclerOptions.Builder<Motorcycle>()
                .setQuery(
                        this.mMotorcyclesCollReference
                                .whereEqualTo(reminderEnabledPath, true)
                                .whereGreaterThan(lastWorkOrderDatePath, -1)
                                .whereLessThanOrEqualTo(lastWorkOrderDatePath, nowMinusDays.getTimeInMillis())
                                .orderBy(lastWorkOrderDatePath, Query.Direction.DESCENDING)
                        , Motorcycle.class)
                .build();

        // manually stop previous existent adapter
        if (this.mMotorcycleAdapter != null) {
            this.mMotorcycleAdapter.stopListening();
            this.mMotorcycleAdapter = null;
        }

        // create new adapter and start listening
        this.mMotorcycleAdapter = new MotorcycleReminderRVAdapter(options, this, this.emptyView);
        this.mMotorcycleAdapter.startListening();
        this.mMotorcycleRecyclerView.setAdapter(this.mMotorcycleAdapter);
    }

    private void detachMotorcycleRVAdapter() {
        // to clean the list and stop the adapter
        if (this.mMotorcycleAdapter != null) {
            this.mMotorcycleAdapter.stopListening();
            this.mMotorcycleAdapter = null;
            this.mMotorcycleRecyclerView.setAdapter(null);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // RV Adapter listener
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onListItemClick(String motorcycleId, Motorcycle motorcycle) {
        Bundle bundle = new Bundle();
        bundle.putString(DetailsActivity.KEY_MOTORCYCLE_ID, motorcycleId);
        bundle.putString(DetailsActivity.KEY_UID, this.mUid);

        Intent detailsIntent = new Intent(this, DetailsActivity.class);
        detailsIntent.putExtras(bundle);

        startActivity(detailsIntent);
    }
}
