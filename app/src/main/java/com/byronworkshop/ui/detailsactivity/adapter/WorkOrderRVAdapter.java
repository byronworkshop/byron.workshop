package com.byronworkshop.ui.detailsactivity.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.byronworkshop.R;
import com.byronworkshop.ui.detailsactivity.adapter.pojo.WorkOrderForm;
import com.byronworkshop.utils.DateUtils;
import com.byronworkshop.utils.DecimalFormatterUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;

public class WorkOrderRVAdapter extends FirestoreRecyclerAdapter<WorkOrderForm, WorkOrderRVAdapter.WorkOrderHolder> {

    private final ListItemClickListener mOnClickListener;
    private final LinearLayout mEmptyText;
    private final TextView mSubtitle;

    public interface ListItemClickListener {
        void onShowEditWorkOrderDialog(String workOrderFormId, WorkOrderForm workOrderForm);

        void onShowEditCostSheetDialog(String workOrderFormId, WorkOrderForm workOrderForm);

        void onShowEditUploadedImagesDialog(String workOrderFormId, WorkOrderForm workOrderForm);

        void onDeleteWorkOrder(String workOrderFormId, WorkOrderForm workOrderForm);
    }

    public WorkOrderRVAdapter(@NonNull FirestoreRecyclerOptions<WorkOrderForm> options,
                              @NonNull ListItemClickListener itemClickListener,
                              @NonNull LinearLayout emptyText,
                              @NonNull TextView subtitle) {
        super(options);

        this.mOnClickListener = itemClickListener;
        this.mEmptyText = emptyText;
        this.mSubtitle = subtitle;
    }

    @NonNull
    @Override
    public WorkOrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.content_details_wo_rv_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new WorkOrderHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull WorkOrderHolder holder, int position, @NonNull WorkOrderForm workOrderForm) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(workOrderForm.getDate());

        Context context = holder.issue.getContext();

        holder.issue.setText(workOrderForm.getIssue());
        holder.date.setText(DateUtils.getFormattedDate(c));
        holder.totalCost.setText(DecimalFormatterUtils.formatCurrency(context, workOrderForm.getTotalCost()));
        if (workOrderForm.getImageCounter() > 0) {
            holder.imageCounter.setText(context.getResources().getQuantityString(
                    R.plurals.content_details_rv_item_image_counter,
                    workOrderForm.getImageCounter(),
                    workOrderForm.getImageCounter()));
        } else {
            holder.imageCounter.setText("");
        }
    }

    @Override
    public void onDataChanged() {
        this.mSubtitle.setVisibility(getItemCount() != 0 ? View.VISIBLE : View.GONE);
        this.mEmptyText.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    class WorkOrderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView issue;
        final TextView date;
        final TextView totalCost;
        final TextView imageCounter;
        final ImageView moreOptions;

        WorkOrderHolder(View itemView) {
            super(itemView);

            this.issue = itemView.findViewById(R.id.content_details_wo_rv_item_issue);
            this.date = itemView.findViewById(R.id.content_details_wo_rv_item_date);
            this.totalCost = itemView.findViewById(R.id.content_details_wo_rv_item_total_cost);
            this.imageCounter = itemView.findViewById(R.id.content_details_wo_rv_item_image_counter);
            this.moreOptions = itemView.findViewById(R.id.content_details_wo_rv_iv_more_options);

            itemView.setOnClickListener(this);
            this.moreOptions.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == this.getItemId()) {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(adapterPosition);
                mOnClickListener.onShowEditWorkOrderDialog(documentSnapshot.getId(), getItem(adapterPosition));
            } else if (v.getId() == this.moreOptions.getId()) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.inflate(R.menu.menu_wo_item_more_options);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        int adapterPosition = getAdapterPosition();
                        if (adapterPosition == RecyclerView.NO_POSITION) {
                            return false;
                        }

                        DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(adapterPosition);
                        switch (id) {
                            case R.id.menu_wo_item_edit_cost_sheet:
                                mOnClickListener.onShowEditCostSheetDialog(documentSnapshot.getId(), getItem(adapterPosition));
                                return true;
                            case R.id.menu_wo_item_delete_wo:
                                mOnClickListener.onDeleteWorkOrder(documentSnapshot.getId(), getItem(adapterPosition));
                                return true;
                            case R.id.menu_wo_item_upload_images:
                                mOnClickListener.onShowEditUploadedImagesDialog(documentSnapshot.getId(), getItem(adapterPosition));
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