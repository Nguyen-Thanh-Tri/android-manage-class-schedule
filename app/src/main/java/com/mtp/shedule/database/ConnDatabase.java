package com.mtp.shedule.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mtp.shedule.dao.TeacherDao;
import com.mtp.shedule.entity.CourseEntity;
import com.mtp.shedule.entity.ExamEntity;
import com.mtp.shedule.dao.CourseDao;
import com.mtp.shedule.dao.ExamDao;
import com.mtp.shedule.entity.TeacherEntity;

@Database(
        entities = {CourseEntity.class, ExamEntity.class, TeacherEntity.class}, version = 3)
public abstract class ConnDatabase extends RoomDatabase {
    public abstract CourseDao courseDao();
    public abstract ExamDao examDao();
    public abstract TeacherDao teacherDao();


    private static volatile ConnDatabase instance;

    public static ConnDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ConnDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    ConnDatabase.class, "course_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}