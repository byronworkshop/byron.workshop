package com.byronworkshop.shared.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.workordercost.pojo.Cost;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditWorkOrderCostSheetAddCostDialogFragment extends DialogFragment {

    // firebase analytics events
    private static final String EVENT_WORK_ORDER_ADD_COST = "add_cost";

    // dialog finals
    private static final String TAG_EDIT_WO_COST_SHEET_ADD_COST_DIALOG = "wo_cost_sheet_add_cost_dialog";
    private static final String KEY_SELECTED_TYPE = "selected_type";

    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    private String mWorkOrderFormId;

    // database
    private CollectionReference mMotorcycleWorkOrderCostSheetCostsCollReference;

    // resources
    private CoordinatorLayout mMainContainer;
    private TextInputLayout tiDescription;
    private EditText etDescription;
    private TextInputLayout tiAmount;
    private EditText etAmount;
    private RadioGroup rgType;

    // selected type
    private String selectedCostType;

    public static void showEditWorkOrderCostSheetAddCostDialog(
            @NonNull Context context,
            @NonNull FragmentManager fm,
            @NonNull String uId,
            @NonNull String motorcycleId,
            @NonNull String workOrderFormId) {
        // replace same dialog fragments with a new one
        replaceAllWithNewInstance(fm, uId, motorcycleId, workOrderFormId);

        // log firebase analytics view item event
        logFirebaseViewItemEvent(context);
    }

    private static void logFirebaseViewItemEvent(Context context) {
        FirebaseAnalytics.getInstance(context).logEvent(EVENT_WORK_ORDER_ADD_COST, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, String workOrderFormId) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editCostSheetAddCostDialog = fm.findFragmentByTag(TAG_EDIT_WO_COST_SHEET_ADD_COST_DIALOG);
        if (editCostSheetAddCostDialog != null) {
            ft.remove(editCostSheetAddCostDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditWorkOrderCostSheetAddCostDialogFragment.newInstance(uId, motorcycleId, workOrderFormId);
        df.show(fm, TAG_EDIT_WO_COST_SHEET_ADD_COST_DIALOG);
    }

    private static EditWorkOrderCostSheetAddCostDialogFragment newInstance(String uId, String motorcycleId, String workOrderFormId) {
        EditWorkOrderCostSheetAddCostDialogFragment frag = new EditWorkOrderCostSheetAddCostDialogFragment();

        Bundle args = new Bundle();
        args.putString("mUid", uId);
        args.putString("mMotorcycleId", motorcycleId);
        args.putString("mWorkOrderFormId", workOrderFormId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_SELECTED_TYPE, this.selectedCostType);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // args mandatory
            this.mUid = getArguments().getString("mUid");
            this.mMotorcycleId = getArguments().getString("mMotorcycleId");
            this.mWorkOrderFormId = getArguments().getString("mWorkOrderFormId");
        }

        // database
        this.mMotorcycleWorkOrderCostSheetCostsCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("cost_sheets").document(this.mWorkOrderFormId).collection("costs");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // get the layout inflater
        LayoutInflater inflater = mOwnerActivity.getLayoutInflater();
        View mDialogView = inflater.inflate(R.layout.dialog_edit_cost_sheet_add_cost, null);

        // resources
        this.mMainContainer = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_main);
        this.tiDescription = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_input_layout_add_cost_desc);
        this.etDescription = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_cost_desc);
        this.tiAmount = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_input_layout_add_cost_amount);
        this.etAmount = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_amount);
        this.rgType = mDialogView.findViewById(R.id.dialog_edit_cost_sheet_add_cost_type);

        // retrieve type if recreation
        this.selectedCostType = Cost.TYPE_REPLACEMENT;
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_TYPE)) {
            this.selectedCostType = savedInstanceState.getString(KEY_SELECTED_TYPE);

            //noinspection ConstantConditions
            switch (this.selectedCostType) {
                case Cost.TYPE_REPLACEMENT:
                    this.rgType.check(R.id.dialog_edit_cost_sheet_add_cost_type_replacement);
                    break;
                case Cost.TYPE_EXTERNAL:
                    this.rgType.check(R.id.dialog_edit_cost_sheet_add_cost_type_external);
                    break;
                case Cost.TYPE_LABOR_COST:
                    this.rgType.check(R.id.dialog_edit_cost_sheet_add_cost_type_labor_cost);
                    break;
                default:
                    this.rgType.check(R.id.dialog_edit_cost_sheet_add_cost_type_labor_cost);
            }
        }

        // type listener
        this.rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.dialog_edit_cost_sheet_add_cost_type_replacement:
                        selectedCostType = Cost.TYPE_REPLACEMENT;
                        break;
                    case R.id.dialog_edit_cost_sheet_add_cost_type_external:
                        selectedCostType = Cost.TYPE_EXTERNAL;
                        break;
                    case R.id.dialog_edit_cost_sheet_add_cost_type_labor_cost:
                        selectedCostType = Cost.TYPE_LABOR_COST;
                        break;
                }
            }
        });

        // create dialog
        String title = getString(R.string.dialog_edit_cost_sheet_add_cost_title);
        AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity);
        builder.setTitle(title)
                .setView(mDialogView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_edit_cost_sheet_add_cost_ok), null)
                .setNegativeButton(getString(R.string.dialog_edit_cost_sheet_add_cost_cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitForm();
                    }
                });

                Button buttonNegative = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                buttonNegative.setFocusable(false);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
            }
        });

        return alertDialog;
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void submitForm() {
        if (this.isFormValid()) {
            this.submitNewForm();
        } else {
            Snackbar.make(this.mMainContainer, getString(R.string.dialog_edit_cost_sheet_add_cost_err_form), Snackbar.LENGTH_SHORT).show();
        }
    }

    private boolean isFormValid() {
        int errorCounter = 0;

        if (TextUtils.isEmpty(this.etDescription.getText().toString())) {
            this.tiDescription.setError(getString(R.string.dialog_edit_cost_sheet_add_cost_err_desc));
            errorCounter++;
        } else {
            this.tiDescription.setError(null);
        }

        if (TextUtils.isEmpty(this.etAmount.getText().toString())) {
            this.tiAmount.setError(getString(R.string.dialog_edit_cost_sheet_add_cost_err_amount));
            errorCounter++;
        } else if (Integer.parseInt(etAmount.getText().toString()) <= 0) {
            this.tiAmount.setError(getString(R.string.dialog_edit_cost_sheet_add_cost_err_amount_positive));
            errorCounter++;
        } else {
            this.tiAmount.setError(null);
        }

        return errorCounter == 0;
    }

    private void submitNewForm() {
        String type = this.selectedCostType;
        String description = this.etDescription.getText().toString();
        int amount = Integer.parseInt(this.etAmount.getText().toString());

        Cost c = new Cost(type, description, amount);
        this.mMotorcycleWorkOrderCostSheetCostsCollReference.add(c);

        dismiss();
    }
}
