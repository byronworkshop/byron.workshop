package com.byronworkshop.ui.reports.reminders.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.byronworkshop.R;
import com.byronworkshop.ui.mainactivity.adapter.pojo.Motorcycle;
import com.byronworkshop.utils.DateUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class MotorcycleReminderRVAdapter extends FirestoreRecyclerAdapter<Motorcycle, MotorcycleReminderRVAdapter.MotorcycleHolder> {

    private final ListItemClickListener mOnClickListener;
    private final View emptyView;

    public interface ListItemClickListener {
        void onListItemClick(String motorcycleId, Motorcycle motorcycle);
    }

    public MotorcycleReminderRVAdapter(@NonNull FirestoreRecyclerOptions<Motorcycle> options,
                                       @NonNull ListItemClickListener itemClickListener,
                                       @NonNull View emptyView) {
        super(options);

        this.mOnClickListener = itemClickListener;
        this.emptyView = emptyView;
    }

    @NonNull
    @Override
    public MotorcycleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.content_reminders_motorcycle_rv_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new MotorcycleHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull MotorcycleHolder holder, int position, @NonNull Motorcycle motorcycle) {
        Context context = holder.elapsedTime.getContext();

        String elapsedDateStr = DateUtils.getFriendlyDateString(context, motorcycle.getMetadata().getLastWorkOrderEndDate().getTime());

        holder.licensePlateNumber.setText(motorcycle.getLicensePlateNumber());
        holder.brand.setText(motorcycle.getBrand());
        holder.elapsedTime.setText(context.getString(R.string.activity_reminders_rv_item_last_work, elapsedDateStr));

        if (motorcycle.getImage() == null) {
            holder.motorcycleAvatar.setImageResource(R.drawable.ic_motorcycle_rv_item_placeholder_no_image);
        } else {
            Glide.with(holder.motorcycleAvatar.getContext())
                    .load(motorcycle.getImage().getThumbnailPublicUrl())
                    .apply(RequestOptions.circleCropTransform())
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
        final TextView elapsedTime;

        MotorcycleHolder(View itemView) {
            super(itemView);

            this.motorcycleAvatar = itemView.findViewById(R.id.content_reminders_motorcycle_rv_item_avatar);
            this.brand = itemView.findViewById(R.id.content_reminders_motorcycle_rv_item_brand);
            this.licensePlateNumber = itemView.findViewById(R.id.content_reminders_motorcycle_rv_item_license_plate_number);
            this.elapsedTime = itemView.findViewById(R.id.content_reminders_motorcycle_rv_item_elapsed_time);

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