package com.akstarcompany.salony.ai;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akstarcompany.salony.ai.db.MemoryDatabaseHelper;
import com.akstarcompany.salony.ai.model.MemoryItem;
import com.akstarcompany.salony.ai.ui.MemoryAdapter;
import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class MemoryActivity extends AppCompatActivity {

    private MemoryDatabaseHelper dbHelper;
    private RecyclerView rvMemories;
    private MemoryAdapter adapter;
    private final List<MemoryItem> memoryList = new ArrayList<>();
    private TextView tvMemoryCount;
    private LinearLayout layoutEmpty;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        dbHelper = MemoryDatabaseHelper.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btnBackMemory);
        ImageButton btnClearAll = findViewById(R.id.btnClearAllMemories);
        Button btnAdd = findViewById(R.id.btnAddMemory);
        etSearch = findViewById(R.id.etSearchMemory);
        tvMemoryCount = findViewById(R.id.tvMemoryCount);
        layoutEmpty = findViewById(R.id.layoutEmptyMemories);
        rvMemories = findViewById(R.id.rvMemories);

        rvMemories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemoryAdapter(memoryList, new MemoryAdapter.OnMemoryClickListener() {
            @Override
            public void onEdit(MemoryItem memory) {
                showEditDialog(memory);
            }

            @Override
            public void onDelete(MemoryItem memory) {
                dbHelper.deleteMemory(memory.getId());
                loadMemories();
                Toast.makeText(MemoryActivity.this, "Memory deleted", Toast.LENGTH_SHORT).show();
            }
        });
        rvMemories.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> showAddDialog());

        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Memories?")
                    .setMessage("Kya aap sach me saari saved memories delete karna chahte ho? Salony aapke facts bhool jayegi.")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        dbHelper.clearAllMemories();
                        loadMemories();
                        Toast.makeText(this, "All memories cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMemories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadMemories();
    }

    private void loadMemories() {
        memoryList.clear();
        memoryList.addAll(dbHelper.getAllMemories());
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void filterMemories(String query) {
        memoryList.clear();
        if (query.trim().isEmpty()) {
            memoryList.addAll(dbHelper.getAllMemories());
        } else {
            memoryList.addAll(dbHelper.searchMemories(query.trim()));
        }
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        tvMemoryCount.setText(memoryList.size() + " facts saved");
        if (memoryList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvMemories.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvMemories.setVisibility(View.VISIBLE);
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_memory, null);
        EditText etKey = dialogView.findViewById(R.id.etMemoryKey);
        EditText etVal = dialogView.findViewById(R.id.etMemoryValue);
        Button btnSave = dialogView.findViewById(R.id.btnSaveMemory);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelMemory);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                dbHelper.insertMemory(new MemoryItem(key, val, "Personal"));
                dialog.dismiss();
                loadMemories();
                Toast.makeText(this, "Memory saved! ❤️", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEditDialog(MemoryItem memory) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_memory, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("Edit Memory");
        EditText etKey = dialogView.findViewById(R.id.etMemoryKey);
        EditText etVal = dialogView.findViewById(R.id.etMemoryValue);
        Button btnSave = dialogView.findViewById(R.id.btnSaveMemory);
        btnSave.setText("Update");
        Button btnCancel = dialogView.findViewById(R.id.btnCancelMemory);

        etKey.setText(memory.getKey());
        etVal.setText(memory.getValue());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String key = etKey.getText().toString().trim();
            String val = etVal.getText().toString().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                memory.setKey(key);
                memory.setValue(val);
                dbHelper.updateMemory(memory);
                dialog.dismiss();
                loadMemories();
                Toast.makeText(this, "Memory updated!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
