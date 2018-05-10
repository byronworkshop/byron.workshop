package com.byronworkshop.shared.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.shared.pojo.Image;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.ui.mainactivity.adapter.pojo.MotorcycleMetadata;
import com.byronworkshop.utils.BitmapUtils;
import com.byronworkshop.utils.ConnectionUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class EditMotorcycleDialogFragment extends DialogFragment {
    // firebase analytics events
    private static final String EVENT_MOTORCYCLE_CREATION = "create_motorcycle";
    private static final String EVENT_MOTORCYCLE_EDITION = "edit_motorcycle";

    // dialog finals
    private static final String TAG_EDIT_MOTORCYCLE_DIALOG = "edit_motorcycle_dialog";

    // arg mandatory
    private String mUid;
    // args on edition
    private String mMotorcycleId;
    private Motorcycle mMotorcycle;

    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 301;

    // camera vars
    private String mTmpAvatarPath;
    private String mAvatarPath;
    private View mDialogView;

    // const
    private static final String TMP_AVATAR_PATH = "TMP_AVATAR_PATH";
    private static final String AVATAR_PATH = "AVATAR_PATH";

    // resources
    private AppCompatButton btnPickImage;
    private LinearLayout llUploadProgressContainer;
    private TextView tvUploadProgressLabel;
    private ProgressBar pbUploadProgress;
    private TextView tvAvatarError;
    private ImageView ivAvatar;
    private ImageView ivAvatarPlaceHolder;
    private TextInputLayout tiBrand;
    private EditText etBrand;
    private TextInputLayout tiModel;
    private EditText etModel;
    private TextInputLayout tiType;
    private EditText etType;
    private TextInputLayout tiCc;
    private EditText etCc;
    private TextInputLayout ticolor;
    private EditText etColor;
    private TextInputLayout tiLicensePlateNumber;
    private EditText etLicensePlateNumber;

    // firebase
    private CollectionReference mMotorcyclesCollReference;
    private StorageReference mMotorcycleImagesStorageReference;

    public static void showEditMotorcycleDialog(
            @NonNull Context context,
            @NonNull FragmentManager fm,
            @NonNull String uId,
            @Nullable String motorcycleId,
            @Nullable Motorcycle motorcycle) {
        // replace same dialog fragments with a new one
        replaceAllWithNewInstance(fm, uId, motorcycleId, motorcycle);

        // log firebase analytics view item event
        logFirebaseViewItemEvent(context, !TextUtils.isEmpty(motorcycleId) && motorcycle != null);
    }

    private static void logFirebaseViewItemEvent(Context context, boolean isEditionMode) {
        FirebaseAnalytics.getInstance(context).logEvent(isEditionMode ?
                EVENT_MOTORCYCLE_EDITION : EVENT_MOTORCYCLE_CREATION, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, Motorcycle motorcycle) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editMotorcycleDialog = fm.findFragmentByTag(TAG_EDIT_MOTORCYCLE_DIALOG);
        if (editMotorcycleDialog != null) {
            ft.remove(editMotorcycleDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditMotorcycleDialogFragment.newInstance(uId, motorcycleId, motorcycle);
        df.show(fm, TAG_EDIT_MOTORCYCLE_DIALOG);
    }

    private static EditMotorcycleDialogFragment newInstance(String uId, String motorcycleId, Motorcycle motorcycle) {
        EditMotorcycleDialogFragment frag = new EditMotorcycleDialogFragment();

        Bundle args = new Bundle();
        args.putString("mUid", uId);
        args.putString("mMotorcycleId", motorcycleId);
        args.putSerializable("mMotorcycle", motorcycle);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TMP_AVATAR_PATH, this.mTmpAvatarPath);
        outState.putString(AVATAR_PATH, this.mAvatarPath);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // arg mandatory
            this.mUid = getArguments().getString("mUid");
            // args on edition
            this.mMotorcycleId = getArguments().getString("mMotorcycleId");
            this.mMotorcycle = (Motorcycle) getArguments().getSerializable("mMotorcycle");
        }

        if (savedInstanceState != null) {
            this.mTmpAvatarPath = savedInstanceState.getString(TMP_AVATAR_PATH);
            this.mAvatarPath = savedInstanceState.getString(AVATAR_PATH);
        }

        this.mMotorcyclesCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("motorcycles");
        this.mMotorcycleImagesStorageReference = FirebaseStorage.getInstance().getReference().child("users").child(this.mUid).child("motorcycles");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // Get the layout inflater
        LayoutInflater inflater = mOwnerActivity.getLayoutInflater();
        this.mDialogView = inflater.inflate(R.layout.dialog_edit_motorcycle, null);

        // set resources
        this.btnPickImage = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_button_pick_image);
        this.llUploadProgressContainer = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_upload_progress_container);
        this.tvUploadProgressLabel = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_upload_progress_label);
        this.pbUploadProgress = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_upload_progress);
        this.tvAvatarError = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_avatar_error);
        this.ivAvatar = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_avatar);
        this.ivAvatarPlaceHolder = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_avatar_placeholder);
        this.tiBrand = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_brand);
        this.etBrand = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_brand);
        this.tiModel = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_model);
        this.etModel = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_model);
        this.tiType = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_type);
        this.etType = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_type);
        this.tiCc = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_cc);
        this.etCc = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_cc);
        this.ticolor = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_color);
        this.etColor = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_color);
        this.tiLicensePlateNumber = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_input_layout_license_plate_number);
        this.etLicensePlateNumber = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_edit_license_plate_number);

        // load form for edition
        if (this.isEditionMode()) {
            this.setEditionForm();
        }

        // set avatar when dialog is re-created
        if (!TextUtils.isEmpty(this.mAvatarPath)) {
            this.setDialogAvatar(this.mAvatarPath);
        }

        // construct default alert dialog
        String title = this.isEditionMode() ? getString(R.string.dialog_edit_motorcycle_edit_title) : getString(R.string.dialog_edit_motorcycle_add_title);
        AlertDialog.Builder editMotorcycleDialogBuilder = new AlertDialog.Builder(mOwnerActivity)
                .setTitle(title)
                .setView(mDialogView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_edit_motorcycle_ok), null)
                .setNegativeButton(getString(R.string.dialog_edit_motorcycle_cancel), null);

        // add event to pick image
        this.btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity mOwnerActivity = requireActivity();

                // check writing permissions before launching the camera
                if (ContextCompat.checkSelfPermission(mOwnerActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
                } else {
                    launchPicker();
                }
            }
        });

        AlertDialog alertDialog = editMotorcycleDialogBuilder.create();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE_PERMISSION: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchPicker();
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.dialog_edit_motorcycle_writing_storage_denied), Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, requireActivity(), new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                Toast.makeText(requireContext(), getString(R.string.dialog_edit_motorcycle_avatar_image_picker_error), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imagesFiles, EasyImage.ImageSource source, int type) {
                File takenImage = imagesFiles.get(0);

                mTmpAvatarPath = takenImage.getAbsolutePath();
                processAndSetDialogAvatar();

                // remove empty file (probably a bug)
                deleteFromCache();
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                // Cancel handling, you might wanna remove cached image if it was canceled
                deleteFromCache();
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void deleteFromCache() {
        File cachedImageFile = EasyImage.lastlyTakenButCanceledPhoto(requireContext());
        if (cachedImageFile != null && cachedImageFile.getAbsolutePath().contains("cache/EasyImage")) {
            cachedImageFile.delete();
        }
    }

    private void launchPicker() {
        EasyImage.openChooserWithDocuments(this, getString(R.string.dialog_edit_motorcycle_avatar_image_picker_chooser_title), 0);
    }

    private void setDialogAvatar(String imagePath) {
        // set image into view
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        BitmapFactory.decodeFile(imagePath, bmOptions);

        // get bitmap
        Bitmap avatar = BitmapFactory.decodeFile(imagePath);

        // set avatar
        this.ivAvatar.setImageBitmap(avatar);
        this.showAvatar();
        this.scrollAvatarUp();
    }

    private void processAndSetDialogAvatar() {
        Activity mCaller = requireActivity();

        // resample saved image
        Bitmap avatar = BitmapUtils.resamplePic(this.mTmpAvatarPath);

        // save image by compressing the file
        this.mAvatarPath = BitmapUtils.saveImage(mCaller, avatar);

        // set avatar and remove avatar placeholder
        setDialogAvatar(this.mAvatarPath);

        // delete tmp file if comes from camera
        BitmapUtils.deleteImageFile(mCaller, this.mTmpAvatarPath);
    }

    private void scrollAvatarUp() {
        final FrameLayout editAvatarContainer = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_avatar_container);
        final ScrollView editScroller = this.mDialogView.findViewById(R.id.dialog_edit_motorcycle_scroller);
        new Handler().post(new Runnable() {
            public void run() {
                editAvatarContainer.requestFocus();
                editScroller.fullScroll(ScrollView.FOCUS_UP);
            }
        });
    }

    private void submitForm(boolean isEditionMode) {
        if (this.isFormValid(isEditionMode)) {
            if (!isEditionMode) {
                this.submitNewForm();
            } else {
                this.submitEditionForm();
            }
        }
    }

    private void updateProgress(int progress) {
        this.llUploadProgressContainer.setVisibility(View.VISIBLE);
        this.tvUploadProgressLabel.setText(getString(R.string.dialog_edit_motorcycle_progress_label, progress));
        this.pbUploadProgress.setProgress(progress);
    }

    private void hideProgress() {
        this.llUploadProgressContainer.setVisibility(View.GONE);
    }

    private void enableDiagButtons() {
        btnPickImage.setEnabled(true);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
    }

    private void disableDiagButtons() {
        btnPickImage.setEnabled(false);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
    }

    private void saveMotorcycle(boolean isEditionMode, boolean uploadImage) {
        TaskCompletionSource<String> uploadFileTaskCompletionSource = new TaskCompletionSource<>();
        Task<String> task = uploadFileTaskCompletionSource.getTask();
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(final String motorcycleId) {
                // avatar file uri
                File mAvatarFile = new File(mAvatarPath);
                Uri mAvatarUri = Uri.fromFile(mAvatarFile);
                String fileName = mAvatarUri.getLastPathSegment();

                // dialog friendly operation
                updateProgress(0);
                disableDiagButtons();

                // storage reference
                final StorageReference imageRef = mMotorcycleImagesStorageReference
                        .child(motorcycleId)
                        .child(fileName);
                imageRef.putFile(mAvatarUri)
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                int progress = (int) (100 * ((double) taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                                updateProgress(progress);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                hideProgress();
                                enableDiagButtons();

                                Toast.makeText(requireContext(), getString(R.string.dialog_edit_motorcycle_error_cannot_upload_image), Toast.LENGTH_LONG).show();
                                getDialog().dismiss();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(requireContext(), getString(R.string.dialog_edit_motorcycle_uploaded_image_in_process), Toast.LENGTH_LONG).show();
                                getDialog().dismiss();
                            }
                        });
            }
        });

        if (!isEditionMode) {
            this.saveMotorcycleInDb(false, null, uploadImage ? uploadFileTaskCompletionSource : null);
        } else {
            this.saveMotorcycleInDb(true, uploadImage ? null : this.mMotorcycle.getImage(), uploadImage ? uploadFileTaskCompletionSource : null);
        }
    }

    private void saveMotorcycleInDb(boolean isEditionMode,
                                    Image image,
                                    final TaskCompletionSource<String> uploadFileTaskCompletionSource) {
        // empty metadata
        MotorcycleMetadata metadata = new MotorcycleMetadata(
                -1,
                false);

        // built motorcycle from form
        final Motorcycle motorcycle = new Motorcycle(
                image,
                this.etBrand.getText().toString().trim().toUpperCase(),
                this.etModel.getText().toString().trim().toUpperCase(),
                this.etType.getText().toString().trim().toUpperCase(),
                Integer.parseInt(this.etCc.getText().toString()),
                this.etColor.getText().toString().trim().toUpperCase(),
                this.etLicensePlateNumber.getText().toString().trim().toUpperCase(),
                metadata);

        // saving
        if (!isEditionMode) {
            // save directly
            this.mMotorcyclesCollReference.add(motorcycle)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            // backend updated, there's internet connection so we can upload
                            if (uploadFileTaskCompletionSource != null) {
                                uploadFileTaskCompletionSource.setResult(documentReference.getId());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireContext(), getString(R.string.dialog_edit_motorcycle_error_cannot_save_db), Toast.LENGTH_LONG).show();
                        }
                    });

            if (uploadFileTaskCompletionSource == null) {
                getDialog().dismiss();
            }
        } else {
            // get current motorcycle metadata
            this.mMotorcyclesCollReference.document(this.mMotorcycleId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Motorcycle currMotorcycle = documentSnapshot.toObject(Motorcycle.class);
                            motorcycle.setMetadata(currMotorcycle.getMetadata());

                            // update motorcycle
                            mMotorcyclesCollReference.document(mMotorcycleId).set(motorcycle)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // backend updated, there's internet connection so we can upload
                                            if (uploadFileTaskCompletionSource != null) {
                                                uploadFileTaskCompletionSource.setResult(mMotorcycleId);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(requireContext(), getString(R.string.dialog_edit_motorcycle_error_cannot_save_db), Toast.LENGTH_LONG).show();
                                        }
                                    });

                            // close dialog if no image will be uploaded
                            if (uploadFileTaskCompletionSource == null) {
                                getDialog().dismiss();
                            }
                        }
                    });
        }
    }

    private void submitNewForm() {
        if (ConnectionUtils.checkInternetConnection(requireContext())) {
            saveMotorcycle(false, true);
        } else {
            AlertDialog.Builder saveConfirmationDiag = new AlertDialog.Builder(requireContext());
            saveConfirmationDiag.setTitle(getString(R.string.dialog_edit_motorcycle_save_offline_title))
                    .setMessage(getString(R.string.dialog_edit_motorcycle_save_offline_msg))
                    .setPositiveButton(getString(R.string.dialog_edit_motorcycle_save_offline_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveMotorcycle(false, false);
                        }
                    })
                    .setNegativeButton(getString(R.string.dialog_edit_motorcycle_save_offline_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            saveConfirmationDiag.create().show();
        }
    }

    private void submitEditionForm() {
        if (ConnectionUtils.checkInternetConnection(requireContext())) {
            if (!TextUtils.isEmpty(this.mAvatarPath)) {
                // image not pristine
                this.saveMotorcycle(true, true);
            } else {
                // image pristine
                this.saveMotorcycle(true, false);
            }
        } else {
            if (!TextUtils.isEmpty(this.mAvatarPath)) {
                // image not pristine
                AlertDialog.Builder saveConfirmationDiag = new AlertDialog.Builder(requireContext());
                saveConfirmationDiag.setTitle(getString(R.string.dialog_edit_motorcycle_save_offline_title))
                        .setMessage(getString(R.string.dialog_edit_motorcycle_save_offline_msg))
                        .setPositiveButton(getString(R.string.dialog_edit_motorcycle_save_offline_continue), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveMotorcycle(true, false);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_edit_motorcycle_save_offline_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                saveConfirmationDiag.create().show();
            } else {
                // image pristine
                saveMotorcycle(true, false);
            }
        }
    }

    private boolean isFormValid(boolean isEditionMode) {
        int errorCounter = 0;

        if (!isEditionMode) {
            if (ConnectionUtils.checkInternetConnection(requireContext())
                    && TextUtils.isEmpty(this.mAvatarPath)) {
                this.tvAvatarError.setVisibility(View.VISIBLE);
                errorCounter++;
            } else {
                this.tvAvatarError.setVisibility(View.GONE);
            }
        } else {
            if (ConnectionUtils.checkInternetConnection(requireContext())
                    && this.mMotorcycle.getImage() == null
                    && TextUtils.isEmpty(this.mAvatarPath)) {
                this.tvAvatarError.setVisibility(View.VISIBLE);
                errorCounter++;
            } else {
                this.tvAvatarError.setVisibility(View.GONE);
            }
        }

        if (TextUtils.isEmpty(etBrand.getText().toString())) {
            this.tiBrand.setError(getString(R.string.dialog_edit_motorcycle_error_brand));
            errorCounter++;
        } else {
            this.tiBrand.setError(null);
        }

        if (TextUtils.isEmpty(etModel.getText().toString())) {
            this.tiModel.setError(getString(R.string.dialog_edit_motorcycle_error_model));
            errorCounter++;
        } else {
            this.tiModel.setError(null);
        }

        if (TextUtils.isEmpty(etType.getText().toString())) {
            this.tiType.setError(getString(R.string.dialog_edit_motorcycle_error_type));
            errorCounter++;
        } else {
            this.tiType.setError(null);
        }

        if (TextUtils.isEmpty(etCc.getText().toString())) {
            this.tiCc.setError(getString(R.string.dialog_edit_motorcycle_error_cylinder));
            errorCounter++;
        } else {
            this.tiCc.setError(null);
        }

        if (TextUtils.isEmpty(etColor.getText().toString())) {
            this.ticolor.setError(getString(R.string.dialog_edit_motorcycle_error_color));
            errorCounter++;
        } else {
            this.ticolor.setError(null);
        }

        if (TextUtils.isEmpty(etLicensePlateNumber.getText().toString())) {
            this.tiLicensePlateNumber.setError(getString(R.string.dialog_edit_motorcycle_error_license_plate_number));
            errorCounter++;
        } else {
            this.tiLicensePlateNumber.setError(null);
        }

        return errorCounter == 0;
    }

    private boolean isEditionMode() {
        return this.mMotorcycleId != null && this.mMotorcycle != null;
    }

    private void setEditionForm() {
        if (this.mMotorcycle.getImage() != null
                && TextUtils.isEmpty(this.mAvatarPath)) {
            this.showAvatar();

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_edit_motorcycle_placeholder)
                    .dontAnimate()
                    .dontTransform();

            Glide.with(this)
                    .load(this.mMotorcycle.getImage().getImagePublicUrl())
                    .apply(options)
                    .into(this.ivAvatar);
        }

        this.etBrand.setText(this.mMotorcycle.getBrand());
        this.etModel.setText(this.mMotorcycle.getModel());
        this.etType.setText(this.mMotorcycle.getType());
        this.etCc.setText(String.valueOf(this.mMotorcycle.getCc()));
        this.etColor.setText(this.mMotorcycle.getColor());
        this.etLicensePlateNumber.setText(this.mMotorcycle.getLicensePlateNumber());
    }

    private void showAvatar() {
        this.ivAvatar.setVisibility(View.VISIBLE);
        this.ivAvatarPlaceHolder.setVisibility(View.GONE);
    }
}
