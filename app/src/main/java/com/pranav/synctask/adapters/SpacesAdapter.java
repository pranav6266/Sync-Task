package com.pranav.synctask.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.synctask.R;
import com.pranav.synctask.activities.MainActivity;
import com.pranav.synctask.models.Space;

import java.util.List;

public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.SpaceViewHolder> {

    private final Context context;
    private final List<Space> spaceList;

    public SpacesAdapter(Context context, List<Space> spaceList) {
        this.context = context;
        this.spaceList = spaceList;
    }

    @NonNull
    @Override
    public SpaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_space, parent, false);
        return new SpaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpaceViewHolder holder, int position) {
        Space space = spaceList.get(position);
        holder.tvSpaceName.setText(space.getSpaceName());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("SPACE_ID", space.getSpaceId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    public void updateSpaces(List<Space> newSpaces) {
        this.spaceList.clear();
        this.spaceList.addAll(newSpaces);
        notifyDataSetChanged();
    }

    static class SpaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpaceName;

        public SpaceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpaceName = itemView.findViewById(R.id.tv_space_name);
        }
    }
}