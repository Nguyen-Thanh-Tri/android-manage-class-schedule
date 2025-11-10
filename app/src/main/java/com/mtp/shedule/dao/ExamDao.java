package com.mtp.shedule.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mtp.shedule.entity.ExamEntity;
import java.util.List;

@Dao
public interface ExamDao {
    @Insert
    void insert(ExamEntity exam);

    @Delete
    void delete(ExamEntity exam);

    @Update
    void update(ExamEntity exam);

    @Query("SELECT * FROM exam")
    LiveData<List<ExamEntity>> getAllExams();
}
