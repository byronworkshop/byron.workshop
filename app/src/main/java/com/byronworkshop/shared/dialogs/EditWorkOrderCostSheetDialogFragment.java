package com.byronworkshop.shared.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.workordercost.CostSheetRVAdapter;
import com.byronworkshop.shared.dialogs.adapters.workordercost.pojo.Cost;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderMetadata;
import com.byronworkshop.utils.DecimalFormatterUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class EditWorkOrderCostSheetDialogFragment extends DialogFragment implements CostSheetRVAdapter.DeleteItemClickListener {

    // firebase analytics events
    private static final String EVENT_WORK_ORDER_COST_SHEET_EDITION = "edit_cost_sheet";

    // dialog finals
    private static final String TAG_EDIT_WO_COST_SHEET_DIALOG = "wo_cost_sheet_dialog";

    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    private String mWorkOrderFormId;
    private WorkOrderForm mWorkOrderForm;

    // database
    private CollectionReference mMotorcycleWorkOrderCostSheetCostsCollReference;
    private DocumentReference mMotorcycleWorkOrderFormDocReference;
    private DocumentReference mMotorcycleWorkOrderMetadataDocReference;

    private ListenerRegistration mCostAggregationListener;

    // resources
    private RecyclerView mCostSheetRecyclerView;
    private LinearLayout mCostSheetRVEmptyText;
    private TextView mTvCostReplacements;
    private TextView mTvCostExternal;
    private TextView mTvCostLaborCost;
    private TextView mTvCost;
    private AppCompatButton mBtnAdd;

    private FirestoreRecyclerAdapter mCostSheetAdapter;

    public static void showEditWorkOrderCostSheetDialog(
            @NonNull Context context,
            @NonNull FragmentManager fm,
            @NonNull String uId,
            @NonNull String motorcycleId,
            @NonNull String workOrderFormId,
            @NonNull WorkOrderForm workOrderForm) {
        // replace same dialog fragments with a new one
        replaceAllWithNewInstance(fm, uId, motorcycleId, workOrderFormId, workOrderForm);

        // log firebase analytics view item event
        logFirebaseViewItemEvent(context);
    }

    private static void logFirebaseViewItemEvent(Context context) {
        FirebaseAnalytics.getInstance(context).logEvent(EVENT_WORK_ORDER_COST_SHEET_EDITION, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editCostSheetDialog = fm.findFragmentByTag(TAG_EDIT_WO_COST_SHEET_DIALOG);
        if (editCostSheetDialog != null) {
            ft.remove(editCostSheetDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditWorkOrderCostSheetDialogFragment.newInstance(uId, motorcycleId, workOrderFormId, workOrderForm);
        df.show(fm, TAG_EDIT_WO_COST_SHEET_DIALOG);
    }

    private static EditWorkOrderCostSheetDialogFragment newInstance(String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        EditWorkOrderCostSheetDialogFragment frag = new EditWorkOrderCostSheetDialogFragment();

        Bundle args = new Bundle();
        args.putString("mUid", uId);
        args.putString("mMotorcycleId", motorcycleId);
        args.putString("mWorkOrderFormId", workOrderFormId);
        args.putSerializable("mWorkOrderForm", workOrderForm);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // args mandatory
            this.mUid = getArguments().getString("mUid");
            this.mMotorcycleId = getArguments().getString("mMotorcycleId");
            this.mWorkOrderFormId = getArguments().getString("mWorkOrderFormId");
            this.mWorkOrderForm = (WorkOrderForm) getArguments().getSerializable("mWorkOrderForm");
        }

        // database
        this.mMotorcycleWorkOrderCostSheetCostsCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("cost_sheets").document(this.mWorkOrderFormId).collection("costs");
        this.mMotorcycleWorkOrderFormDocReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("forms").document(this.mWorkOrderFormId);
        this.mMotorcycleWorkOrderMetadataDocReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders$metadata").document(this.mWorkOrderFormId);
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // get the layout inflater
        LayoutInflater inflater = mOwnerActivity.getLayoutInflater();
        View mDialogView = inflater.inflate(R.layout.dialog_edit_cost_sheet, null);

        // resources
        this.mCostSheetRecyclerView = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_rv);
        this.mCostSheetRVEmptyText = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_rv_empty_view);
        this.mTvCostReplacements = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_cost_replacements);
        this.mTvCostExternal = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_cost_external);
        this.mTvCostLaborCost = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_cost_labor_cost);
        this.mTvCost = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_cost_total);
        this.mBtnAdd = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_cost);

        // setting up recyclerview
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mOwnerActivity);
        this.mCostSheetRecyclerView.setLayoutManager(linearLayoutManager);
        this.mCostSheetRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // add button listener
        this.mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditWorkOrderCostSheetAddCostDialogFragment.showEditWorkOrderCostSheetAddCostDialog(
                        requireContext(),
                        requireFragmentManager(),
                        mUid,
                        mMotorcycleId,
                        mWorkOrderFormId);
            }
        });

        // create dialog
        String title = getString(R.string.dialog_edit_cost_sheet_dialog_title);
        AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity);
        builder.setTitle(title)
                .setView(mDialogView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_edit_cost_sheet_dialog_btn_close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // attach firebase
        this.attachCostSheetRVAdapter();
        this.attachCostAggrListener();
    }

    @Override
    public void onStop() {
        super.onStop();

        // detach firebase
        this.detachCostSheetRVAdapter();
        this.detachCostAggrListener();
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void attachCostAggrListener() {
        final Context context = requireContext();

        this.mTvCostReplacements.setText(DecimalFormatterUtils.formatCurrency(context, 0));
        this.mTvCostExternal.setText(DecimalFormatterUtils.formatCurrency(context, 0));
        this.mTvCostLaborCost.setText(DecimalFormatterUtils.formatCurrency(context, 0));
        this.mTvCost.setText(DecimalFormatterUtils.formatCurrency(context, 0));

        this.mCostAggregationListener = this.mMotorcycleWorkOrderCostSheetCostsCollReference.addSnapshotListener(
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null || queryDocumentSnapshots == null) {
                            return;
                        }

                        int totalReplacements = 0;
                        int totalExternal = 0;
                        int totalLaborCost = 0;

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.exists()) {
                                continue;
                            }

                            Cost c = doc.toObject(Cost.class);

                            switch (c.getType()) {
                                case Cost.TYPE_REPLACEMENT:
                                    totalReplacements += c.getAmount();
                                    break;
                                case Cost.TYPE_EXTERNAL:
                                    totalExternal += c.getAmount();
                                    break;
                                case Cost.TYPE_LABOR_COST:
                                    totalLaborCost += c.getAmount();
                                    break;
                            }
                        }

                        // total
                        int total = totalReplacements + totalExternal + totalLaborCost;

                        // update texts
                        mTvCostReplacements.setText(DecimalFormatterUtils.formatCurrency(context, totalReplacements));
                        mTvCostExternal.setText(DecimalFormatterUtils.formatCurrency(context, totalExternal));
                        mTvCostLaborCost.setText(DecimalFormatterUtils.formatCurrency(context, totalLaborCost));
                        mTvCost.setText(DecimalFormatterUtils.formatCurrency(context, total));

                        // update total cost field
                        WorkOrderMetadata workOrderMetadata = new WorkOrderMetadata(mWorkOrderForm.getDate(), total, totalReplacements, totalExternal, totalLaborCost);

                        // batch writings
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();

                        batch.set(mMotorcycleWorkOrderMetadataDocReference, workOrderMetadata);
                        batch.update(mMotorcycleWorkOrderFormDocReference, "totalCost", total);

                        batch.commit();
                    }
                });
    }

    private void detachCostAggrListener() {
        if (this.mCostAggregationListener != null) {
            this.mCostAggregationListener.remove();
            this.mCostAggregationListener = null;
        }

    }

    private void attachCostSheetRVAdapter() {
        // prepare recycler options
        FirestoreRecyclerOptions<Cost> options =
                new FirestoreRecyclerOptions.Builder<Cost>()
                        .setQuery(this.mMotorcycleWorkOrderCostSheetCostsCollReference.orderBy("type"), Cost.class)
                        .build();

        // manually stop previous existent adapter
        if (this.mCostSheetAdapter != null) {
            this.mCostSheetAdapter.stopListening();
            this.mCostSheetAdapter = null;
        }

        // create new adapter and start listening
        this.mCostSheetAdapter = new CostSheetRVAdapter(
                options,
                this,
                this.mCostSheetRVEmptyText);
        this.mCostSheetAdapter.startListening();
        this.mCostSheetRecyclerView.setAdapter(mCostSheetAdapter);
    }

    private void detachCostSheetRVAdapter() {
        if (this.mCostSheetAdapter != null) {
            this.mCostSheetAdapter.stopListening();
            this.mCostSheetAdapter = null;
            this.mCostSheetRecyclerView.setAdapter(null);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // RV Adapter listener
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onDeleteCost(final String costId, Cost cost) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.dialog_edit_cost_sheet_icon_delete_title, cost.getDescription(), cost.getAmount()))
                .setMessage(getString(R.string.dialog_edit_cost_sheet_icon_delete_msg))
                .setPositiveButton(getString(R.string.dialog_edit_cost_sheet_icon_delete_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMotorcycleWorkOrderCostSheetCostsCollReference.document(costId).delete();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_edit_cost_sheet_icon_delete_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }
}
