package com.akstarcompany.salony.ai.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.akstarcompany.salony.ai.model.MemoryItem;

import java.util.ArrayList;
import java.util.List;

public class MemoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "salony_memory.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_MEMORY = "memories";
    private static final String COL_ID = "_id";
    private static final String COL_KEY = "topic_key";
    private static final String COL_VAL = "detail_val";
    private static final String COL_CAT = "category";
    private static final String COL_TIMESTAMP = "timestamp";

    private static MemoryDatabaseHelper instance;

    public static synchronized MemoryDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MemoryDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private MemoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MEMORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_KEY + " TEXT NOT NULL, " +
                COL_VAL + " TEXT NOT NULL, " +
                COL_CAT + " TEXT, " +
                COL_TIMESTAMP + " INTEGER NOT NULL" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMORY);
        onCreate(db);
    }

    public long insertMemory(MemoryItem memory) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEY, memory.getKey());
        values.put(COL_VAL, memory.getValue());
        values.put(COL_CAT, memory.getCategory() != null ? memory.getCategory() : "General");
        values.put(COL_TIMESTAMP, memory.getTimestamp());

        long id = db.insert(TABLE_MEMORY, null, values);
        memory.setId(id);
        return id;
    }

    public int updateMemory(MemoryItem memory) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEY, memory.getKey());
        values.put(COL_VAL, memory.getValue());
        values.put(COL_CAT, memory.getCategory() != null ? memory.getCategory() : "General");

        return db.update(TABLE_MEMORY, values, COL_ID + "=?", new String[]{String.valueOf(memory.getId())});
    }

    public int deleteMemory(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_MEMORY, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAllMemories() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MEMORY, null, null);
    }

    public List<MemoryItem> getAllMemories() {
        List<MemoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEMORY, null, null, null, null, null, COL_TIMESTAMP + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COL_KEY));
                String val = cursor.getString(cursor.getColumnIndexOrThrow(COL_VAL));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));

                list.add(new MemoryItem(id, key, val, cat, timestamp));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<MemoryItem> searchMemories(String query) {
        List<MemoryItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = COL_KEY + " LIKE ? OR " + COL_VAL + " LIKE ? OR " + COL_CAT + " LIKE ?";
        String wild = "%" + query + "%";
        Cursor cursor = db.query(TABLE_MEMORY, null, selection, new String[]{wild, wild, wild}, null, null, COL_TIMESTAMP + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COL_KEY));
                String val = cursor.getString(cursor.getColumnIndexOrThrow(COL_VAL));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));

                list.add(new MemoryItem(id, key, val, cat, timestamp));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }
}
