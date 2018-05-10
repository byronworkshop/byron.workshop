package com.byronworkshop.shared.dialogs.adapters.workordercost;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.byronworkshop.R;
import com.byronworkshop.shared.dialogs.adapters.workordercost.pojo.Cost;
import com.byronworkshop.utils.DecimalFormatterUtils;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class CostSheetRVAdapter extends FirestoreRecyclerAdapter<Cost, CostSheetRVAdapter.CostSheetHolder> {

    private final DeleteItemClickListener mListener;
    private final LinearLayout mEmptyText;

    public interface DeleteItemClickListener {
        void onDeleteCost(String costId, Cost cost);
    }

    public CostSheetRVAdapter(@NonNull FirestoreRecyclerOptions<Cost> options,
                              @NonNull DeleteItemClickListener listener,
                              @NonNull LinearLayout emptyText) {
        super(options);

        this.mListener = listener;
        this.mEmptyText = emptyText;
    }

    @NonNull
    @Override
    public CostSheetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.dialog_edit_cost_sheet_rv_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new CostSheetHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CostSheetHolder holder, int position, @NonNull Cost cost) {
        switch (cost.getType()) {
            case Cost.TYPE_EXTERNAL:
                holder.costType.setImageResource(R.drawable.ic_cost_external);
                break;
            case Cost.TYPE_REPLACEMENT:
                holder.costType.setImageResource(R.drawable.ic_cost_replacement);
                break;
            case Cost.TYPE_LABOR_COST:
                holder.costType.setImageResource(R.drawable.ic_cost_labor);
                break;
            default:
                holder.costType.setImageResource(R.drawable.ic_money);
                break;
        }

        holder.description.setText(cost.getDescription());
        holder.cost.setText(DecimalFormatterUtils.formatCurrency(holder.cost.getContext(), cost.getAmount()));
    }

    @Override
    public void onDataChanged() {
        this.mEmptyText.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    class CostSheetHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView costType;
        final TextView description;
        final TextView cost;
        final ImageView delete;

        CostSheetHolder(View itemView) {
            super(itemView);

            this.costType = itemView.findViewById(R.id.cost_sheet_rv_item_type);
            this.description = itemView.findViewById(R.id.cost_sheet_rv_item_description);
            this.cost = itemView.findViewById(R.id.cost_sheet_rv_item_amount);
            this.delete = itemView.findViewById(R.id.cost_sheet_rv_item_delete);

            this.delete.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == this.delete.getId()) {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                DocumentSnapshot documentSnapshot = getSnapshots().getSnapshot(adapterPosition);
                mListener.onDeleteCost(documentSnapshot.getId(), getItem(adapterPosition));
            }
        }
    }
}