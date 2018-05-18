package com.byronworkshop.ui.reports.reminders;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.ui.detailsactivity.DetailsActivity;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.reports.reminders.adapter.MotorcycleReminderRVAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;

public class RemindersActivity extends AppCompatActivity implements MotorcycleReminderRVAdapter.ListItemClickListener {

    // firebase analytics events
    private static final String EVENT_REMINDERS_ACTIVITY_CREATION = "report_service_reminders";

    // keys
    public static final String KEY_UID = "uid";

    // vars
    private int maxDays;

    // args
    private String mUid;

    // resources
    private RecyclerView mMotorcycleRecyclerView;
    private FirestoreRecyclerAdapter mMotorcycleAdapter;
    private View emptyView;
    private ImageView ivImageHeader;

    // firebase
    private FirebaseAnalytics mFirebaseAnalytics;
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

        // fetch max days from preferences and store it in maxDays var
        this.fetchMaxDaysFromPreferences();

        // firebase
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        this.mMotorcyclesCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("motorcycles");

        // firebase log
        this.logRemindersActivityCreation();

        // resources
        this.mMotorcycleRecyclerView = findViewById(R.id.content_reminders_rv_motorcycles);
        this.mMotorcycleRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        this.mMotorcycleRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.emptyView = findViewById(R.id.content_reminders_rv_empty_view);
        this.ivImageHeader = findViewById(R.id.activity_reminders_avatar);

        // set image parallax header
        ColorDrawable imagePlaceholder = new ColorDrawable(ContextCompat.getColor(this, R.color.colorPlaceholder));
        RequestOptions options = RequestOptions.placeholderOf(imagePlaceholder);

        Glide.with(this)
                .load(R.drawable.header_reminders_bg)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivImageHeader);
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
        this.attachMotorcycleRVAdapter();
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

    private void fetchMaxDaysFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.maxDays = Integer.parseInt(preferences.getString(getString(R.string.pref_max_elapsed_time_last_service_key),
                getString(R.string.pref_max_elapsed_time_last_service_default_value)));
    }

    private void attachMotorcycleRVAdapter() {
        // setting fields
        FieldPath reminderEnabledPath = FieldPath.of("metadata", "reminderEnabled");
        FieldPath lastWorkOrderEndDatePath = FieldPath.of("metadata", "lastWorkOrderEndDate");

        Calendar nowMinusDays = Calendar.getInstance();
        nowMinusDays.add(Calendar.DATE, maxDays * -1);

        // generating recyclers options
        FirestoreRecyclerOptions<Motorcycle> options = new FirestoreRecyclerOptions.Builder<Motorcycle>()
                .setQuery(
                        this.mMotorcyclesCollReference
                                .whereEqualTo(reminderEnabledPath, true)
                                .whereLessThanOrEqualTo(lastWorkOrderEndDatePath, nowMinusDays.getTime())
                                .orderBy(lastWorkOrderEndDatePath, Query.Direction.DESCENDING)
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
