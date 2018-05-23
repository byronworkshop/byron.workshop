package com.byronworkshop.ui.reports.income;

import android.app.DatePickerDialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderMetadata;
import com.byronworkshop.utils.DatePickerFragment;
import com.byronworkshop.utils.DateUtils;
import com.byronworkshop.utils.DecimalFormatterUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;

public class IncomeActivity extends AppCompatActivity {

    // firebase analytics events
    private static final String EVENT_START_INCOME_ACTIVITY = "report_labor_income";

    // keys
    public static final String KEY_UID = "uid";
    private static final String KEY_START_DATE = "start_date";
    private static final String KEY_END_DATE = "end_date";

    // args
    private String mUid;

    // resources
    private AppCompatButton btnStart;
    private AppCompatButton btnEnd;
    private TextView tvStart;
    private TextView tvEnd;
    private TextView tvTotalLaborIncome;
    private ImageView ivImageHeader;

    // firebase
    private FirebaseAnalytics mFirebaseAnalytics;
    private ListenerRegistration mTotalLaborListener;

    // dates
    private Calendar startDate;
    private Calendar endDate;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_START_DATE, this.startDate);
        outState.putSerializable(KEY_END_DATE, this.endDate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income);

        // get intent extras
        if (this.getIntent().getExtras() != null
                && this.getIntent().getExtras().containsKey(KEY_UID)) {
            this.mUid = this.getIntent().getExtras().getString(KEY_UID);
        }

        // saved states
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_START_DATE) && savedInstanceState.containsKey(KEY_END_DATE)) {
            this.startDate = (Calendar) savedInstanceState.getSerializable(KEY_START_DATE);
            this.endDate = (Calendar) savedInstanceState.getSerializable(KEY_END_DATE);
        }

        // layout
        Toolbar toolbar = findViewById(R.id.activity_income_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // firebase
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // resources
        this.btnStart = findViewById(R.id.content_income_btn_start);
        this.btnEnd = findViewById(R.id.content_income_btn_end);
        this.tvStart = findViewById(R.id.content_income_tv_start);
        this.tvEnd = findViewById(R.id.content_income_tv_end);
        this.tvTotalLaborIncome = findViewById(R.id.content_income_total_labor_inc);

        this.ivImageHeader = findViewById(R.id.activity_income_avatar);

        // set image parallax header
        ColorDrawable imagePlaceholder = new ColorDrawable(ContextCompat.getColor(this, R.color.colorPlaceholder));
        RequestOptions options = RequestOptions.placeholderOf(imagePlaceholder);

        Glide.with(this)
                .load(R.drawable.header_income_bg)
                .apply(options)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivImageHeader);

        // settings current month on new activity
        if (savedInstanceState == null) {
            this.setCurrentMonth();
        } else {
            this.showDates();
        }

        // listeners
        this.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickStartDate();
            }
        });
        this.btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickEndDate();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // attach firebase
        this.logStartIncomeActivity();
        this.attachTotalLaborIncomeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // detach firebase
        this.detachTotalLaborIncomeListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_income, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_income_reset_month:
                this.setCurrentMonth();
                this.attachTotalLaborIncomeListener();
                return true;
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

    private void pickStartDate() {
        final boolean mTabletMode = getResources().getBoolean(R.bool.tablet_mode);

        DatePickerFragment newFragment = DatePickerFragment.newInstance(startDate, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                startDate = Calendar.getInstance();
                startDate.set(year, month, day);
                startDate.set(Calendar.HOUR_OF_DAY, 0);
                startDate.set(Calendar.MINUTE, 0);
                startDate.set(Calendar.SECOND, 0);
                startDate.set(Calendar.MILLISECOND, 0);

                if (!mTabletMode) {
                    tvStart.setText(DateUtils.getShortFormattedDate(startDate.getTime()));
                } else {
                    tvStart.setText(DateUtils.getFormattedDate(startDate.getTime()));
                }
                attachTotalLaborIncomeListener();
            }
        });

        newFragment.show(this.getSupportFragmentManager(), "datePicker");
    }

    private void pickEndDate() {
        final boolean mTabletMode = getResources().getBoolean(R.bool.tablet_mode);

        DatePickerFragment newFragment = DatePickerFragment.newInstance(endDate, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                endDate = Calendar.getInstance();
                endDate.set(year, month, day);
                endDate.set(Calendar.HOUR_OF_DAY, 23);
                endDate.set(Calendar.MINUTE, 59);
                endDate.set(Calendar.SECOND, 59);
                endDate.set(Calendar.MILLISECOND, 0);

                if (!mTabletMode) {
                    tvEnd.setText(DateUtils.getShortFormattedDate(endDate.getTime()));
                } else {
                    tvEnd.setText(DateUtils.getFormattedDate(endDate.getTime()));
                }
                attachTotalLaborIncomeListener();
            }
        });

        newFragment.show(this.getSupportFragmentManager(), "datePicker");
    }

    private void setCurrentMonth() {
        this.startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        this.endDate = Calendar.getInstance();
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 0);

        this.showDates();
    }

    private void showDates() {
        boolean mTabletMode = getResources().getBoolean(R.bool.tablet_mode);

        String formattedStart;
        String formattedEnd;
        if (!mTabletMode) {
            formattedStart = DateUtils.getShortFormattedDate(this.startDate.getTime());
            formattedEnd = DateUtils.getShortFormattedDate(this.endDate.getTime());
        } else {
            formattedStart = DateUtils.getFormattedDate(this.startDate.getTime());
            formattedEnd = DateUtils.getFormattedDate(this.endDate.getTime());
        }

        this.tvStart.setText(formattedStart);
        this.tvEnd.setText(formattedEnd);
    }

    private void logStartIncomeActivity() {
        this.mFirebaseAnalytics.logEvent(EVENT_START_INCOME_ACTIVITY, null);
    }

    private void attachTotalLaborIncomeListener() {
        if (this.startDate == null || this.endDate == null || this.startDate.compareTo(this.endDate) > 0) {
            this.tvTotalLaborIncome.setText(DecimalFormatterUtils.formatCurrency(IncomeActivity.this, 0));
            return;
        }

        this.mTotalLaborListener = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders$metadata")
                .whereGreaterThanOrEqualTo("endDate", startDate.getTime())
                .whereLessThanOrEqualTo("endDate", endDate.getTime())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null || queryDocumentSnapshots == null) {
                            return;
                        }

                        int totalLaborCost = 0;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.exists()) {
                                continue;
                            }

                            WorkOrderMetadata workOrderMetadata = doc.toObject(WorkOrderMetadata.class);
                            totalLaborCost += workOrderMetadata.getTotalLaborCost();
                        }

                        tvTotalLaborIncome.setText(DecimalFormatterUtils.formatCurrency(IncomeActivity.this, totalLaborCost));
                    }
                });
    }

    private void detachTotalLaborIncomeListener() {
        if (this.mTotalLaborListener != null) {
            this.mTotalLaborListener.remove();
            this.mTotalLaborListener = null;
        }
    }
}