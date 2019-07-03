package com.byronworkshop.ui.mainactivity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class MotorcycleRVAdapter extends FirestoreRecyclerAdapter<Motorcycle, MotorcycleRVAdapter.MotorcycleHolder> {

    private final ListItemClickListener mOnClickListener;
    private final View emptyView;
    private final boolean mUseTabletLayout;

    public interface ListItemClickListener {
        void onListItemClick(String motorcycleId, Motorcycle motorcycle);
    }

    public MotorcycleRVAdapter(@NonNull Context context,
                               @NonNull FirestoreRecyclerOptions<Motorcycle> options,
                               @NonNull ListItemClickListener itemClickListener,
                               @NonNull View emptyView) {
        super(options);

        this.mOnClickListener = itemClickListener;
        this.emptyView = emptyView;
        this.mUseTabletLayout = context.getResources().getBoolean(R.bool.tablet_mode);
    }

    @NonNull
    @Override
    public MotorcycleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        int layoutIdForListItem = R.layout.content_main_motorcycle_rv_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new MotorcycleHolder(view, this.mUseTabletLayout);
    }

    @Override
    protected void onBindViewHolder(@NonNull MotorcycleHolder holder, int position, @NonNull Motorcycle motorcycle) {
        String cc = motorcycle.getCc() + " cc";

        // generic TextView's
        holder.licensePlateNumber.setText(motorcycle.getLicensePlateNumber());
        holder.brand.setText(motorcycle.getBrand());
        holder.cc.setText(cc);
        holder.model.setText(motorcycle.getModel());

        // tablet mode
        if (this.mUseTabletLayout) {
            holder.type.setText(motorcycle.getType());
            holder.color.setText(motorcycle.getColor());
        }

        // load image
        if (motorcycle.getImage() == null) {
            holder.motorcycleAvatar.setImageResource(R.drawable.ic_motorcycle_rv_item_placeholder_no_image);
        } else {
            RequestOptions options = RequestOptions.placeholderOf(R.drawable.ic_motorcycle_rv_item_placeholder)
                    .circleCrop();

            Glide.with(holder.motorcycleAvatar.getContext())
                    .load(motorcycle.getImage().getThumbnailPublicUrl())
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.motorcycleAvatar);
        }
    }

    @Override
    public void onDataChanged() {
        this.emptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    class MotorcycleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView motorcycleAvatar;
        final TextView brand;
        final TextView licensePlateNumber;
        final TextView cc;
        final TextView model;
        final TextView type;
        final TextView color;

        MotorcycleHolder(View itemView, boolean useTabletLayout) {
            super(itemView);

            this.motorcycleAvatar = itemView.findViewById(R.id.content_main_motorcycle_rv_item_avatar);
            this.brand = itemView.findViewById(R.id.content_main_motorcycle_rv_item_brand);
            this.licensePlateNumber = itemView.findViewById(R.id.content_main_motorcycle_rv_item_license_plate_number);
            this.cc = itemView.findViewById(R.id.content_main_motorcycle_rv_item_cc);
            this.model = itemView.findViewById(R.id.content_main_motorcycle_rv_item_model);
            if (useTabletLayout) {
                this.type = itemView.findViewById(R.id.content_main_motorcycle_rv_item_type);
                this.color = itemView.findViewById(R.id.content_main_motorcycle_rv_item_color);
            } else {
                this.type = null;
                this.color = null;
            }

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(adapterPosition);
            mOnClickListener.onListItemClick(documentSnapshot.getId(), getSnapshots().get(adapterPosition));
        }
    }
}