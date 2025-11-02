package com.mtp.shedule.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mtp.shedule.entity.CourseEntity;
import com.mtp.shedule.entity.ExamEntity;
import com.mtp.shedule.dao.CourseDao;
import com.mtp.shedule.dao.ExamDao;

@Database(
         entities = {CourseEntity.class, ExamEntity.class}, version = 2)
public abstract class CourseDatabase extends RoomDatabase {
    public abstract CourseDao courseDao();
    public abstract ExamDao examDao();

    private static volatile CourseDatabase instance;

    public static CourseDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (CourseDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    CourseDatabase.class, "course_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}

