package com.mtp.shedule.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mtp.shedule.entity.CourseEntity;

import java.util.List;

@Dao
public interface CourseDao {

    @Insert
    void insert(CourseEntity course);

    @Delete
    void delete(CourseEntity course);

    @Query("SELECT * FROM course")
    LiveData<List<CourseEntity>> getAllCourses();

    @Query("SELECT * FROM course WHERE dayOfWeek = :day")
    LiveData<List<CourseEntity>> getCoursesByDay(String day);

    @Query("SELECT * FROM course WHERE id = :id LIMIT 1")
    CourseEntity getCourseById(int id);

    @Update
    void update(CourseEntity course);
}

