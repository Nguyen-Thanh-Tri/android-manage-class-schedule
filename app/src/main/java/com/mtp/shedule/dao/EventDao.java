package com.mtp.shedule.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.mtp.shedule.entity.EventEntity;

import java.util.List;

@Dao
public interface EventDao {

    @Insert
    void insertEvent(EventEntity event);

    @Query("SELECT * FROM events WHERE startTime BETWEEN :start AND :end")
    List<EventEntity> getEventsInRange(long start, long end);

    @Query("SELECT * FROM events WHERE date(startTime/1000,'unixepoch', 'localtime') = date(:day/1000,'unixepoch', 'localtime')")
    List<EventEntity> getEventsByDay(long day);

    @Delete
    void deleteEvent(EventEntity event);
}
