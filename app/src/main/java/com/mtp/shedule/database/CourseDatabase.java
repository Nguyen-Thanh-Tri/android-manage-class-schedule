package com.mtp.shedule.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mtp.shedule.entity.CourseEntity;
import com.mtp.shedule.dao.CourseDao;

@Database(entities = {CourseEntity.class}, version = 1)
public abstract class CourseDatabase extends RoomDatabase {
    public abstract CourseDao courseDao();

    private static volatile CourseDatabase instance;

    public static CourseDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CourseDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    CourseDatabase.class, "course_database")
                            .build();
                }
            }
        }
        return instance;
    }
}

