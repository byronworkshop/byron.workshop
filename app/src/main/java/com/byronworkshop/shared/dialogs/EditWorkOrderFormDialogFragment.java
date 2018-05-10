package com.byronworkshop.shared.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.byronworkshop.R;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.utils.DatePickerFragment;
import com.byronworkshop.utils.DateUtils;
import com.byronworkshop.utils.TimePickerFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class EditWorkOrderFormDialogFragment extends DialogFragment {

    // firebase analytics events
    private static final String EVENT_WORK_ORDER_FORM_CREATION = "create_work_order";
    private static final String EVENT_WORK_ORDER_FORM_EDITION = "edit_work_order";

    // dialogs finals
    private static final String TAG_EDIT_WORK_ORDER_FORM_DIALOG = "wo_form_dialog";
    private static final String KEY_SELECTED_DATE = "selected_date";

    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    // arg on edition
    private String mWorkOrderFormId;
    private WorkOrderForm mWorkOrderForm;

    // resources
    private Calendar selectedDate;
    private TextInputLayout tiDate;
    private EditText etDate;
    private TextInputLayout tiTime;
    private EditText etTime;
    private TextInputLayout tiInitialCompression;
    private EditText etInitialCompression;
    private EditText etFinalCompression;
    private TextInputLayout tiIssue;
    private EditText etIssue;
    private EditText etOthers;

    // accessories
    private CheckBox cbIgnition;               // arranque
    private CheckBox cbBattery;                // batería
    private CheckBox cbCentralBipod;           // bípode central
    private CheckBox cbHorn;                   // bocina
    private CheckBox cbCdi;                    // CDI
    private CheckBox cbHeadlight;              // farol
    private CheckBox cbLightBulbs;             // focos
    private CheckBox cbMudguard;               // guarda barros
    private CheckBox cbWinkers;                // guiñadores
    private CheckBox cbPaint;                  // pintura
    private CheckBox cbHandGrips;              // puños
    private CheckBox cbRectifier;              // rectificador
    private CheckBox cbRearviewMirrors;        // retrovisores
    private CheckBox cbChainCover;             // tapa cadena
    private CheckBox cbTelescopicFork;         // telescopios

    private CheckBox cbCaps;                   // capuchones
    private CheckBox cbIgnitionSwitch;         // chapa de contacto
    private CheckBox cbClutchCable;            // chicotillo
    private CheckBox cbExhaust;                // escape
    private CheckBox cbTires;                  // llantas
    private CheckBox cbKey;                    // llave
    private CheckBox cbHandlebar;              // manillar
    private CheckBox cbSeat;                   // montura
    private CheckBox cbLevers;                 // palancas
    private CheckBox cbRacks;                  // parrillas
    private CheckBox cbRadiator;               // radiador
    private CheckBox cbLicensePlateSupport;    // soporte de placa
    private CheckBox cbValveCover;             // tapa válvulas
    private CheckBox cbUpholstery;             // tapiz
    private CheckBox cbChainTensioner;         // tesador de cadena

    private CheckBox cbRims;                   // aros
    private CheckBox cbFilters;                // filtros
    private CheckBox cbBrakes;                 // frenos
    private CheckBox cbTools;                  // herramientas
    private CheckBox cbEmblems;                // insignias
    private CheckBox cbCommands;               // mandos
    private CheckBox cbHoses;                  // mangueras
    private CheckBox cbCatEyes;                // ojos de gato ****
    private CheckBox cbToolHolder;             // porta herramientas
    private CheckBox cbElectricSystem;         // sistema eléctrico
    private CheckBox cbFuelCap;                // tapa de combustible
    private CheckBox cbEngineCovers;           // tapas de motor
    private CheckBox cbSideCovers;             // tapas laterales
    private CheckBox cbTransmission;           // transmisión
    private CheckBox cbSpeedometer;            // velocímetro

    // preventive maintenance
    private CheckBox cbOil;                    // aceite
    private CheckBox cbLubrication;            // engrase
    private CheckBox cbBoltAdjustment;         // ajuste de pernos
    private CheckBox cbEngineTuning;           // afinado de motor
    private CheckBox cbCarburetion;            // carburación
    private CheckBox cbChangeSparkPlugs;       // cambio de bugias
    private CheckBox cbTransmissionCleaning;   // limpieza de transmisión
    private CheckBox cbBreakAdjustment;         // reajuste de frenos
    private CheckBox cbTirePressure;           // presión de llantas
    private CheckBox cbFusesRevision;          // revisión de fusibles

    // corrective maintenance
    private CheckBox cbValves;                 // válvulas
    private CheckBox cbPistonRings;            // anillas
    private CheckBox cbPiston;                 // pistón
    private CheckBox cbTransmissionCorrection; // transmisión
    private CheckBox cbClutchPlates;           // discos de embrague
    private CheckBox cbEngineHead;             // culata
    private CheckBox cbClutchPress;            // prensa de embrague
    private CheckBox cbStarter;                // motor de arranque
    private CheckBox cbSolenoid;               // solenoide
    private CheckBox cbGearBox;                // caja de velocidades
    private CheckBox cbGrinding;               // rectificado

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

        outState.putSerializable(KEY_SELECTED_DATE, this.selectedDate);
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

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SELECTED_DATE)) {
            this.selectedDate = (Calendar) savedInstanceState.getSerializable(KEY_SELECTED_DATE);
        }

        this.mMotorcycleWorkOrderFormsCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("forms");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // get the layout inflater
        LayoutInflater inflater = mOwnerActivity.getLayoutInflater();
        View mDialogView = inflater.inflate(R.layout.dialog_edit_work_order, null);

        // header resources
        this.tiDate = mDialogView.findViewById(R.id.dialog_edit_wo_input_layout_wo_date);
        this.etDate = mDialogView.findViewById(R.id.dialog_edit_wo_date);
        this.tiTime = mDialogView.findViewById(R.id.dialog_edit_wo_input_layout_wo_time);
        this.etTime = mDialogView.findViewById(R.id.dialog_edit_wo_time);
        this.tiIssue = mDialogView.findViewById(R.id.dialog_edit_wo_input_layout_wo_issue);
        this.etIssue = mDialogView.findViewById(R.id.dialog_edit_wo_issue);
        this.tiInitialCompression = mDialogView.findViewById(R.id.dialog_edit_wo_input_layout_wo_initial_compression);
        this.etInitialCompression = mDialogView.findViewById(R.id.dialog_edit_wo_initial_compression);
        this.etFinalCompression = mDialogView.findViewById(R.id.dialog_edit_wo_final_compression);
        this.etOthers = mDialogView.findViewById(R.id.dialog_edit_wo_others);

        // accessories resources
        this.cbIgnition = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_ignition);
        this.cbBattery = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_battery);
        this.cbCentralBipod = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_central_bipod);
        this.cbHorn = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_horn);
        this.cbCdi = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_cdi);
        this.cbHeadlight = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_headlight);
        this.cbLightBulbs = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_lightbulbs);
        this.cbMudguard = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_mudguard);
        this.cbWinkers = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_winkers);
        this.cbPaint = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_paint);
        this.cbHandGrips = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_handgrips);
        this.cbRectifier = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_rectifier);
        this.cbRearviewMirrors = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_rearview_mirrors);
        this.cbChainCover = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_chaincover);
        this.cbTelescopicFork = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_telescopic_fork);

        this.cbCaps = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_caps);
        this.cbIgnitionSwitch = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_ignition_switch);
        this.cbClutchCable = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_clutch_cable);
        this.cbExhaust = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_exhaust);
        this.cbTires = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_tires);
        this.cbKey = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_key);
        this.cbHandlebar = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_handlebar);
        this.cbSeat = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_seat);
        this.cbLevers = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_levers);
        this.cbRacks = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_racks);
        this.cbRadiator = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_radiator);
        this.cbLicensePlateSupport = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_license_plate_support);
        this.cbValveCover = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_valve_cover);
        this.cbUpholstery = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_upholstery);
        this.cbChainTensioner = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_chain_tensioner);

        this.cbRims = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_rims);
        this.cbFilters = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_filters);
        this.cbBrakes = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_brakes);
        this.cbTools = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_tools);
        this.cbEmblems = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_emblems);
        this.cbCommands = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_commands);
        this.cbHoses = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_hoses);
        this.cbCatEyes = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_cat_eyes);
        this.cbToolHolder = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_tool_holder);
        this.cbElectricSystem = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_electric_system);
        this.cbFuelCap = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_fuel_cap);
        this.cbEngineCovers = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_engine_covers);
        this.cbSideCovers = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_side_covers);
        this.cbTransmission = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_transmission);
        this.cbSpeedometer = mDialogView.findViewById(R.id.dialog_edit_wo_accessories_speedometer);

        // preventive maintenance
        this.cbOil = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_oil);
        this.cbLubrication = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_lubrication);
        this.cbBoltAdjustment = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_bolt_adjustment);
        this.cbEngineTuning = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_engine_tuning);
        this.cbCarburetion = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_carburetion);
        this.cbChangeSparkPlugs = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_change_spark_plugs);
        this.cbTransmissionCleaning = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_transmission_cleaning);
        this.cbBreakAdjustment = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_break_adjustment);
        this.cbTirePressure = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_tire_pressure);
        this.cbFusesRevision = mDialogView.findViewById(R.id.dialog_edit_wo_prev_maint_fuses_revision);

        // corrective maintenance
        this.cbValves = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_valves);
        this.cbPistonRings = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_piston_rings);
        this.cbPiston = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_piston);
        this.cbTransmissionCorrection = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_transmission);
        this.cbClutchPlates = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_clutch_plates);
        this.cbEngineHead = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_engine_head);
        this.cbClutchPress = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_clutch_press);
        this.cbStarter = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_starter);
        this.cbSolenoid = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_solenoid);
        this.cbGearBox = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_gearbox);
        this.cbGrinding = mDialogView.findViewById(R.id.dialog_edit_wo_corr_maint_grinding);

        // load form for edition
        if (this.isEditionMode()) {
            this.setEditionForm();
        } else {
            this.setNewForm();
        }

        // date picker
        this.etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
        this.etTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePickerDialog();
            }
        });

        // create dialog
        String title = !this.isEditionMode() ? getString(R.string.dialog_edit_work_order_title_new)
                : getString(R.string.dialog_edit_work_order_title_edit);
        AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity);
        builder.setTitle(title)
                .setView(mDialogView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_edit_work_order_save), null)
                .setNegativeButton(getString(R.string.dialog_edit_work_order_cancel), null);


        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        submitForm(isEditionMode());
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
    private void setNewForm() {
        // set default date and time
        this.selectedDate = Calendar.getInstance();
        this.etDate.setText(DateUtils.getFormattedDate(this.selectedDate));
        this.etTime.setText(DateUtils.getFormattedTime(this.selectedDate));
    }

    private void setEditionForm() {
        // set date
        Calendar savedDate;
        if (this.selectedDate == null) {
            savedDate = Calendar.getInstance();
            savedDate.setTimeInMillis(this.mWorkOrderForm.getDate());
        } else {
            savedDate = this.selectedDate;
        }

        String dateStr = DateUtils.getFormattedDate(savedDate);
        String timeStr = DateUtils.getFormattedTime(savedDate);
        this.selectedDate = savedDate;
        this.etDate.setText(dateStr);
        this.etTime.setText(timeStr);

        // header resources
        this.etDate.setText(dateStr);
        this.etIssue.setText(this.mWorkOrderForm.getIssue());
        this.etInitialCompression.setText(String.valueOf(this.mWorkOrderForm.getInitialCompLevel()));
        this.etFinalCompression.setText(this.mWorkOrderForm.getFinalCompLevel() != -1 ? String.valueOf(this.mWorkOrderForm.getFinalCompLevel()) : "");
        this.etOthers.setText(this.mWorkOrderForm.getOthers());

        // accessories resources
        this.cbIgnition.setChecked(this.mWorkOrderForm.isIgnition());
        this.cbBattery.setChecked(this.mWorkOrderForm.isBattery());
        this.cbCentralBipod.setChecked(this.mWorkOrderForm.isCentralBipod());
        this.cbHorn.setChecked(this.mWorkOrderForm.isHorn());
        this.cbCdi.setChecked(this.mWorkOrderForm.isCdi());
        this.cbHeadlight.setChecked(this.mWorkOrderForm.isHeadlight());
        this.cbLightBulbs.setChecked(this.mWorkOrderForm.isLightBulbs());
        this.cbMudguard.setChecked(this.mWorkOrderForm.isMudguard());
        this.cbWinkers.setChecked(this.mWorkOrderForm.isWinkers());
        this.cbPaint.setChecked(this.mWorkOrderForm.isPaint());
        this.cbHandGrips.setChecked(this.mWorkOrderForm.isHandGrips());
        this.cbRectifier.setChecked(this.mWorkOrderForm.isRectifier());
        this.cbRearviewMirrors.setChecked(this.mWorkOrderForm.isRearviewMirrors());
        this.cbChainCover.setChecked(this.mWorkOrderForm.isChainCover());
        this.cbTelescopicFork.setChecked(this.mWorkOrderForm.isTelescopicFork());

        this.cbCaps.setChecked(this.mWorkOrderForm.isCaps());
        this.cbIgnitionSwitch.setChecked(this.mWorkOrderForm.isIgnitionSwitch());
        this.cbClutchCable.setChecked(this.mWorkOrderForm.isClutchCable());
        this.cbExhaust.setChecked(this.mWorkOrderForm.isExhaust());
        this.cbTires.setChecked(this.mWorkOrderForm.isTires());
        this.cbKey.setChecked(this.mWorkOrderForm.isKey());
        this.cbHandlebar.setChecked(this.mWorkOrderForm.isHandlebar());
        this.cbSeat.setChecked(this.mWorkOrderForm.isSeat());
        this.cbLevers.setChecked(this.mWorkOrderForm.isLevers());
        this.cbRacks.setChecked(this.mWorkOrderForm.isRacks());
        this.cbRadiator.setChecked(this.mWorkOrderForm.isRadiator());
        this.cbLicensePlateSupport.setChecked(this.mWorkOrderForm.isLicensePlateSupport());
        this.cbValveCover.setChecked(this.mWorkOrderForm.isValveCover());
        this.cbUpholstery.setChecked(this.mWorkOrderForm.isUpholstery());
        this.cbChainTensioner.setChecked(this.mWorkOrderForm.isChainTensioner());

        this.cbRims.setChecked(this.mWorkOrderForm.isRims());
        this.cbFilters.setChecked(this.mWorkOrderForm.isFilters());
        this.cbBrakes.setChecked(this.mWorkOrderForm.isBrakes());
        this.cbTools.setChecked(this.mWorkOrderForm.isTools());
        this.cbEmblems.setChecked(this.mWorkOrderForm.isEmblems());
        this.cbCommands.setChecked(this.mWorkOrderForm.isCommands());
        this.cbHoses.setChecked(this.mWorkOrderForm.isHoses());
        this.cbCatEyes.setChecked(this.mWorkOrderForm.isCatEyes());
        this.cbToolHolder.setChecked(this.mWorkOrderForm.isToolHolder());
        this.cbElectricSystem.setChecked(this.mWorkOrderForm.isElectricSystem());
        this.cbFuelCap.setChecked(this.mWorkOrderForm.isFuelCap());
        this.cbEngineCovers.setChecked(this.mWorkOrderForm.isEngineCovers());
        this.cbSideCovers.setChecked(this.mWorkOrderForm.isSideCovers());
        this.cbTransmission.setChecked(this.mWorkOrderForm.isTransmission());
        this.cbSpeedometer.setChecked(this.mWorkOrderForm.isSpeedometer());

        // preventive maintenance
        this.cbOil.setChecked(this.mWorkOrderForm.isOil());
        this.cbLubrication.setChecked(this.mWorkOrderForm.isLubrication());
        this.cbBoltAdjustment.setChecked(this.mWorkOrderForm.isBoltAdjustment());
        this.cbEngineTuning.setChecked(this.mWorkOrderForm.isEngineTuning());
        this.cbCarburetion.setChecked(this.mWorkOrderForm.isCarburetion());
        this.cbChangeSparkPlugs.setChecked(this.mWorkOrderForm.isChangeSparkPlugs());
        this.cbTransmissionCleaning.setChecked(this.mWorkOrderForm.isTransmissionCleaning());
        this.cbBreakAdjustment.setChecked(this.mWorkOrderForm.isBreakAdjustment());
        this.cbTirePressure.setChecked(this.mWorkOrderForm.isTirePressure());
        this.cbFusesRevision.setChecked(this.mWorkOrderForm.isFusesRevision());

        // corrective maintenance
        this.cbValves.setChecked(this.mWorkOrderForm.isValves());
        this.cbPistonRings.setChecked(this.mWorkOrderForm.isPistonRings());
        this.cbPiston.setChecked(this.mWorkOrderForm.isPiston());
        this.cbTransmissionCorrection.setChecked(this.mWorkOrderForm.isTransmissionCorrection());
        this.cbClutchPlates.setChecked(this.mWorkOrderForm.isClutchPlates());
        this.cbEngineHead.setChecked(this.mWorkOrderForm.isEngineHead());
        this.cbClutchPress.setChecked(this.mWorkOrderForm.isClutchPress());
        this.cbStarter.setChecked(this.mWorkOrderForm.isStarter());
        this.cbSolenoid.setChecked(this.mWorkOrderForm.isSolenoid());
        this.cbGearBox.setChecked(this.mWorkOrderForm.isGearBox());
        this.cbGrinding.setChecked(this.mWorkOrderForm.isGrinding());
    }

    private void submitForm(boolean isEditionMode) {
        if (this.isFormValid()) {
            if (!isEditionMode) {
                this.submitNewForm();
            } else {
                this.submitEditionForm();
            }
        }
    }

    private void submitNewForm() {
        WorkOrderForm newWorkOrderForm = constructWorkOrderForm();
        this.mMotorcycleWorkOrderFormsCollReference.add(newWorkOrderForm);
        dismiss();
    }

    private void submitEditionForm() {
        WorkOrderForm workOrderForm = constructWorkOrderForm();
        this.mMotorcycleWorkOrderFormsCollReference.document(this.mWorkOrderFormId).set(workOrderForm);
        dismiss();
    }

    private WorkOrderForm constructWorkOrderForm() {
        int initialCompression = Integer.parseInt(this.etInitialCompression.getText().toString());
        int finalCompression = this.etFinalCompression.getText().length() > 0 ?
                Integer.parseInt(this.etFinalCompression.getText().toString()) : -1;
        int totalCost = this.isEditionMode() ? this.mWorkOrderForm.getTotalCost() : 0;
        int imagesCounter = this.isEditionMode() ? this.mWorkOrderForm.getImageCounter() : 0;

        return new WorkOrderForm(
                // header resources
                this.selectedDate.getTimeInMillis(),
                this.etIssue.getText().toString().trim(),
                initialCompression,
                finalCompression,
                this.etOthers.getText().toString().trim(),
                totalCost,
                imagesCounter,

                // accessories resources
                this.cbIgnition.isChecked(),
                this.cbBattery.isChecked(),
                this.cbCentralBipod.isChecked(),
                this.cbHorn.isChecked(),
                this.cbCdi.isChecked(),
                this.cbHeadlight.isChecked(),
                this.cbLightBulbs.isChecked(),
                this.cbMudguard.isChecked(),
                this.cbWinkers.isChecked(),
                this.cbPaint.isChecked(),
                this.cbHandGrips.isChecked(),
                this.cbRectifier.isChecked(),
                this.cbRearviewMirrors.isChecked(),
                this.cbChainCover.isChecked(),
                this.cbTelescopicFork.isChecked(),

                this.cbCaps.isChecked(),
                this.cbIgnitionSwitch.isChecked(),
                this.cbClutchCable.isChecked(),
                this.cbExhaust.isChecked(),
                this.cbTires.isChecked(),
                this.cbKey.isChecked(),
                this.cbHandlebar.isChecked(),
                this.cbSeat.isChecked(),
                this.cbLevers.isChecked(),
                this.cbRacks.isChecked(),
                this.cbRadiator.isChecked(),
                this.cbLicensePlateSupport.isChecked(),
                this.cbValveCover.isChecked(),
                this.cbUpholstery.isChecked(),
                this.cbChainTensioner.isChecked(),

                this.cbRims.isChecked(),
                this.cbFilters.isChecked(),
                this.cbBrakes.isChecked(),
                this.cbTools.isChecked(),
                this.cbEmblems.isChecked(),
                this.cbCommands.isChecked(),
                this.cbHoses.isChecked(),
                this.cbCatEyes.isChecked(),
                this.cbToolHolder.isChecked(),
                this.cbElectricSystem.isChecked(),
                this.cbFuelCap.isChecked(),
                this.cbEngineCovers.isChecked(),
                this.cbSideCovers.isChecked(),
                this.cbTransmission.isChecked(),
                this.cbSpeedometer.isChecked(),

                // preventive maintenance
                this.cbOil.isChecked(),
                this.cbLubrication.isChecked(),
                this.cbBoltAdjustment.isChecked(),
                this.cbEngineTuning.isChecked(),
                this.cbCarburetion.isChecked(),
                this.cbChangeSparkPlugs.isChecked(),
                this.cbTransmissionCleaning.isChecked(),
                this.cbBreakAdjustment.isChecked(),
                this.cbTirePressure.isChecked(),
                this.cbFusesRevision.isChecked(),

                // corrective maintenance
                this.cbValves.isChecked(),
                this.cbPistonRings.isChecked(),
                this.cbPiston.isChecked(),
                this.cbTransmissionCorrection.isChecked(),
                this.cbClutchPlates.isChecked(),
                this.cbEngineHead.isChecked(),
                this.cbClutchPress.isChecked(),
                this.cbStarter.isChecked(),
                this.cbSolenoid.isChecked(),
                this.cbGearBox.isChecked(),
                this.cbGrinding.isChecked()
        );
    }

    private boolean isFormValid() {
        int errorCounter = 0;

        if (TextUtils.isEmpty(this.etDate.getText().toString())) {
            this.tiDate.setError(getString(R.string.dialog_edit_work_order_err_date));
            errorCounter++;
        } else {
            this.tiDate.setError(null);
        }

        if (TextUtils.isEmpty(this.etTime.getText().toString())) {
            this.tiTime.setError(getString(R.string.dialog_edit_work_order_err_time));
            errorCounter++;
        } else {
            this.tiTime.setError(null);
        }

        if (TextUtils.isEmpty(this.etIssue.getText().toString())) {
            this.tiIssue.setError(getString(R.string.dialog_edit_work_order_err_issue));
            errorCounter++;
        } else {
            this.tiIssue.setError(null);
        }

        if (TextUtils.isEmpty(this.etInitialCompression.getText().toString())) {
            this.tiInitialCompression.setError(getString(R.string.dialog_edit_work_order_err_initial_compression));
            errorCounter++;
        } else {
            this.tiInitialCompression.setError(null);
        }

        return errorCounter == 0;
    }

    private void showDatePickerDialog() {
        DialogFragment newFragment = DatePickerFragment.newInstance(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                if (selectedDate == null) {
                    selectedDate = Calendar.getInstance();
                }

                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, day);

                etDate.setText(DateUtils.getFormattedDate(selectedDate));
            }
        });

        newFragment.show(requireActivity().getSupportFragmentManager(), "datePicker");
    }

    private void showTimePickerDialog() {
        DialogFragment newFragment = TimePickerFragment.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (selectedDate == null) {
                    selectedDate = Calendar.getInstance();
                }

                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);

                etTime.setText(DateUtils.getFormattedTime(selectedDate));
            }
        });

        newFragment.show(requireActivity().getSupportFragmentManager(), "timePicker");
    }

    private boolean isEditionMode() {
        return this.mWorkOrderFormId != null && this.mWorkOrderForm != null;
    }
}
