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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
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
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.uploadedfiles.UploadedImagesRVAdapter;
import com.byronworkshop.shared.dialogs.adapters.uploadedfiles.pojo.UploadedImage;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.utils.BitmapUtils;
import com.byronworkshop.utils.ConnectionUtils;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;

public class EditWorkOrderUploadedImagesDialogFragment extends DialogFragment
        implements UploadedImagesRVAdapter.ImageClickListener {

    // firebase analytics events
    private static final String EVENT_UPLOAD_IMAGES = "show_uploaded_images";

    // dialog finals
    private static final String TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG = "wo_uploaded_images_dialog";
    private static final String KEY_UPLOADING_IMAGE_REF = "uploading_image_ref";
    private static final String KEY_UPLOADING_IMAGE_DOC_ID = "image_doc_id";

    // request permission codes
    private static final int REQUEST_WRITE_STORAGE_PERMISSION_PICKER = 301;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION_DOWNLOAD = 302;

    // args mandatory
    private String mUid;
    private String mMotorcycleId;
    private String mWorkOrderFormId;
    private WorkOrderForm mWorkOrderForm;

    // firebase
    private CollectionReference mMotorcycleWorkOrderImagesCollReference;
    private DocumentReference mMotorcycleWorkOrderFormDocReference;
    private StorageReference mMotorcycleWorkOrderImagesRef;

    // for restarting partial uploads
    private String mTmpImageDocId;
    private StorageReference mTmpUploadingImageRef;

    // to avoid leaking fragment with upload listeners we should stop them in onStop
    private StorageTask<UploadTask.TaskSnapshot> mTmpUploadTask;
    private OnProgressListener<UploadTask.TaskSnapshot> mTmpUploadProgressListener;
    private OnFailureListener mTmpUploadFailureListener;
    private OnSuccessListener<UploadTask.TaskSnapshot> mTmpUploadSuccessListener;

    private ListenerRegistration mImageCounterListener;

    // resources
    private CoordinatorLayout mMainContainer;
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
            @NonNull String workOrderFormId,
            @NonNull WorkOrderForm workOrderForm) {
        // replace same dialog fragments with a new one
        replaceAllWithNewInstance(fm, uId, motorcycleId, workOrderFormId, workOrderForm);

        // log firebase analytics view item event
        logFirebaseViewItemEvent(context);
    }

    private static void logFirebaseViewItemEvent(Context context) {
        FirebaseAnalytics.getInstance(context).logEvent(EVENT_UPLOAD_IMAGES, null);
    }

    private static void replaceAllWithNewInstance(FragmentManager fm, String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment editCostSheetDialog = fm.findFragmentByTag(TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG);
        if (editCostSheetDialog != null) {
            ft.remove(editCostSheetDialog);
        }
        ft.addToBackStack(null);
        ft.commit();

        DialogFragment df = EditWorkOrderUploadedImagesDialogFragment.newInstance(uId, motorcycleId, workOrderFormId, workOrderForm);
        df.show(fm, TAG_EDIT_WO_UPLOADED_IMAGES_DIALOG);
    }

    private static EditWorkOrderUploadedImagesDialogFragment newInstance(String uId, String motorcycleId, String workOrderFormId, WorkOrderForm workOrderForm) {
        EditWorkOrderUploadedImagesDialogFragment frag = new EditWorkOrderUploadedImagesDialogFragment();

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

        // if there's an upload in progress, save the reference so you can query it later
        if (this.mTmpUploadingImageRef != null && this.mTmpImageDocId != null) {
            outState.putString(KEY_UPLOADING_IMAGE_REF, this.mTmpUploadingImageRef.toString());
            outState.putString(KEY_UPLOADING_IMAGE_DOC_ID, this.mTmpImageDocId);
        }
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

        if (savedInstanceState != null) {
            // check partial upload
            if (savedInstanceState.containsKey(KEY_UPLOADING_IMAGE_DOC_ID)
                    && savedInstanceState.containsKey(KEY_UPLOADING_IMAGE_REF)) {
                String uploadingImageRef = savedInstanceState.getString(KEY_UPLOADING_IMAGE_REF);
                String imageDocId = savedInstanceState.getString(KEY_UPLOADING_IMAGE_DOC_ID);

                if (uploadingImageRef != null && imageDocId != null) {
                    this.mTmpUploadingImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(uploadingImageRef);
                    this.mTmpImageDocId = imageDocId;
                }
            }
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
        this.mMainContainer = mDialogView.findViewById(R.id.dialog_edit_uploaded_images_main);
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
        if (!this.mWorkOrderForm.isClosed()) {
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
        } else {
            this.mBtnUpload.setVisibility(View.GONE);
        }

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

        // restart file uploads
        this.restartPendingImageUploadsListeners();

        // attach firebase
        this.attachUploadedImagesRVAdapter();
        this.attachImageCounterListener();
    }

    @Override
    public void onStop() {
        super.onStop();

        // detach firebase
        this.detachImageUploadListeners();
        this.detachUploadedImagesRVAdapter();
        this.detachImageCounterListener();
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
        Context context = requireContext();
        if (!ConnectionUtils.checkInternetConnection(context)) {
            Snackbar.make(this.mMainContainer, context.getString(R.string.dialog_edit_uploaded_images_no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        EasyImage.openChooserWithDocuments(this, getString(R.string.dialog_edit_uploaded_images_image_chooser_title), 0);
    }

    private void processAndUploadImage(File takenImageFile) {
        Activity mCaller = requireActivity();

        // save image by resizing and compressing the file
        String savedImagePath = BitmapUtils.saveImage(mCaller, takenImageFile.getAbsolutePath());

        // delete tmp file if comes from camera
        BitmapUtils.deleteImageFile(mCaller, takenImageFile.getAbsolutePath());

        // image uri
        Uri imageUri = Uri.fromFile(new File(savedImagePath));

        // check again internet connection
        if (!ConnectionUtils.checkInternetConnection(mCaller)) {
            Snackbar.make(this.mMainContainer, mCaller.getString(R.string.dialog_edit_uploaded_images_no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        // ---
        // start uploading
        // ---

        // saving upload data
        mTmpImageDocId = this.mMotorcycleWorkOrderImagesCollReference.document().getId();
        mTmpUploadingImageRef = mMotorcycleWorkOrderImagesRef
                .child(mTmpImageDocId)
                .child(imageUri.getLastPathSegment());
        UploadTask task = mTmpUploadingImageRef.putFile(imageUri);
        attachImageUploadListeners(task, mTmpImageDocId);
    }

    private void restartPendingImageUploadsListeners() {
        if (this.mTmpUploadingImageRef == null || this.mTmpImageDocId == null
                // cannot restart if already uploading due to onImagesPicked
                || this.mTmpUploadTask != null) {
            return;
        }

        // find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasks = this.mTmpUploadingImageRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // get the task monitoring the upload
            UploadTask task = tasks.get(0);

            // attach listeners
            attachImageUploadListeners(task, this.mTmpImageDocId);
        } else {
            this.mTmpUploadingImageRef = null;
            this.mTmpImageDocId = null;
        }
    }

    private void detachImageUploadListeners() {
        if (this.mTmpUploadTask != null) {
            // stop listening the upload
            this.mTmpUploadTask
                    .removeOnSuccessListener(this.mTmpUploadSuccessListener)
                    .removeOnFailureListener(this.mTmpUploadFailureListener)
                    .removeOnProgressListener(this.mTmpUploadProgressListener);

            this.mTmpUploadTask = null;
            this.mTmpUploadProgressListener = null;
            this.mTmpUploadSuccessListener = null;
            this.mTmpUploadFailureListener = null;
        }
    }

    private void attachImageUploadListeners(UploadTask task, final String imageId) {
        // disable all dialog actions
        this.disableDiagButtons();

        // create listeners
        this.mTmpUploadProgressListener = new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                int progress = (int) (100 * ((double) taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                updateProgress(progress);
            }
        };

        this.mTmpUploadFailureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // dialog get back to normal
                hideProgress();
                enableDiagButtons();

                // remove tmp vars
                mTmpImageDocId = null;
                mTmpUploadingImageRef = null;

                // show failure SnackBar
                Snackbar.make(mMainContainer, getString(R.string.dialog_edit_uploaded_images_upload_error), Snackbar.LENGTH_SHORT).show();
            }
        };

        this.mTmpUploadSuccessListener = new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot uploadTaskSnapshot) {
                // dialog get back to normal
                hideProgress();
                enableDiagButtons();

                // remove tmp vars
                mTmpImageDocId = null;
                mTmpUploadingImageRef = null;

                // save image in firestore
                Map<String, Object> image = new HashMap<>();
                image.put("date", FieldValue.serverTimestamp());
                SetOptions options = SetOptions.merge();

                mMotorcycleWorkOrderImagesCollReference.document(imageId).set(image, options)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // log error to Crashlytics
                                Crashlytics.logException(e);

                                // check if still with context
                                if (getContext() == null) {
                                    return;
                                }

                                // cannot save in firestore show SnackBar error
                                // (images will rollback automatically thanks to google cloud functions when
                                // no reference is found to this document due to nonexistence, then they won't
                                // be able to update the image reference of the document and will be deleted
                                // automatically)
                                // we must wait to recyclerview to finish its animations before showing the SnackBar

                                mUploadedImagesRecyclerView.smoothScrollToPosition(0);
                                mUploadedImagesRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        // check if still with context
                                        if (getContext() == null) {
                                            return;
                                        }

                                        // show failure SnackBar
                                        Snackbar.make(mUploadedImagesRecyclerView, getString(R.string.dialog_edit_uploaded_images_save_error), Snackbar.LENGTH_LONG).show();

                                        // remove listener
                                        mUploadedImagesRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                });
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // check if still with context
                                if (getContext() == null) {
                                    return;
                                }

                                // go to top
                                mUploadedImagesRecyclerView.smoothScrollToPosition(0);
                            }
                        });
            }
        };


        // start listening the upload
        this.mTmpUploadTask = task
                .addOnProgressListener(this.mTmpUploadProgressListener)
                .addOnFailureListener(this.mTmpUploadFailureListener)
                .addOnSuccessListener(this.mTmpUploadSuccessListener);
    }

    private void updateProgress(int progress) {
        if (this.llUploadProgressContainer.getVisibility() != View.VISIBLE) {
            this.llUploadProgressContainer.setVisibility(View.VISIBLE);
        }

        this.tvUploadProgressLabel.setText(getString(R.string.dialog_edit_motorcycle_progress_label, progress));
        this.pbUploadProgress.setProgress(progress);
    }

    private void hideProgress() {
        if (this.llUploadProgressContainer.getVisibility() != View.GONE) {
            this.llUploadProgressContainer.setVisibility(View.GONE);
        }
    }

    private void enableDiagButtons() {
        if (!this.mBtnUpload.isEnabled()) {
            this.mBtnUpload.setEnabled(true);
        }

        Button positiveBtn = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        if (!positiveBtn.isEnabled()) {
            positiveBtn.setEnabled(true);
        }
    }

    private void disableDiagButtons() {
        if (this.mBtnUpload.isEnabled()) {
            this.mBtnUpload.setEnabled(false);
        }

        Button positiveBtn = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveBtn.isEnabled()) {
            positiveBtn.setEnabled(false);
        }
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
                this.mWorkOrderForm.isClosed(),
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

        // check if fully processed
        if (uploadedImage.getImage() == null) {
            Snackbar.make(this.mMainContainer, context.getString(R.string.dialog_edit_uploaded_images_rv_item_cant_delete), Snackbar.LENGTH_SHORT).show();
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.dialog_edit_uploaded_images_delete_title))
                .setMessage(getString(R.string.dialog_edit_uploaded_images_delete_msg))
                .setPositiveButton(getString(R.string.dialog_edit_uploaded_images_delete_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // deleting from firestore
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
            Snackbar.make(this.mMainContainer, context.getString(R.string.dialog_edit_uploaded_images_no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        // check if fully processed
        if (uploadedImage.getImage() == null) {
            Snackbar.make(this.mMainContainer, context.getString(R.string.dialog_edit_uploaded_images_rv_item_cant_delete), Snackbar.LENGTH_SHORT).show();
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
