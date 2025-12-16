package com.mtp.shedule.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mtp.shedule.entity.EventEntity;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    long insertEvent(EventEntity event);

    @Query("SELECT * FROM events WHERE startTime BETWEEN :start AND :end")
    List<EventEntity> getEventsInRange(long start, long end);

    @Query("SELECT * FROM events WHERE date(startTime/1000,'unixepoch', 'localtime') = date(:day/1000,'unixepoch', 'localtime')")
    List<EventEntity> getEventsByDay(long day);

    @Query("SELECT * FROM events " +
            "WHERE strftime('%Y', startTime/1000, 'unixepoch', 'localtime') = :year " +
            "AND strftime('%m', startTime/1000, 'unixepoch', 'localtime') = :month")
    List<EventEntity> getEventsByMonth(String year, String month);

    @Delete
    void deleteEvent(EventEntity event);
    @Update
    void updateEvent(EventEntity event);
    @Query("SELECT * FROM events WHERE startTime BETWEEN :weekStart AND :weekEnd")
    List<EventEntity> getEventsByWeek(long weekStart, long weekEnd);
    
    // Methods for repeating events
    @Query("SELECT * FROM events WHERE repeatType = 'weekly'")
    List<EventEntity> getWeeklyRepeatingEvents();
    
    @Query("SELECT * FROM events WHERE isCourse = 1")
    List<EventEntity> getCourseEvents();
    
    @Query("SELECT * FROM events WHERE repeatType != 'none' AND repeatType IS NOT NULL")
    List<EventEntity> getAllRepeatingEvents();
    
    @Query("SELECT * FROM events WHERE id = :id")
    EventEntity getEventById(int id);

    //BOOT RECEIVER
    @Query("SELECT * FROM events")
    List<EventEntity> getAllEvents();

    @Query("SELECT * FROM events WHERE isCourse = 1 AND LOWER(dayOfWeek) = LOWER(:dayOfWeek) ORDER BY startTime ASC")
    LiveData<List<EventEntity>> getCoursesByDay(String dayOfWeek);
}
