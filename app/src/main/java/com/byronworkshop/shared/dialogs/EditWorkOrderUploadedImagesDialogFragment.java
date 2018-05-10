package com.byronworkshop.shared.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.uploadedfiles.UploadedImagesRVAdapter;
import com.byronworkshop.shared.dialogs.adapters.uploadedfiles.pojo.UploadedImage;
import com.byronworkshop.utils.BitmapUtils;
import com.byronworkshop.utils.ConnectionUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class EditWorkOrderUploadedImagesDialogFragment extends DialogFragment
        implements UploadedImagesRVAdapter.ImageClickListener {

    // firebase analytics events
    private static final String EVENT_UPLOAD_IMAGES = "show_uploaded_images";

    // dialog finals
    private static final String TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG = "wo_uploaded_images_dialog";

    // request permission codes
    private static final int REQUEST_WRITE_STORAGE_PERMISSION_PICKER = 301;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION_DOWNLOAD = 302;

    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    private String mWorkOrderFormId;

    // database
    private CollectionReference mMotorcycleWorkOrderImagesCollReference;
    private DocumentReference mMotorcycleWorkOrderFormDocReference;
    private StorageReference mMotorcycleWorkOrderImagesRef;

    private ListenerRegistration mImageCounterListener;

    // resources
    private RecyclerView mUploadedImagesRecyclerView;
    private LinearLayout mUploadedImagesRVEmptyText;
    private AppCompatButton mBtnUpload;

    private FirestoreRecyclerAdapter mUploadedImagesAdapter;

    private LinearLayout llUploadProgressContainer;
    private TextView tvUploadProgressLabel;
    private ProgressBar pbUploadProgress;

    public static void showEditWorkOrderUploadedImagesDialog(
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
        FirebaseAnalytics.getInstance(context).logEvent(EVENT_UPLOAD_IMAGES, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, String workOrderFormId) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editCostSheetDialog = fm.findFragmentByTag(TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG);
        if (editCostSheetDialog != null) {
            ft.remove(editCostSheetDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditWorkOrderUploadedImagesDialogFragment.newInstance(uId, motorcycleId, workOrderFormId);
        df.show(fm, TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG);
    }

    private static EditWorkOrderUploadedImagesDialogFragment newInstance(String uId, String motorcycleId, String workOrderFormId) {
        EditWorkOrderUploadedImagesDialogFragment frag = new EditWorkOrderUploadedImagesDialogFragment();

        Bundle args = new Bundle();
        args.putString("mUid", uId);
        args.putString("mMotorcycleId", motorcycleId);
        args.putString("mWorkOrderFormId", workOrderFormId);
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
        }

        // database
        this.mMotorcycleWorkOrderImagesCollReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("file_repos").document(this.mWorkOrderFormId).collection("images");
        this.mMotorcycleWorkOrderFormDocReference = FirebaseFirestore.getInstance().collection("users").document(this.mUid).collection("work_orders").document(this.mMotorcycleId).collection("forms").document(this.mWorkOrderFormId);
        this.mMotorcycleWorkOrderImagesRef = FirebaseStorage.getInstance().getReference().child("users").child(this.mUid).child("work_orders").child(this.mMotorcycleId).child("file_repos").child(this.mWorkOrderFormId).child("images");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity mOwnerActivity = requireActivity();

        // get the layout inflater
        LayoutInflater inflater = mOwnerActivity.getLayoutInflater();
        View mDialogView = inflater.inflate(R.layout.dialog_edit_uploaded_images, null);

        // resources
        this.mUploadedImagesRecyclerView = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_rv);
        this.mUploadedImagesRVEmptyText = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_rv_empty_view);
        this.mBtnUpload = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_upload);

        this.llUploadProgressContainer = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_progress_container);
        this.tvUploadProgressLabel = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_progress_label);
        this.pbUploadProgress = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_progress);

        // setting up recyclerview
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mOwnerActivity);
        this.mUploadedImagesRecyclerView.setLayoutManager(linearLayoutManager);
        this.mUploadedImagesRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // add button listener
        this.mBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity mOwnerActivity = requireActivity();

                // check writing permissions before launching the camera
                if (ContextCompat.checkSelfPermission(mOwnerActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION_PICKER);
                } else {
                    launchPicker();
                }
            }
        });

        // create dialog
        String title = getString(R.string.dialog_edit_uploaded_images_dialog_title);
        AlertDialog.Builder builder = new AlertDialog.Builder(mOwnerActivity);
        builder.setTitle(title)
                .setView(mDialogView)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_edit_uploaded_images_dialog_btn_close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE_PERMISSION_PICKER: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchPicker();
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.dialog_edit_uploaded_images_writing_storage_denied), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_WRITE_STORAGE_PERMISSION_DOWNLOAD: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), getString(R.string.dialog_edit_uploaded_images_writing_storage_accepter_download), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireActivity(), getString(R.string.dialog_edit_uploaded_images_writing_storage_denied), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), getString(R.string.dialog_edit_uploaded_images_image_chooser_error), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onImagesPicked(@NonNull List<File> imagesFiles, EasyImage.ImageSource source, int type) {
                File takenImageFile = imagesFiles.get(0);

                // process and upload
                processAndUploadImage(takenImageFile);

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

    @Override
    public void onStart() {
        super.onStart();

        // attach firebase
        this.attachUploadedImagesRVAdapter();
        this.attachImageCounterListener();
    }

    @Override
    public void onStop() {
        super.onStop();

        // detach firebase
        this.detachUploadedImagesRVAdapter();
        this.detachImageCounterListener();
    }

    // ---------------------------------------------------------------------------------------------
    // custom methods
    // ---------------------------------------------------------------------------------------------
    private void launchPicker() {
        Context context = requireContext();
        if (!ConnectionUtils.checkInternetConnection(context)) {
            Toast.makeText(context, context.getString(R.string.dialog_edit_uploaded_images_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        EasyImage.openChooserWithDocuments(this, getString(R.string.dialog_edit_uploaded_images_image_chooser_title), 0);
    }

    private void deleteFromCache() {
        File cachedImageFile = EasyImage.lastlyTakenButCanceledPhoto(requireContext());
        if (cachedImageFile != null && cachedImageFile.getAbsolutePath().contains("cache/EasyImage")) {
            cachedImageFile.delete();
        }
    }

    private void processAndUploadImage(File takenImageFile) {
        Activity mCaller = requireActivity();

        // resample saved image
        Bitmap avatar = BitmapUtils.resamplePic(takenImageFile.getAbsolutePath());

        // save image by compressing the file
        String savedImagePath = BitmapUtils.saveImage(mCaller, avatar);

        // delete tmp file if comes from camera
        BitmapUtils.deleteImageFile(mCaller, takenImageFile.getAbsolutePath());

        // check again internet connection
        if (!ConnectionUtils.checkInternetConnection(mCaller)) {
            Toast.makeText(mCaller, mCaller.getString(R.string.dialog_edit_uploaded_images_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        // dialog friendly operation
        this.updateProgress(0);
        this.disableDiagButtons();

        // image uri
        final Uri imageUri = Uri.fromFile(new File(savedImagePath));

        // save in database
        UploadedImage image = new UploadedImage();
        image.setDate(Calendar.getInstance().getTimeInMillis());
        this.mMotorcycleWorkOrderImagesCollReference.add(image)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(final DocumentReference documentReference) {
                        // backend updated, there's an internet connection so we can upload the file
                        StorageReference imageRef = mMotorcycleWorkOrderImagesRef
                                .child(documentReference.getId())
                                .child(imageUri.getLastPathSegment());
                        imageRef.putFile(imageUri)
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

                                        // on failure delete from database
                                        documentReference.delete();

                                        Toast.makeText(requireContext(), getString(R.string.dialog_edit_uploaded_images_progress_error), Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        hideProgress();
                                        enableDiagButtons();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgress();
                        enableDiagButtons();

                        Toast.makeText(requireContext(), getString(R.string.dialog_edit_uploaded_images_save_error), Toast.LENGTH_LONG).show();
                    }
                });
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
        this.mBtnUpload.setEnabled(true);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
    }

    private void disableDiagButtons() {
        this.mBtnUpload.setEnabled(false);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    private void attachUploadedImagesRVAdapter() {
        // prepare recycler options
        FirestoreRecyclerOptions<UploadedImage> options =
                new FirestoreRecyclerOptions.Builder<UploadedImage>()
                        .setQuery(this.mMotorcycleWorkOrderImagesCollReference.orderBy("date", Query.Direction.DESCENDING), UploadedImage.class)
                        .build();

        // manually stop previous existent adapter
        if (this.mUploadedImagesAdapter != null) {
            this.mUploadedImagesAdapter.stopListening();
            this.mUploadedImagesAdapter = null;
        }

        // create new adapter and start listening
        this.mUploadedImagesAdapter = new UploadedImagesRVAdapter(
                options,
                this,
                this.mUploadedImagesRVEmptyText);
        this.mUploadedImagesAdapter.startListening();
        this.mUploadedImagesRecyclerView.setAdapter(mUploadedImagesAdapter);
    }

    private void detachUploadedImagesRVAdapter() {
        if (this.mUploadedImagesAdapter != null) {
            this.mUploadedImagesAdapter.stopListening();
            this.mUploadedImagesAdapter = null;
            this.mUploadedImagesRecyclerView.setAdapter(null);
        }
    }

    private void attachImageCounterListener() {
        this.mImageCounterListener = this.mMotorcycleWorkOrderImagesCollReference.addSnapshotListener(
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null || queryDocumentSnapshots == null) {
                            return;
                        }

                        int counter = 0;
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.exists()) {
                                continue;
                            }

                            counter++;
                        }

                        // update image counter
                        mMotorcycleWorkOrderFormDocReference.update("imageCounter", counter);
                    }
                });
    }

    private void detachImageCounterListener() {
        if (this.mImageCounterListener != null) {
            this.mImageCounterListener.remove();
            this.mImageCounterListener = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // RV Adapter listener
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onDeleteImage(final String imageId, final UploadedImage uploadedImage) {
        Context context = requireContext();

        // check internet connection
        if (!ConnectionUtils.checkInternetConnection(context)) {
            Toast.makeText(context, getString(R.string.dialog_edit_uploaded_images_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        // check if fully processed
        if (uploadedImage.getImage() == null) {
            Toast.makeText(context, context.getString(R.string.dialog_edit_uploaded_images_rv_item_cant_delete), Toast.LENGTH_LONG).show();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.dialog_edit_uploaded_images_delete_title))
                .setMessage(getString(R.string.dialog_edit_uploaded_images_delete_msg))
                .setPositiveButton(getString(R.string.dialog_edit_uploaded_images_delete_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMotorcycleWorkOrderImagesCollReference.document(imageId).delete();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_edit_uploaded_images_delete_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    @Override
    public void onReqDownloadWriteStoragePermission(UploadedImage uploadedImage) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION_DOWNLOAD);
        } else {
            downloadImage(uploadedImage);
        }
    }

    private void downloadImage(UploadedImage uploadedImage) {
        Context context = requireContext();

        // check internet connection
        if (!ConnectionUtils.checkInternetConnection(context)) {
            Toast.makeText(context, context.getString(R.string.dialog_edit_uploaded_images_no_internet), Toast.LENGTH_LONG).show();
            return;
        }

        // check if fully processed
        if (uploadedImage.getImage() == null) {
            Toast.makeText(context, context.getString(R.string.dialog_edit_uploaded_images_rv_item_cant_delete), Toast.LENGTH_LONG).show();
            return;
        }

        // image uri
        Uri imageUri = Uri.parse(uploadedImage.getImage().getImageUrl());
        String imageName = imageUri.getLastPathSegment();

        // public download uri
        Uri downloadUri = Uri.parse(uploadedImage.getImage().getImagePublicUrl());

        // create byron dir if not existent
        File downloadByron = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Byron");
        if (!downloadByron.exists()) {
            downloadByron.mkdirs();
        }

        //download
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setTitle(context.getString(R.string.dialog_edit_uploaded_images_rv_item_download_title));
        request.setDescription(context.getString(R.string.dialog_edit_uploaded_images_rv_item_download_msg, imageName));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/Byron/" + imageUri.getLastPathSegment());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
        }
    }
}
