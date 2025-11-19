package com.pranav.synctask.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pranav.synctask.R;
import com.pranav.synctask.models.DialogItem;
import com.pranav.synctask.models.Space;
import java.util.List;

public class SpaceSelectionAdapter extends RecyclerView.Adapter<SpaceSelectionAdapter.ViewHolder> {

    private final List<DialogItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DialogItem item);
    }

    public SpaceSelectionAdapter(List<DialogItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_space_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DialogItem item = items.get(position);
        holder.tvName.setText(item.getDisplayName());

        if (Space.TYPE_PERSONAL.equals(item.getSpaceType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_profile);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_scope_shared);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_space_name);
            ivIcon = itemView.findViewById(R.id.iv_space_icon);
        }
    }
}