package com.byronworkshop.ui.detailsactivity;

import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderMetadata;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.utils.ConnectionUtils;
import com.byronworkshop.utils.DateUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;

public class DetailsActivity extends AppCompatActivity
        implements WorkOrderRVAdapter.ListItemClickListener,
        EditMotorcycleDialogFragment.MotorcycleDialogCallback,
        EditWorkOrderFormDialogFragment.WorkOrderFormDialogCallback {

    // firebase analytics events
    private static final String EVENT_DETAILS_ACTIVITY_CREATION = "show_motorcycle_details";

    // keys
    public static final String KEY_UID = "uid";
    public static final String KEY_MOTORCYCLE_ID = "motorcycleId";

    // mandatory
    private String mUid;
    private String mMotorcycleId;

    // from firebase
    private Motorcycle mMotorcycle;

    // ui
    private CoordinatorLayout mMainContainer;
    private RecyclerView mWorkOrderRecyclerView;
    private FirestoreRecyclerAdapter mWorkOrderAdapter;

    // firebase configs
    private FirebaseAnalytics mFirebaseAnalytics;
    private DocumentReference mMotorcycleDocReference;
    private CollectionReference mMotorcycleWorkOrderFormsCollReference;
    private CollectionReference mMotorcycleWorkOrderMetadataCollReference;

    private ListenerRegistration mMotorcycleChangesListener;
    private ListenerRegistration mLastWorkOrderDateListener;

    private ListenerRegistration mMotorcycleMetadataListenerLastWoDate;
    private ListenerRegistration mMotorcycleMetadataListenerCloseWo;

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
        this.mMotorcycleWorkOrderMetadataCollReference = db.collection("users").document(this.mUid).collection("work_orders$metadata");

        // firebase log
        this.logDetailsActivityCreation();

        // ui
        this.mMainContainer = findViewById(R.id.activity_details_main);
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
            case R.id.menu_details_action_delete:
                if (this.mMotorcycle == null) {
                    return false;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.content_details_action_delete_title, mMotorcycle.getBrand(), mMotorcycle.getLicensePlateNumber()))
                        .setMessage(getString(R.string.content_details_action_delete_msg))
                        .setPositiveButton(getString(R.string.content_details_action_delete_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // delete from firestore
                                        mMotorcycleDocReference.delete();

                                        // show success deletion
                                        Toast.makeText(DetailsActivity.this,
                                                getString(R.string.content_details_action_delete_confirmation, mMotorcycle.getBrand(), mMotorcycle.getLicensePlateNumber()),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        // finish activity
                                        finish();
                                    }
                                })
                        .setNegativeButton(getString(R.string.content_details_action_delete_cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                builder.create().show();
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
                        .setQuery(this.mMotorcycleWorkOrderFormsCollReference.orderBy("startDate"), WorkOrderForm.class)
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

                        // get last work order end date
                        final Date minimumDate = new Date(0L);
                        Date lastWorkOrderEndDate = minimumDate;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (doc.exists()) {
                                WorkOrderForm workOrderForm = doc.toObject(WorkOrderForm.class);

                                if (workOrderForm.isClosed()
                                        && workOrderForm.getEndDate().compareTo(lastWorkOrderEndDate) > 0) {
                                    lastWorkOrderEndDate = workOrderForm.getEndDate();
                                }
                            }
                        }

                        // check if lastWorkOrderEndDate didn't change
                        if (lastWorkOrderEndDate.equals(minimumDate)) {
                            mMotorcycleDocReference.update(FieldPath.of("metadata", "lastWorkOrderEndDate"), null);
                            return;
                        }

                        // check if current lastWorkOrderEndDate is different from db
                        final Date finalLastWorkOrderEndDate = lastWorkOrderEndDate;
                        mMotorcycleMetadataListenerLastWoDate = mMotorcycleDocReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                mMotorcycleMetadataListenerLastWoDate.remove();
                                mMotorcycleMetadataListenerLastWoDate = null;

                                if (e != null || documentSnapshot == null) {
                                    return;
                                }

                                Motorcycle m = documentSnapshot.toObject(Motorcycle.class);
                                if (m.getMetadata() == null
                                        || m.getMetadata().getLastWorkOrderEndDate() == null
                                        || !finalLastWorkOrderEndDate.equals(m.getMetadata().getLastWorkOrderEndDate())) {
                                    mMotorcycleDocReference.update(FieldPath.of("metadata", "lastWorkOrderEndDate"), finalLastWorkOrderEndDate);
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
        this.sReminder.setChecked(motorcycle.getMetadata() != null && motorcycle.getMetadata().isReminderEnabled());
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
            Snackbar.make(this.mMainContainer, getString(R.string.dialog_edit_uploaded_images_no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        EditWorkOrderUploadedImagesDialogFragment.showEditWorkOrderUploadedImagesDialog(
                this,
                getSupportFragmentManager(),
                this.mUid,
                this.mMotorcycleId,
                workOrderFormId,
                workOrderForm);
    }

    @Override
    public void onDeleteWorkOrder(final String workOrderFormId, WorkOrderForm workOrderForm) {
        String formattedDate = DateUtils.getFormattedDate(workOrderForm.getStartDate());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_work_order_more_options_delete_wo_title, workOrderForm.getIssue(), formattedDate))
                .setMessage(getString(R.string.dialog_edit_work_order_more_options_delete_wo_msg))
                .setPositiveButton(getString(R.string.dialog_edit_work_order_more_options_delete_wo_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete work order from firestore
                        mMotorcycleWorkOrderFormsCollReference.document(workOrderFormId).delete();

                        // show SnackBar
                        mWorkOrderRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Snackbar.make(mMainContainer, getString(R.string.menu_wo_item_delete_wo_success), Snackbar.LENGTH_LONG).show();
                                mWorkOrderRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
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

    @Override
    public void onCloseWorkOrder(final String workOrderFormId, WorkOrderForm workOrderForm) {
        if (!ConnectionUtils.checkInternetConnection(this)) {
            Snackbar.make(this.mMainContainer, getString(R.string.dialog_edit_work_order_more_options_close_wo_no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        String formattedDate = DateUtils.getFormattedDate(workOrderForm.getStartDate());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_edit_work_order_more_options_close_wo_title, workOrderForm.getIssue(), formattedDate))
                .setMessage(getString(R.string.dialog_edit_work_order_more_options_close_wo_msg))
                .setPositiveButton(getString(R.string.dialog_edit_work_order_more_options_close_wo_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMotorcycleMetadataListenerCloseWo = mMotorcycleWorkOrderMetadataCollReference.document(workOrderFormId)
                                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                        mMotorcycleMetadataListenerCloseWo.remove();
                                        mMotorcycleMetadataListenerCloseWo = null;

                                        if (e != null || documentSnapshot == null) {
                                            return;
                                        }

                                        // work order metadata not existent yet
                                        if (!documentSnapshot.exists()) {
                                            Snackbar.make(mMainContainer, getString(R.string.dialog_edit_work_order_more_options_close_wo_denied), Snackbar.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // check total cost on existent work order metadata
                                        WorkOrderMetadata workOrderMetadata = documentSnapshot.toObject(WorkOrderMetadata.class);
                                        if (workOrderMetadata.getTotalCost() > 0) {
                                            // update closure data
                                            WriteBatch batch = FirebaseFirestore.getInstance().batch();

                                            batch.update(mMotorcycleWorkOrderMetadataCollReference.document(workOrderFormId), "endDate", FieldValue.serverTimestamp());
                                            batch.update(mMotorcycleWorkOrderFormsCollReference.document(workOrderFormId), "endDate", FieldValue.serverTimestamp());

                                            batch.commit();

                                            // show SnackBar
                                            mWorkOrderRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                                @Override
                                                public void onGlobalLayout() {
                                                    Snackbar.make(mMainContainer, getString(R.string.menu_wo_item_close_wo_success), Snackbar.LENGTH_LONG).show();
                                                    mWorkOrderRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                                }
                                            });
                                        } else {
                                            Snackbar.make(mMainContainer, getString(R.string.dialog_edit_work_order_more_options_close_wo_denied), Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.dialog_edit_work_order_more_options_close_wo_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    // ---------------------------------------------------------------------------------------------
    // Dialog callbacks
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onMotorcycleSaveError() {
        Snackbar.make(this.mMainContainer, getString(R.string.dialog_edit_motorcycle_error_cannot_save), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onMotorcycleSaved(@NonNull String msg, @Nullable final String motorcycleId) {
        Snackbar.make(this.mMainContainer, msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onWorkOrderCreated(@NonNull final String msg) {
        mWorkOrderRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Snackbar.make(mMainContainer, msg, Snackbar.LENGTH_LONG).show();
                mWorkOrderRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onWorkOrderEdited(@NonNull final String msg) {
        Snackbar.make(mMainContainer, msg, Snackbar.LENGTH_LONG).show();
    }
}
