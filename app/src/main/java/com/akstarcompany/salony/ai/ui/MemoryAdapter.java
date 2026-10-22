package com.akstarcompany.salony.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akstarcompany.salony.ai.model.MemoryItem;
import com.example.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    public interface OnMemoryClickListener {
        void onEdit(MemoryItem memory);
        void onDelete(MemoryItem memory);
    }

    private final List<MemoryItem> memoryList;
    private final OnMemoryClickListener listener;
    private final SimpleDateFormat dateFormat;

    public MemoryAdapter(List<MemoryItem> memoryList, OnMemoryClickListener listener) {
        this.memoryList = memoryList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        MemoryItem item = memoryList.get(position);
        holder.tvKey.setText(item.getKey());
        holder.tvValue.setText(item.getValue());
        holder.tvCategory.setText(item.getCategory() != null ? item.getCategory() : "General");
        holder.tvDate.setText(dateFormat.format(new Date(item.getTimestamp())));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return memoryList.size();
    }

    static class MemoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvKey;
        TextView tvValue;
        TextView tvCategory;
        TextView tvDate;
        ImageButton btnEdit;
        ImageButton btnDelete;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKey = itemView.findViewById(R.id.tvMemoryKey);
            tvValue = itemView.findViewById(R.id.tvMemoryValue);
            tvCategory = itemView.findViewById(R.id.tvMemoryCategory);
            tvDate = itemView.findViewById(R.id.tvMemoryDate);
            btnEdit = itemView.findViewById(R.id.btnEditMemory);
            btnDelete = itemView.findViewById(R.id.btnDeleteMemory);
        }
    }
}
