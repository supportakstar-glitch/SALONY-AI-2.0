package com.akstarcompany.salony.ai.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.akstarcompany.salony.ai.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "salony_chat.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CHAT = "chat_messages";
    private static final String COL_ID = "_id";
    private static final String COL_TEXT = "message_text";
    private static final String COL_IS_USER = "is_user";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_ACTION_TAG = "action_tag";
    private static final String COL_ACTION_LABEL = "action_label";

    private static ChatDatabaseHelper instance;

    public static synchronized ChatDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ChatDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CHAT + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TEXT + " TEXT NOT NULL, " +
                COL_IS_USER + " INTEGER NOT NULL, " +
                COL_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_ACTION_TAG + " TEXT, " +
                COL_ACTION_LABEL + " TEXT" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        onCreate(db);
    }

    public long insertMessage(ChatMessage message) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TEXT, message.getText());
        values.put(COL_IS_USER, message.isUser() ? 1 : 0);
        values.put(COL_TIMESTAMP, message.getTimestamp());
        values.put(COL_ACTION_TAG, message.getActionTag());
        values.put(COL_ACTION_LABEL, message.getActionLabel());

        long id = db.insert(TABLE_CHAT, null, values);
        message.setId(id);
        return id;
    }

    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHAT, null, null, null, null, null, COL_TIMESTAMP + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT));
                boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_USER)) == 1;
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
                String tag = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION_TAG));
                String label = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION_LABEL));

                list.add(new ChatMessage(id, text, isUser, timestamp, tag, label));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public void clearAllMessages() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CHAT, null, null);
    }

    public void deleteMessage(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CHAT, COL_ID + "=?", new String[]{String.valueOf(id)});
    }
}
