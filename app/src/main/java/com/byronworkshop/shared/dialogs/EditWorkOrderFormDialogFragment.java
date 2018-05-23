package com.byronworkshop.shared.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.byronworkshop.R;
import com.byronworkshop.databinding.DialogEditWorkOrderBinding;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.utils.DatePickerFragment;
import com.byronworkshop.utils.DateUtils;
import com.byronworkshop.utils.TimePickerFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class EditWorkOrderFormDialogFragment extends DialogFragment {

    // firebase analytics events
    private static final String EVENT_WORK_ORDER_FORM_CREATION = "create_work_order";
    private static final String EVENT_WORK_ORDER_FORM_EDITION = "edit_work_order";

    // dialogs finals
    private static final String TAG_EDIT_WORK_ORDER_FORM_DIALOG = "wo_form_dialog";
    private static final String KEY_SELECTED_START_DATE = "selected_start_date";

    // activity callback
    public interface WorkOrderFormDialogCallback {
        void onWorkOrderCreated(@NonNull String msg);

        void onWorkOrderEdited(@NonNull String msg);
    }

    private WorkOrderFormDialogCallback mWorkOrderFormDialogCallback;


    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    // arg on edition
    private String mWorkOrderFormId;
    private WorkOrderForm mWorkOrderForm;

    // resources
    private Calendar selectedStartDate;

    // databinding
    private DialogEditWorkOrderBinding binding;

    // firebase
    private CollectionReference mMotorcycleWorkOrderFormsCollReference;

    public static void showEditWorkOrderFormDialog(
            @NonNull Context context,
            @NonNull FragmentManager fm,
            @NonNull String uId,
            @NonNull String motorcycleId,
            @Nullable String workOrderFormId,
            @Nullable WorkOrderForm workOrderForm) {
        // replace same dialog fragments with a new one
        replaceAllWithNewInstance(fm, uId, motorcycleId, workOrderFormId, workOrderForm);

        // log firebase analytics view item event
        logFirebaseViewItemEvent(context, !TextUtils.isEmpty(workOrderFormId) && workOrderForm != null);
    }

    private static void logFirebaseViewItemEvent(Context context, boolean isEditionMode) {
        FirebaseAnalytics.getInstance(context).logEvent(isEditionMode ?
                EVENT_WORK_ORDER_FORM_EDITION : EVENT_WORK_ORDER_FORM_CREATION, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editWorkOrderDialog = fm.findFragmentByTag(TAG_EDIT_WORK_ORDER_FORM_DIALOG);
        if (editWorkOrderDialog != null) {
            ft.remove(editWorkOrderDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditWorkOrderFormDialogFragment.newInstance(uId, motorcycleId, workOrderFormId, workOrderForm);
        df.show(fm, TAG_EDIT_WORK_ORDER_FORM_DIALOG);
    }

    private static EditWorkOrderFormDialogFragment newInstance(String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        EditWorkOrderFormDialogFragment frag = new EditWorkOrderFormDialogFragment();

        Bundle args = new Bundle();
        args.putString("mUid", uId);
        args.putString("mMotorcycleId", motorcycleId);
        args.putString("mWorkOrderFormId", workOrderFormId);
        args.putSerializable("mWorkOrderForm", workOrderForm);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_SELECTED_START_DATE, this.selectedStartDate);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // args mandatory
            this.mUid = getArguments().getString("mUid");
            this.mMotorcycleId = getArguments().getString("mMotorcycleId");
            // args on edition
            this.mWorkOrderFormId = getArguments().getString("mWorkOrderFormId");
            this.mWorkOrderForm = (WorkOrderForm) getArguments().getSerializable("mWorkOrderForm");
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_START_DATE)) {
            this.selectedStartDate = (Calendar) savedInstanceState.getSerializable(KEY_SELECTED_START_DATE);
        }

        this.mMotorcycleWorkOrderFormsCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("forms");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // get the layout inflater
        this.binding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()),
                R.layout.dialog_edit_work_order, null, false);

        // load form for edition
        if (this.isEditionMode()) {
            this.setEditionForm();
        } else {
            this.setNewForm();
        }

        // date picker
        this.setDateTimePickers();

        // create dialog
        String title = !this.isEditionMode() ? getString(R.string.dialog_edit_work_order_title_new)
                : this.mWorkOrderForm.isClosed() ?
                getString(R.string.dialog_edit_work_order_title_closed) :
                getString(R.string.dialog_edit_work_order_title_edit);
        AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity);
        builder.setTitle(title)
                .setView(binding.getRoot())
                .setCancelable(false);

        if (!this.isEditionMode()
                || !this.mWorkOrderForm.isClosed()) {
            builder.setPositiveButton(getString(R.string.dialog_edit_work_order_save), null);
            builder.setNegativeButton(getString(R.string.dialog_edit_work_order_cancel), null);
        } else {
            builder.setNegativeButton(getString(R.string.dialog_edit_work_order_close), null);
        }


        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (!isEditionMode() || !mWorkOrderForm.isClosed()) {
                    Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonPositive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            submitForm(isEditionMode());
                        }
                    });
                }

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            this.mWorkOrderFormDialogCallback = (WorkOrderFormDialogCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement WorkOrderFormDialogCallback");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void setDateTimePickers() {
        if (!this.isEditionMode()
                || !this.mWorkOrderForm.isClosed()) {
            this.binding.dialogEditWoStartDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });
            this.binding.dialogEditWoStartTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePickerDialog();
                }
            });
        }
    }

    private void setNewForm() {
        // set default start date and time
        this.selectedStartDate = Calendar.getInstance();
        this.binding.dialogEditWoStartDate.setText(DateUtils.getFormattedDate(this.selectedStartDate.getTime()));
        this.binding.dialogEditWoStartTime.setText(DateUtils.getFormattedTime(this.selectedStartDate.getTime()));
    }

    private void setEditionForm() {
        // set start date
        Date savedStartDate;
        if (this.selectedStartDate == null) {
            savedStartDate = this.mWorkOrderForm.getStartDate();
        } else {
            savedStartDate = this.selectedStartDate.getTime();
        }

        String dateStr = DateUtils.getFormattedDate(savedStartDate);
        String timeStr = DateUtils.getFormattedTime(savedStartDate);

        Calendar savedCalendarStartDate = Calendar.getInstance();
        savedCalendarStartDate.setTime(savedStartDate);
        this.selectedStartDate = savedCalendarStartDate;
        this.binding.dialogEditWoStartDate.setText(dateStr);
        this.binding.dialogEditWoStartTime.setText(timeStr);

        // header resources
        this.binding.dialogEditWoStartDate.setText(dateStr);
        this.binding.dialogEditWoIssue.setText(this.mWorkOrderForm.getIssue());
        this.binding.dialogEditWoInitialCompression.setText(this.mWorkOrderForm.getInitialCompLevel() != -1 ? String.valueOf(this.mWorkOrderForm.getInitialCompLevel()) : "");
        this.binding.dialogEditWoFinalCompression.setText(this.mWorkOrderForm.getFinalCompLevel() != -1 ? String.valueOf(this.mWorkOrderForm.getFinalCompLevel()) : "");
        this.binding.dialogEditWoOthers.setText(this.mWorkOrderForm.getOthers());

        // accessories resources
        this.binding.dialogEditWoAccessoriesRims.setChecked(this.mWorkOrderForm.isRims());
        this.binding.dialogEditWoAccessoriesIgnition.setChecked(this.mWorkOrderForm.isIgnition());
        this.binding.dialogEditWoAccessoriesBattery.setChecked(this.mWorkOrderForm.isBattery());
        this.binding.dialogEditWoAccessoriesHorn.setChecked(this.mWorkOrderForm.isHorn());
        this.binding.dialogEditWoAccessoriesCentralBipod.setChecked(this.mWorkOrderForm.isCentralBipod());
        this.binding.dialogEditWoAccessoriesCaps.setChecked(this.mWorkOrderForm.isCaps());
        this.binding.dialogEditWoAccessoriesCdi.setChecked(this.mWorkOrderForm.isCdi());
        this.binding.dialogEditWoAccessoriesIgnitionSwitch.setChecked(this.mWorkOrderForm.isIgnitionSwitch());
        this.binding.dialogEditWoAccessoriesClutchCable.setChecked(this.mWorkOrderForm.isClutchCable());
        this.binding.dialogEditWoAccessoriesExhaust.setChecked(this.mWorkOrderForm.isExhaust());
        this.binding.dialogEditWoAccessoriesHeadlight.setChecked(this.mWorkOrderForm.isHeadlight());
        this.binding.dialogEditWoAccessoriesFilters.setChecked(this.mWorkOrderForm.isFilters());
        this.binding.dialogEditWoAccessoriesLightbulbs.setChecked(this.mWorkOrderForm.isLightBulbs());
        this.binding.dialogEditWoAccessoriesBrakes.setChecked(this.mWorkOrderForm.isBrakes());
        this.binding.dialogEditWoAccessoriesMudguard.setChecked(this.mWorkOrderForm.isMudguard());
        this.binding.dialogEditWoAccessoriesWinkers.setChecked(this.mWorkOrderForm.isWinkers());
        this.binding.dialogEditWoAccessoriesTools.setChecked(this.mWorkOrderForm.isTools());
        this.binding.dialogEditWoAccessoriesEmblems.setChecked(this.mWorkOrderForm.isEmblems());
        this.binding.dialogEditWoAccessoriesTires.setChecked(this.mWorkOrderForm.isTires());
        this.binding.dialogEditWoAccessoriesKey.setChecked(this.mWorkOrderForm.isKey());
        this.binding.dialogEditWoAccessoriesCommands.setChecked(this.mWorkOrderForm.isCommands());
        this.binding.dialogEditWoAccessoriesHoses.setChecked(this.mWorkOrderForm.isHoses());
        this.binding.dialogEditWoAccessoriesHandlebar.setChecked(this.mWorkOrderForm.isHandlebar());
        this.binding.dialogEditWoAccessoriesSeat.setChecked(this.mWorkOrderForm.isSeat());
        this.binding.dialogEditWoAccessoriesCatEyes.setChecked(this.mWorkOrderForm.isCatEyes());
        this.binding.dialogEditWoAccessoriesLevers.setChecked(this.mWorkOrderForm.isLevers());
        this.binding.dialogEditWoAccessoriesRacks.setChecked(this.mWorkOrderForm.isRacks());
        this.binding.dialogEditWoAccessoriesPaint.setChecked(this.mWorkOrderForm.isPaint());
        this.binding.dialogEditWoAccessoriesToolHolder.setChecked(this.mWorkOrderForm.isToolHolder());
        this.binding.dialogEditWoAccessoriesHandGrips.setChecked(this.mWorkOrderForm.isHandGrips());
        this.binding.dialogEditWoAccessoriesRadiator.setChecked(this.mWorkOrderForm.isRadiator());
        this.binding.dialogEditWoAccessoriesRectifier.setChecked(this.mWorkOrderForm.isRectifier());
        this.binding.dialogEditWoAccessoriesRearviewMirrors.setChecked(this.mWorkOrderForm.isRearviewMirrors());
        this.binding.dialogEditWoAccessoriesElectricSystem.setChecked(this.mWorkOrderForm.isElectricSystem());
        this.binding.dialogEditWoAccessoriesLicensePlateSupport.setChecked(this.mWorkOrderForm.isLicensePlateSupport());
        this.binding.dialogEditWoAccessoriesChainCover.setChecked(this.mWorkOrderForm.isChainCover());
        this.binding.dialogEditWoAccessoriesFuelCap.setChecked(this.mWorkOrderForm.isFuelCap());
        this.binding.dialogEditWoAccessoriesValveCover.setChecked(this.mWorkOrderForm.isValveCover());
        this.binding.dialogEditWoAccessoriesEngineCovers.setChecked(this.mWorkOrderForm.isEngineCovers());
        this.binding.dialogEditWoAccessoriesSideCovers.setChecked(this.mWorkOrderForm.isSideCovers());
        this.binding.dialogEditWoAccessoriesUpholstery.setChecked(this.mWorkOrderForm.isUpholstery());
        this.binding.dialogEditWoAccessoriesTelescopicFork.setChecked(this.mWorkOrderForm.isTelescopicFork());
        this.binding.dialogEditWoAccessoriesChainTensioner.setChecked(this.mWorkOrderForm.isChainTensioner());
        this.binding.dialogEditWoAccessoriesTransmission.setChecked(this.mWorkOrderForm.isTransmission());
        this.binding.dialogEditWoAccessoriesSpeedometer.setChecked(this.mWorkOrderForm.isSpeedometer());

        // preventive maintenance
        this.binding.dialogEditWoPrevMaintOil.setChecked(this.mWorkOrderForm.isOil());
        this.binding.dialogEditWoPrevMaintEngineTuning.setChecked(this.mWorkOrderForm.isEngineTuning());
        this.binding.dialogEditWoPrevMaintBoltAdjustment.setChecked(this.mWorkOrderForm.isBoltAdjustment());
        this.binding.dialogEditWoPrevMaintChangeSparkPlugs.setChecked(this.mWorkOrderForm.isChangeSparkPlugs());
        this.binding.dialogEditWoPrevMaintCarburetion.setChecked(this.mWorkOrderForm.isCarburetion());
        this.binding.dialogEditWoPrevMaintLubrication.setChecked(this.mWorkOrderForm.isLubrication());
        this.binding.dialogEditWoPrevMaintFusesRevision.setChecked(this.mWorkOrderForm.isFusesRevision());
        this.binding.dialogEditWoPrevMaintTransmissionCleaning.setChecked(this.mWorkOrderForm.isTransmissionCleaning());
        this.binding.dialogEditWoPrevMaintTirePressure.setChecked(this.mWorkOrderForm.isTirePressure());
        this.binding.dialogEditWoPrevMaintBreakAdjustment.setChecked(this.mWorkOrderForm.isBreakAdjustment());

        // corrective maintenance
        this.binding.dialogEditWoCorrMaintPistonRings.setChecked(this.mWorkOrderForm.isPistonRings());
        this.binding.dialogEditWoCorrMaintGearbox.setChecked(this.mWorkOrderForm.isGearBox());
        this.binding.dialogEditWoCorrMaintEngineHead.setChecked(this.mWorkOrderForm.isEngineHead());
        this.binding.dialogEditWoCorrMaintClutchPlates.setChecked(this.mWorkOrderForm.isClutchPlates());
        this.binding.dialogEditWoCorrMaintStarter.setChecked(this.mWorkOrderForm.isStarter());
        this.binding.dialogEditWoCorrMaintPiston.setChecked(this.mWorkOrderForm.isPiston());
        this.binding.dialogEditWoCorrMaintClutchPress.setChecked(this.mWorkOrderForm.isClutchPress());
        this.binding.dialogEditWoCorrMaintGrinding.setChecked(this.mWorkOrderForm.isGrinding());
        this.binding.dialogEditWoCorrMaintSolenoid.setChecked(this.mWorkOrderForm.isSolenoid());
        this.binding.dialogEditWoCorrMaintTransmission.setChecked(this.mWorkOrderForm.isTransmissionCorrection());
        this.binding.dialogEditWoCorrMaintValves.setChecked(this.mWorkOrderForm.isValves());

        // disable form on closed work order
        if (this.mWorkOrderForm.isClosed()) {
            // set end date
            String endDateStr = DateUtils.getFormattedDate(this.mWorkOrderForm.getEndDate());
            String endTimeStr = DateUtils.getFormattedTime(this.mWorkOrderForm.getEndDate());

            this.binding.dialogEditWoEndDateImg.setVisibility(View.VISIBLE);
            this.binding.dialogEditWoEndDate.setVisibility(View.VISIBLE);

            this.binding.dialogEditWoEndDate.setText(getString(R.string.dialog_edit_work_order_closed_label, endDateStr, endTimeStr));

            // disable header
            this.binding.dialogEditWoInputLayoutWoStartDate.setClickable(false);
            this.binding.dialogEditWoStartDate.setClickable(false);
            this.binding.dialogEditWoStartDate.setKeyListener(null);
            this.binding.dialogEditWoInputLayoutWoStartTime.setClickable(false);
            this.binding.dialogEditWoStartTime.setClickable(false);
            this.binding.dialogEditWoStartTime.setKeyListener(null);
            this.binding.dialogEditWoIssue.setKeyListener(null);
            this.binding.dialogEditWoInitialCompression.setKeyListener(null);
            this.binding.dialogEditWoFinalCompression.setKeyListener(null);
            this.binding.dialogEditWoOthers.setKeyListener(null);

            // disable accessories resources
            this.binding.dialogEditWoAccessoriesRims.setClickable(false);
            this.binding.dialogEditWoAccessoriesIgnition.setClickable(false);
            this.binding.dialogEditWoAccessoriesBattery.setClickable(false);
            this.binding.dialogEditWoAccessoriesHorn.setClickable(false);
            this.binding.dialogEditWoAccessoriesCentralBipod.setClickable(false);
            this.binding.dialogEditWoAccessoriesCaps.setClickable(false);
            this.binding.dialogEditWoAccessoriesCdi.setClickable(false);
            this.binding.dialogEditWoAccessoriesIgnitionSwitch.setClickable(false);
            this.binding.dialogEditWoAccessoriesClutchCable.setClickable(false);
            this.binding.dialogEditWoAccessoriesExhaust.setClickable(false);
            this.binding.dialogEditWoAccessoriesHeadlight.setClickable(false);
            this.binding.dialogEditWoAccessoriesFilters.setClickable(false);
            this.binding.dialogEditWoAccessoriesLightbulbs.setClickable(false);
            this.binding.dialogEditWoAccessoriesBrakes.setClickable(false);
            this.binding.dialogEditWoAccessoriesMudguard.setClickable(false);
            this.binding.dialogEditWoAccessoriesWinkers.setClickable(false);
            this.binding.dialogEditWoAccessoriesTools.setClickable(false);
            this.binding.dialogEditWoAccessoriesEmblems.setClickable(false);
            this.binding.dialogEditWoAccessoriesTires.setClickable(false);
            this.binding.dialogEditWoAccessoriesKey.setClickable(false);
            this.binding.dialogEditWoAccessoriesCommands.setClickable(false);
            this.binding.dialogEditWoAccessoriesHoses.setClickable(false);
            this.binding.dialogEditWoAccessoriesHandlebar.setClickable(false);
            this.binding.dialogEditWoAccessoriesSeat.setClickable(false);
            this.binding.dialogEditWoAccessoriesCatEyes.setClickable(false);
            this.binding.dialogEditWoAccessoriesLevers.setClickable(false);
            this.binding.dialogEditWoAccessoriesRacks.setClickable(false);
            this.binding.dialogEditWoAccessoriesPaint.setClickable(false);
            this.binding.dialogEditWoAccessoriesToolHolder.setClickable(false);
            this.binding.dialogEditWoAccessoriesHandGrips.setClickable(false);
            this.binding.dialogEditWoAccessoriesRadiator.setClickable(false);
            this.binding.dialogEditWoAccessoriesRectifier.setClickable(false);
            this.binding.dialogEditWoAccessoriesRearviewMirrors.setClickable(false);
            this.binding.dialogEditWoAccessoriesElectricSystem.setClickable(false);
            this.binding.dialogEditWoAccessoriesLicensePlateSupport.setClickable(false);
            this.binding.dialogEditWoAccessoriesChainCover.setClickable(false);
            this.binding.dialogEditWoAccessoriesFuelCap.setClickable(false);
            this.binding.dialogEditWoAccessoriesValveCover.setClickable(false);
            this.binding.dialogEditWoAccessoriesEngineCovers.setClickable(false);
            this.binding.dialogEditWoAccessoriesSideCovers.setClickable(false);
            this.binding.dialogEditWoAccessoriesUpholstery.setClickable(false);
            this.binding.dialogEditWoAccessoriesTelescopicFork.setClickable(false);
            this.binding.dialogEditWoAccessoriesChainTensioner.setClickable(false);
            this.binding.dialogEditWoAccessoriesTransmission.setClickable(false);
            this.binding.dialogEditWoAccessoriesSpeedometer.setClickable(false);

            // disable preventive maintenance
            this.binding.dialogEditWoPrevMaintOil.setClickable(false);
            this.binding.dialogEditWoPrevMaintEngineTuning.setClickable(false);
            this.binding.dialogEditWoPrevMaintBoltAdjustment.setClickable(false);
            this.binding.dialogEditWoPrevMaintChangeSparkPlugs.setClickable(false);
            this.binding.dialogEditWoPrevMaintCarburetion.setClickable(false);
            this.binding.dialogEditWoPrevMaintLubrication.setClickable(false);
            this.binding.dialogEditWoPrevMaintFusesRevision.setClickable(false);
            this.binding.dialogEditWoPrevMaintTransmissionCleaning.setClickable(false);
            this.binding.dialogEditWoPrevMaintTirePressure.setClickable(false);
            this.binding.dialogEditWoPrevMaintBreakAdjustment.setClickable(false);

            // disable corrective maintenance
            this.binding.dialogEditWoCorrMaintPistonRings.setClickable(false);
            this.binding.dialogEditWoCorrMaintGearbox.setClickable(false);
            this.binding.dialogEditWoCorrMaintEngineHead.setClickable(false);
            this.binding.dialogEditWoCorrMaintClutchPlates.setClickable(false);
            this.binding.dialogEditWoCorrMaintStarter.setClickable(false);
            this.binding.dialogEditWoCorrMaintPiston.setClickable(false);
            this.binding.dialogEditWoCorrMaintClutchPress.setClickable(false);
            this.binding.dialogEditWoCorrMaintGrinding.setClickable(false);
            this.binding.dialogEditWoCorrMaintSolenoid.setClickable(false);
            this.binding.dialogEditWoCorrMaintTransmission.setClickable(false);
            this.binding.dialogEditWoCorrMaintValves.setClickable(false);
        }
    }

    private void submitForm(boolean isEditionMode) {
        if (this.isFormValid()) {
            // save form
            if (!isEditionMode) {
                this.submitNewForm();
            } else {
                this.submitEditionForm();
            }

            // show SnackBar
            if (!isEditionMode) {
                this.mWorkOrderFormDialogCallback.onWorkOrderCreated(getString(R.string.dialog_edit_work_order_creation_success));
            } else {
                this.mWorkOrderFormDialogCallback.onWorkOrderEdited(getString(R.string.dialog_edit_work_order_edition_success));
            }

            // go back to activity caller
            dismiss();
        } else {
            Snackbar.make(this.binding.dialogEditWoMain, getString(R.string.dialog_edit_work_order_err_form), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void submitNewForm() {
        WorkOrderForm newWorkOrderForm = constructWorkOrderForm();
        this.mMotorcycleWorkOrderFormsCollReference.add(newWorkOrderForm);
    }

    private void submitEditionForm() {
        WorkOrderForm workOrderForm = constructWorkOrderForm();
        this.mMotorcycleWorkOrderFormsCollReference.document(this.mWorkOrderFormId).set(workOrderForm);
    }

    private WorkOrderForm constructWorkOrderForm() {
        int initialCompression = this.binding.dialogEditWoInitialCompression.getText().length() > 0 ?
                Integer.parseInt(this.binding.dialogEditWoInitialCompression.getText().toString()) : -1;
        int finalCompression = this.binding.dialogEditWoFinalCompression.getText().length() > 0 ?
                Integer.parseInt(this.binding.dialogEditWoFinalCompression.getText().toString()) : -1;
        int totalCost = this.isEditionMode() ? this.mWorkOrderForm.getTotalCost() : 0;
        int imagesCounter = this.isEditionMode() ? this.mWorkOrderForm.getImageCounter() : 0;
        Date endDate = this.isEditionMode() ? this.mWorkOrderForm.getEndDate() : null;

        return new WorkOrderForm(
                // header resources
                this.selectedStartDate.getTime(),
                endDate,
                this.binding.dialogEditWoIssue.getText().toString().trim(),
                initialCompression,
                finalCompression,
                this.binding.dialogEditWoOthers.getText().toString().trim(),
                totalCost,
                imagesCounter,

                // accessories resources
                this.binding.dialogEditWoAccessoriesRims.isChecked(),
                this.binding.dialogEditWoAccessoriesIgnition.isChecked(),
                this.binding.dialogEditWoAccessoriesBattery.isChecked(),
                this.binding.dialogEditWoAccessoriesHorn.isChecked(),
                this.binding.dialogEditWoAccessoriesCentralBipod.isChecked(),
                this.binding.dialogEditWoAccessoriesCaps.isChecked(),
                this.binding.dialogEditWoAccessoriesCdi.isChecked(),
                this.binding.dialogEditWoAccessoriesIgnitionSwitch.isChecked(),
                this.binding.dialogEditWoAccessoriesClutchCable.isChecked(),
                this.binding.dialogEditWoAccessoriesExhaust.isChecked(),
                this.binding.dialogEditWoAccessoriesHeadlight.isChecked(),
                this.binding.dialogEditWoAccessoriesFilters.isChecked(),
                this.binding.dialogEditWoAccessoriesLightbulbs.isChecked(),
                this.binding.dialogEditWoAccessoriesBrakes.isChecked(),
                this.binding.dialogEditWoAccessoriesMudguard.isChecked(),
                this.binding.dialogEditWoAccessoriesWinkers.isChecked(),
                this.binding.dialogEditWoAccessoriesTools.isChecked(),
                this.binding.dialogEditWoAccessoriesEmblems.isChecked(),
                this.binding.dialogEditWoAccessoriesTires.isChecked(),
                this.binding.dialogEditWoAccessoriesKey.isChecked(),
                this.binding.dialogEditWoAccessoriesCommands.isChecked(),
                this.binding.dialogEditWoAccessoriesHoses.isChecked(),
                this.binding.dialogEditWoAccessoriesHandlebar.isChecked(),
                this.binding.dialogEditWoAccessoriesSeat.isChecked(),
                this.binding.dialogEditWoAccessoriesCatEyes.isChecked(),
                this.binding.dialogEditWoAccessoriesLevers.isChecked(),
                this.binding.dialogEditWoAccessoriesRacks.isChecked(),
                this.binding.dialogEditWoAccessoriesPaint.isChecked(),
                this.binding.dialogEditWoAccessoriesToolHolder.isChecked(),
                this.binding.dialogEditWoAccessoriesHandGrips.isChecked(),
                this.binding.dialogEditWoAccessoriesRadiator.isChecked(),
                this.binding.dialogEditWoAccessoriesRectifier.isChecked(),
                this.binding.dialogEditWoAccessoriesRearviewMirrors.isChecked(),
                this.binding.dialogEditWoAccessoriesElectricSystem.isChecked(),
                this.binding.dialogEditWoAccessoriesLicensePlateSupport.isChecked(),
                this.binding.dialogEditWoAccessoriesChainCover.isChecked(),
                this.binding.dialogEditWoAccessoriesFuelCap.isChecked(),
                this.binding.dialogEditWoAccessoriesValveCover.isChecked(),
                this.binding.dialogEditWoAccessoriesEngineCovers.isChecked(),
                this.binding.dialogEditWoAccessoriesSideCovers.isChecked(),
                this.binding.dialogEditWoAccessoriesUpholstery.isChecked(),
                this.binding.dialogEditWoAccessoriesTelescopicFork.isChecked(),
                this.binding.dialogEditWoAccessoriesChainTensioner.isChecked(),
                this.binding.dialogEditWoAccessoriesTransmission.isChecked(),
                this.binding.dialogEditWoAccessoriesSpeedometer.isChecked(),

                // preventive maintenance
                this.binding.dialogEditWoPrevMaintOil.isChecked(),
                this.binding.dialogEditWoPrevMaintEngineTuning.isChecked(),
                this.binding.dialogEditWoPrevMaintBoltAdjustment.isChecked(),
                this.binding.dialogEditWoPrevMaintChangeSparkPlugs.isChecked(),
                this.binding.dialogEditWoPrevMaintCarburetion.isChecked(),
                this.binding.dialogEditWoPrevMaintLubrication.isChecked(),
                this.binding.dialogEditWoPrevMaintFusesRevision.isChecked(),
                this.binding.dialogEditWoPrevMaintTransmissionCleaning.isChecked(),
                this.binding.dialogEditWoPrevMaintTirePressure.isChecked(),
                this.binding.dialogEditWoPrevMaintBreakAdjustment.isChecked(),

                // corrective maintenance
                this.binding.dialogEditWoCorrMaintPistonRings.isChecked(),
                this.binding.dialogEditWoCorrMaintGearbox.isChecked(),
                this.binding.dialogEditWoCorrMaintEngineHead.isChecked(),
                this.binding.dialogEditWoCorrMaintClutchPlates.isChecked(),
                this.binding.dialogEditWoCorrMaintStarter.isChecked(),
                this.binding.dialogEditWoCorrMaintPiston.isChecked(),
                this.binding.dialogEditWoCorrMaintClutchPress.isChecked(),
                this.binding.dialogEditWoCorrMaintGrinding.isChecked(),
                this.binding.dialogEditWoCorrMaintSolenoid.isChecked(),
                this.binding.dialogEditWoCorrMaintTransmission.isChecked(),
                this.binding.dialogEditWoCorrMaintValves.isChecked());
    }

    private boolean isFormValid() {
        int errorCounter = 0;

        if (TextUtils.isEmpty(this.binding.dialogEditWoStartDate.getText().toString())) {
            this.binding.dialogEditWoInputLayoutWoStartDate.setError(getString(R.string.dialog_edit_work_order_err_start_date));
            errorCounter++;
        } else {
            this.binding.dialogEditWoInputLayoutWoStartDate.setError(null);
        }

        if (TextUtils.isEmpty(this.binding.dialogEditWoStartTime.getText().toString())) {
            this.binding.dialogEditWoInputLayoutWoStartTime.setError(getString(R.string.dialog_edit_work_order_err_start_time));
            errorCounter++;
        } else {
            this.binding.dialogEditWoInputLayoutWoStartTime.setError(null);
        }

        if (TextUtils.isEmpty(this.binding.dialogEditWoIssue.getText().toString())) {
            this.binding.dialogEditWoInputLayoutWoIssue.setError(getString(R.string.dialog_edit_work_order_err_issue));
            errorCounter++;
        } else {
            this.binding.dialogEditWoInputLayoutWoIssue.setError(null);
        }

        return errorCounter == 0;
    }

    private void showDatePickerDialog() {
        DialogFragment newFragment = DatePickerFragment.newInstance(this.selectedStartDate, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                if (selectedStartDate == null) {
                    selectedStartDate = Calendar.getInstance();
                }

                selectedStartDate.set(Calendar.YEAR, year);
                selectedStartDate.set(Calendar.MONTH, month);
                selectedStartDate.set(Calendar.DAY_OF_MONTH, day);

                binding.dialogEditWoStartDate.setText(DateUtils.getFormattedDate(selectedStartDate.getTime()));
            }
        });

        newFragment.show(requireActivity().getSupportFragmentManager(), "datePicker");
    }

    private void showTimePickerDialog() {
        DialogFragment newFragment = TimePickerFragment.newInstance(this.selectedStartDate, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (selectedStartDate == null) {
                    selectedStartDate = Calendar.getInstance();
                }

                selectedStartDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedStartDate.set(Calendar.MINUTE, minute);
                selectedStartDate.set(Calendar.SECOND, 0);
                selectedStartDate.set(Calendar.MILLISECOND, 0);

                binding.dialogEditWoStartTime.setText(DateUtils.getFormattedTime(selectedStartDate.getTime()));
            }
        });

        newFragment.show(requireActivity().getSupportFragmentManager(), "timePicker");
    }

    private boolean isEditionMode() {
        return this.mWorkOrderFormId != null && this.mWorkOrderForm != null;
    }
}
