package com.pranav.synctask.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pranav.synctask.R;
import com.pranav.synctask.models.Subtask;
import java.util.List;

public class CreateSubtaskAdapter extends RecyclerView.Adapter<CreateSubtaskAdapter.ViewHolder> {

    private final List<Subtask> subtasks;

    public CreateSubtaskAdapter(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_create_subtask, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // We don't bind text here to avoid focus issues with TextWatcher during scrolls,
        // but for a small list, we manually set it once.
        // To prevent index issues, we use the holder position carefully.

        // Important: Remove listener before setting text to avoid loops
        if (holder.textWatcher != null) {
            holder.etTitle.removeTextChangedListener(holder.textWatcher);
        }

        holder.etTitle.setText(subtasks.get(position).getTitle());

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    subtasks.get(pos).setTitle(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        holder.etTitle.addTextChangedListener(holder.textWatcher);

        holder.ivRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                subtasks.remove(pos);
                notifyItemRemoved(pos);
            }
        });

        // Auto-focus on the last added item if it's empty (UX improvement)
        if (position == subtasks.size() - 1 && holder.etTitle.getText().length() == 0) {
            holder.etTitle.requestFocus();
        }
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }

    public void addSubtask() {
        subtasks.add(new Subtask(""));
        notifyItemInserted(subtasks.size() - 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        EditText etTitle;
        ImageView ivRemove;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            etTitle = itemView.findViewById(R.id.et_subtask_title);
            ivRemove = itemView.findViewById(R.id.iv_remove_subtask);
        }
    }
}