package com.mtp.shedule;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.mtp.shedule.dao.CourseDao;
import com.mtp.shedule.database.CourseDatabase;
import com.mtp.shedule.entity.CourseEntity;

import java.util.List;

public class CourseRepository {
    private CourseDao courseDao;

    public CourseRepository(Context context) {
        CourseDatabase db = CourseDatabase.getInstance(context);
        courseDao = db.courseDao();
    }

    public void insert(CourseEntity course) {
        new Thread(() -> courseDao.insert(course)).start();
    }

    public void delete(CourseEntity course) {
        new Thread(() -> courseDao.delete(course)).start();
    }

    public LiveData<List<CourseEntity>> getAllCourses() {
        return courseDao.getAllCourses();
    }

    public LiveData<List<CourseEntity>> getCoursesByDay(String day) {
        return courseDao.getCoursesByDay(day);
    }
}
