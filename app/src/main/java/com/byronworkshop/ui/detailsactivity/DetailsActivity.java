package com.byronworkshop.ui.detailsactivity;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.EditMotorcycleDialogFragment;
import com.byronworkshop.shared.dialogs.EditWorkOrderCostSheetDialogFragment;
import com.byronworkshop.shared.dialogs.EditWorkOrderFormDialogFragment;
import com.byronworkshop.shared.dialogs.EditWorkOrderUploadedImagesDialogFragment;
import com.byronworkshop.ui.detailsactivity.adapter.WorkOrderRVAdapter;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.utils.ConnectionUtils;
import com.byronworkshop.utils.DateUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Calendar;
import java.util.concurrent.Callable;

public class DetailsActivity extends AppCompatActivity implements WorkOrderRVAdapter.ListItemClickListener {

    // firebase analytics events
    private static final String EVENT_DETAILS_ACTIVITY_CREATION = "show_motorcycle_details";
    private static final String EVENT_MOTORCYCLE_UPDATE = "motorcycle_updated";

    private static final String PARAM_BRAND = "brand";
    private static final String PARAM_MODEL = "model";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_CC = "cc";
    private static final String PARAM_LICENSE_PLATE_NUMBER = "license_plate_number";
    private static final String PARAM_COLOR = "color";
    private static final String PARAM_LAST_WORK_ORDER_DATE = "last_service_date";
    private static final String PARAM_REMINDER_ENABLED = "reminder";

    // keys
    public static final String KEY_UID = "uid";
    public static final String KEY_MOTORCYCLE_ID = "motorcycleId";

    // mandatory
    private String mUid;
    private String mMotorcycleId;

    // from firebase
    private Motorcycle mMotorcycle;

    // ui
    private RecyclerView mWorkOrderRecyclerView;
    private FirestoreRecyclerAdapter mWorkOrderAdapter;

    // firebase configs
    private FirebaseAnalytics mFirebaseAnalytics;
    private DocumentReference mMotorcycleDocReference;
    private CollectionReference mMotorcycleWorkOrderFormsCollReference;
    private CollectionReference mMotorcycleWorkOrderMetadataCollReference;
    private CollectionReference mMotorcycleWorkOrderCostSheetsCollReference;
    private CollectionReference mMotorcycleWorkOrderImagesCollReference;

    private ListenerRegistration mMotorcycleChangesListener;
    private ListenerRegistration mLastWorkOrderDateListener;

    // resources
    private CollapsingToolbarLayout mCollapsingToolbar;
    private TextView tvModel;
    private TextView tvType;
    private TextView tvCc;
    private TextView tvColor;
    private TextView tvLicensePlateNumber;
    private SwitchCompat sReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // toolbar defaults
        Toolbar mToolbar = findViewById(R.id.activity_details_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // resources
        this.mCollapsingToolbar = findViewById(R.id.activity_details_toolbar_layout);
        this.tvModel = findViewById(R.id.activity_details_model);
        this.tvType = findViewById(R.id.content_details_type);
        this.tvCc = findViewById(R.id.content_details_cc);
        this.tvColor = findViewById(R.id.content_details_color);
        this.tvLicensePlateNumber = findViewById(R.id.content_details_license_plate_number);
        this.sReminder = findViewById(R.id.content_details_reminder);

        // retrieve arguments
        if (this.getIntent().getExtras() != null
                && this.getIntent().getExtras().containsKey(KEY_UID)
                && this.getIntent().getExtras().containsKey(KEY_MOTORCYCLE_ID)) {
            this.mUid = this.getIntent().getExtras().getString(KEY_UID);
            this.mMotorcycleId = this.getIntent().getExtras().getString(KEY_MOTORCYCLE_ID);
        } else {
            throw new IllegalArgumentException(getString(R.string.activity_details_creation_error));
        }

        // firebase initialization
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        this.mMotorcycleDocReference = db.collection("users").document(this.mUid).collection("motorcycles").document(this.mMotorcycleId);
        this.mMotorcycleWorkOrderFormsCollReference = db.collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("forms");
        this.mMotorcycleWorkOrderCostSheetsCollReference = db.collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("cost_sheets");
        this.mMotorcycleWorkOrderImagesCollReference = db.collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("file_repos");
        this.mMotorcycleWorkOrderMetadataCollReference = db.collection("users").document(this.mUid).collection("work_orders$metadata");

        // firebase log
        this.logDetailsActivityCreation();

        // ui
        this.mWorkOrderRecyclerView = findViewById(R.id.content_details_rv_wos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setReverseLayout(true);
        this.mWorkOrderRecyclerView.setLayoutManager(linearLayoutManager);
        this.mWorkOrderRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // add work order
        FloatingActionButton fab = findViewById(R.id.activity_details_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditWorkOrderFormDialogFragment.showEditWorkOrderFormDialog(
                        DetailsActivity.this,
                        getSupportFragmentManager(),
                        mUid,
                        mMotorcycleId,
                        null,
                        null);
            }
        });

        // change reminder state
        this.sReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                FieldPath reminderEnabledPath = FieldPath.of("metadata", "reminderEnabled");

                mMotorcycleDocReference.update(reminderEnabledPath, isChecked);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // activity lifecycle
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onStart() {
        super.onStart();

        // attach firebase
        this.attachWorkOrderRVAdapter();
        this.attachLastWorkOrderDateListener();
        this.attachMotorcycleListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // detach firebase
        this.detachWorkOrderRVAdapter();
        this.detachLastWorkOrderDateListener();
        this.detachMotorcycleListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_details_action_edit:
                EditMotorcycleDialogFragment.showEditMotorcycleDialog(
                        this,
                        getSupportFragmentManager(),
                        this.mUid,
                        this.mMotorcycleId,
                        this.mMotorcycle);

                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void logDetailsActivityCreation() {
        this.mFirebaseAnalytics.logEvent(EVENT_DETAILS_ACTIVITY_CREATION, null);
    }


    private void attachWorkOrderRVAdapter() {
        // set recycler options
        FirestoreRecyclerOptions<WorkOrderForm> options =
                new FirestoreRecyclerOptions.Builder<WorkOrderForm>()
                        .setQuery(this.mMotorcycleWorkOrderFormsCollReference.orderBy("date"), WorkOrderForm.class)
                        .build();

        // manually stop previous existent adapter
        if (this.mWorkOrderAdapter != null) {
            this.mWorkOrderAdapter.stopListening();
            this.mWorkOrderAdapter = null;
        }

        // create new adapter and let activity to start/stop
        this.mWorkOrderAdapter = new WorkOrderRVAdapter(
                options,
                this,
                (LinearLayout) findViewById(R.id.content_details_rv_wo_empty_container),
                (TextView) findViewById(R.id.content_details_rv_wo_title));
        this.mWorkOrderAdapter.startListening();
        this.mWorkOrderRecyclerView.setAdapter(mWorkOrderAdapter);
    }

    private void detachWorkOrderRVAdapter() {
        if (this.mWorkOrderAdapter != null) {
            this.mWorkOrderAdapter.stopListening();
            this.mWorkOrderAdapter = null;
            this.mWorkOrderRecyclerView.setAdapter(null);
        }
    }

    private void attachMotorcycleListener() {
        this.mMotorcycleChangesListener = this.mMotorcycleDocReference.addSnapshotListener(
                new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e == null && documentSnapshot != null && documentSnapshot.exists()) {
                            // motorcycle exists fill ui
                            mMotorcycle = documentSnapshot.toObject(Motorcycle.class);
                            logMotorcycleUpdate(documentSnapshot.getId(), mMotorcycle);
                            fillUI(mMotorcycle);
                        } else {
                            // motorcycle deleted, go back safely
                            terminateUI();
                        }
                    }
                });
    }

    private void detachMotorcycleListener() {
        if (this.mMotorcycleChangesListener != null) {
            this.mMotorcycleChangesListener.remove();
            this.mMotorcycleChangesListener = null;
        }
    }

    private void attachLastWorkOrderDateListener() {
        this.mLastWorkOrderDateListener = this.mMotorcycleWorkOrderFormsCollReference.addSnapshotListener(
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null || queryDocumentSnapshots == null) {
                            return;
                        }

                        long lastWorkOrderDate = -1;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.exists()) {
                                WorkOrderForm workOrderForm = doc.toObject(WorkOrderForm.class);

                                if (workOrderForm.getDate() > lastWorkOrderDate) {
                                    lastWorkOrderDate = workOrderForm.getDate();
                                }
                            }
                        }

                        // check if current lastWorkOrderDate is different from db
                        final long finalLastWorkOrderDate = lastWorkOrderDate;
                        mMotorcycleDocReference.get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        if (documentSnapshot != null && documentSnapshot.exists()) {
                                            Motorcycle m = documentSnapshot.toObject(Motorcycle.class);

                                            if (m != null &&
                                                    m.getMetadata() != null
                                                    && finalLastWorkOrderDate != m.getMetadata().getLastWorkOrderDate()) {
                                                WriteBatch writeBatch = FirebaseFirestore.getInstance().batch();
                                                writeBatch.update(mMotorcycleDocReference, FieldPath.of("metadata", "lastWorkOrderDate"), finalLastWorkOrderDate);
                                                writeBatch.commit();
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    private void detachLastWorkOrderDateListener() {
        if (this.mLastWorkOrderDateListener != null) {
            this.mLastWorkOrderDateListener.remove();
            this.mLastWorkOrderDateListener = null;
        }
    }

    private void terminateUI() {
        finish();
    }

    private void logMotorcycleUpdate(String motorcycleId, Motorcycle motorcycle) {
        Bundle motorcycleInfo = new Bundle();
        motorcycleInfo.putString(FirebaseAnalytics.Param.ITEM_ID, motorcycleId);
        motorcycleInfo.putString(PARAM_BRAND, motorcycle.getBrand());
        motorcycleInfo.putString(PARAM_MODEL, motorcycle.getModel());
        motorcycleInfo.putString(PARAM_TYPE, motorcycle.getType());
        motorcycleInfo.putInt(PARAM_CC, motorcycle.getCc());
        motorcycleInfo.putString(PARAM_LICENSE_PLATE_NUMBER, motorcycle.getLicensePlateNumber());
        motorcycleInfo.putString(PARAM_COLOR, motorcycle.getColor());
        motorcycleInfo.putLong(PARAM_LAST_WORK_ORDER_DATE, motorcycle.getMetadata().getLastWorkOrderDate());
        motorcycleInfo.putString(PARAM_REMINDER_ENABLED, motorcycle.getMetadata().isReminderEnabled() ? "enabled" : "disabled");
        this.mFirebaseAnalytics.logEvent(EVENT_MOTORCYCLE_UPDATE, motorcycleInfo);
    }

    private void fillUI(Motorcycle motorcycle) {
        if (motorcycle == null) {
            terminateUI();
            return;
        }

        // texts on toolbar
        this.mCollapsingToolbar.setTitle(motorcycle.getBrand());
        this.tvLicensePlateNumber.setText(motorcycle.getLicensePlateNumber());

        // info
        this.tvModel.setText(motorcycle.getModel());
        this.tvType.setText(motorcycle.getType());
        this.tvCc.setText(getString(R.string.activity_details_tv_cc, motorcycle.getCc()));
        this.tvColor.setText(motorcycle.getColor());

        // avatar
        ImageView detailsAvatar = findViewById(R.id.activity_details_avatar);
        if (motorcycle.getImage() != null) {
            ColorDrawable imagePlaceholder = new ColorDrawable(ContextCompat.getColor(this, R.color.colorPlaceholder));
            RequestOptions options = RequestOptions.placeholderOf(imagePlaceholder);

            detailsAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(motorcycle.getImage().getImagePublicUrl())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(detailsAvatar);
        } else {
            detailsAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            detailsAvatar.setImageResource(R.drawable.ic_avatar_placeholder_no_image);
        }

        // reminder
        this.sReminder.setChecked(motorcycle.getMetadata().isReminderEnabled());
    }

    // ---------------------------------------------------------------------------------------------
    // RV Adapter listener
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onShowEditWorkOrderDialog(String workOrderFormId, WorkOrderForm workOrderForm) {
        EditWorkOrderFormDialogFragment.showEditWorkOrderFormDialog(
                this,
                this.getSupportFragmentManager(),
                this.mUid,
                this.mMotorcycleId,
                workOrderFormId,
                workOrderForm);
    }

    @Override
    public void onShowEditCostSheetDialog(String workOrderFormId, WorkOrderForm workOrderForm) {
        EditWorkOrderCostSheetDialogFragment.showEditWorkOrderCostSheetDialog(
                this,
                this.getSupportFragmentManager(),
                this.mUid,
                this.mMotorcycleId,
                workOrderFormId,
                workOrderForm);
    }

    @Override
    public void onShowEditUploadedImagesDialog(String workOrderFormId, WorkOrderForm workOrderForm) {
        if (!ConnectionUtils.checkInternetConnection(this)) {
            Toast.makeText(this, getString(R.string.dialog_edit_uploaded_images_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        EditWorkOrderUploadedImagesDialogFragment.showEditWorkOrderUploadedImagesDialog(
                this,
                getSupportFragmentManager(),
                this.mUid,
                this.mMotorcycleId,
                workOrderFormId);
    }

    @Override
    public void onDeleteWorkOrder(final String workOrderFormId, WorkOrderForm workOrderForm) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(workOrderForm.getDate());

        String formattedDate = DateUtils.getFormattedDate(c);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_work_order_more_options_delete_wo_title, workOrderForm.getIssue(), formattedDate))
                .setMessage(getString(R.string.dialog_edit_work_order_more_options_delete_wo_msg))
                .setPositiveButton(getString(R.string.dialog_edit_work_order_more_options_delete_wo_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        Tasks.call(new Callable<Void>() {
                            @Override
                            public Void call() {
                                // add work order form to WriteBatch
                                batch.delete(mMotorcycleWorkOrderFormsCollReference.document(workOrderFormId));

                                return null;
                            }
                        }).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(@NonNull Task<Void> task) {
                                // open task result
                                task.getResult();

                                // add work order metadata to WriteBatch
                                batch.delete(mMotorcycleWorkOrderMetadataCollReference.document(workOrderFormId));

                                return null;
                            }
                        }).continueWithTask(new Continuation<Void, Task<QuerySnapshot>>() {
                            @Override
                            public Task<QuerySnapshot> then(@NonNull Task<Void> task) {
                                // open task result
                                task.getResult();

                                // get all costs documents
                                return mMotorcycleWorkOrderCostSheetsCollReference
                                        .document(workOrderFormId)
                                        .collection("costs")
                                        .get();
                            }
                        }).continueWith(new Continuation<QuerySnapshot, Void>() {
                            @Override
                            public Void then(@NonNull Task<QuerySnapshot> task) {
                                // open task result
                                QuerySnapshot queryDocumentSnapshots = task.getResult();

                                // add work order costs to WriteBatch
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    if (document.exists()) {
                                        batch.delete(document.getReference());
                                    }
                                }

                                // add work order cost sheet to WriteBatch
                                batch.delete(mMotorcycleWorkOrderCostSheetsCollReference.document(workOrderFormId));

                                return null;
                            }
                        }).continueWithTask(new Continuation<Void, Task<QuerySnapshot>>() {
                            @Override
                            public Task<QuerySnapshot> then(@NonNull Task<Void> task) {
                                // open task result
                                task.getResult();

                                // get all work order images
                                return mMotorcycleWorkOrderImagesCollReference
                                        .document(workOrderFormId)
                                        .collection("images")
                                        .get();
                            }
                        }).continueWithTask(new Continuation<QuerySnapshot, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<QuerySnapshot> task) {
                                // open task result
                                QuerySnapshot queryDocumentSnapshots = task.getResult();

                                // add work order images to WriteBatch
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    if (document.exists()) {
                                        batch.delete(document.getReference());
                                    }
                                }

                                batch.delete(mMotorcycleWorkOrderImagesCollReference.document(workOrderFormId));

                                return batch.commit();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(DetailsActivity.this, getString(R.string.activity_details_delete_wo_error), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton(getString(R.string.dialog_edit_work_order_more_options_delete_wo_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
}
