package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryEntity

@Database(entities = [ChatMessage::class, MemoryEntity::class], version = 1, exportSchema = false)
abstract class SalonyDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: SalonyDatabase? = null

        fun getDatabase(context: Context): SalonyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalonyDatabase::class.java,
                    "salony_ai_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
