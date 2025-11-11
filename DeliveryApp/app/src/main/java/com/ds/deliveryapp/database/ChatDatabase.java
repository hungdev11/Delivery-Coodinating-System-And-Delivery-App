package com.ds.deliveryapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ds.deliveryapp.database.dao.ChatMessageDao;
import com.ds.deliveryapp.database.entities.ChatMessageEntity;

/**
 * Room Database for local chat storage
 */
@Database(entities = {ChatMessageEntity.class}, version = 1, exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "chat_database";
    private static ChatDatabase instance;
    
    public abstract ChatMessageDao chatMessageDao();
    
    public static synchronized ChatDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    ChatDatabase.class,
                    DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build();
        }
        return instance;
    }
}
