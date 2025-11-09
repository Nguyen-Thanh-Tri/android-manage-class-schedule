package com.mtp.shedule.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mtp.shedule.entity.TeacherEntity;

import java.util.List;

@Dao
public interface TeacherDao {

    @Query("SELECT * FROM teacher ORDER BY id DESC")
    LiveData<List<TeacherEntity>> getAllTeachers();

    @Insert
    void insertTeacher(TeacherEntity teacher);

    @Update
    void updateTeacher(TeacherEntity teacher);

    @Delete
    void deleteTeacher(TeacherEntity teacher);
}
