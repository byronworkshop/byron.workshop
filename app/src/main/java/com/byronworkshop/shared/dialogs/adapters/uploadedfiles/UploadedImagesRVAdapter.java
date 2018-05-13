package com.byronworkshop.shared.dialogs.adapters.uploadedfiles;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.uploadedfiles.pojo.UploadedImage;
import com.byronworkshop.utils.DateUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class UploadedImagesRVAdapter extends FirestoreRecyclerAdapter<UploadedImage, UploadedImagesRVAdapter.ImageHolder> {

    private final ImageClickListener mListener;
    private final LinearLayout mEmptyText;
    private final boolean mDisableActions;

    public interface ImageClickListener {
        void onDeleteImage(String imageId, UploadedImage uploadedImage);

        void onReqDownloadWriteStoragePermission(UploadedImage uploadedImage);
    }

    public UploadedImagesRVAdapter(@NonNull FirestoreRecyclerOptions<UploadedImage> options,
                                   @NonNull ImageClickListener listener,
                                   boolean disableActions,
                                   @NonNull LinearLayout emptyText) {
        super(options);

        this.mListener = listener;
        this.mEmptyText = emptyText;
        this.mDisableActions = disableActions;
    }

    @NonNull
    @Override
    public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.dialog_edit_uploaded_images_rv_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new ImageHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ImageHolder holder, int position, @NonNull UploadedImage uploadedImage) {
        // image
        if (uploadedImage.getImage() != null) {
            ColorDrawable imagePlaceholder = new ColorDrawable(ContextCompat.getColor(holder.imageView.getContext(), R.color.colorPlaceholder));
            RequestOptions options = RequestOptions.placeholderOf(imagePlaceholder);

            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(holder.imageView.getContext())
                    .load(uploadedImage.getImage().getThumbnailPublicUrl())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageView);
        } else {
            holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.imageView.setImageResource(R.drawable.ic_upload_image_processing);
        }

        // uploaded date
        if (uploadedImage.getDate() != null) {
            holder.date.setText(DateUtils.getFormattedDate(uploadedImage.getDate()));
        }
    }

    @Override
    public void onDataChanged() {
        this.mEmptyText.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    class ImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView imageView;
        final TextView date;
        final ImageView moreOptions;

        ImageHolder(View itemView) {
            super(itemView);

            this.imageView = itemView.findViewById(R.id.dialog_edit_uploaded_images_rv_item_image);
            this.date = itemView.findViewById(R.id.dialog_edit_uploaded_images_rv_item_date);
            this.moreOptions = itemView.findViewById(R.id.dialog_edit_uploaded_images_rv_item_more_options);

            this.moreOptions.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == this.moreOptions.getId()) {
                Context context = v.getContext();

                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.inflate(R.menu.menu_wo_uploaded_images_more_options);
                popupMenu.getMenu().findItem(R.id.menu_wo_uploaded_images_delete).setVisible(!mDisableActions);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        int adapterPosition = getAdapterPosition();
                        if (adapterPosition == RecyclerView.NO_POSITION) {
                            return false;
                        }

                        DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(adapterPosition);
                        UploadedImage uploadedImage = documentSnapshot.toObject(UploadedImage.class);
                        if (uploadedImage == null) {
                            return false;
                        }

                        switch (id) {
                            case R.id.menu_wo_uploaded_images_download:
                                mListener.onReqDownloadWriteStoragePermission(uploadedImage);
                                return true;
                            case R.id.menu_wo_uploaded_images_delete:
                                mListener.onDeleteImage(documentSnapshot.getId(), uploadedImage);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
            }
        }
    }
}