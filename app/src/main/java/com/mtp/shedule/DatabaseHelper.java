package com.mtp.shedule;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "timetable.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_COURSE = "course";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TEACHER = "teacher";
    private static final String COLUMN_ROOM = "room";
    private static final String COLUMN_START = "start_time";
    private static final String COLUMN_END = "end_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE =
                "CREATE TABLE " + TABLE_COURSE + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TITLE + " TEXT, " +
                        COLUMN_TEACHER + " TEXT, " +
                        COLUMN_ROOM + " TEXT, " +
                        COLUMN_START + " TEXT, " +
                        COLUMN_END + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSE);
        onCreate(db);
    }

    // ✅ Thêm môn học
    public void addCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, course.getTitle());
        values.put(COLUMN_TEACHER, course.getTeacher());
        values.put(COLUMN_ROOM, course.getRoom());
        values.put(COLUMN_START, course.getTimeStart());
        values.put(COLUMN_END, course.getTimeEnd());

        db.insert(TABLE_COURSE, null, values);
        db.close();
    }

    //  Lấy toàn bộ danh sách môn học
    public List<Course> getAllCourses() {
        List<Course> courseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COURSE, null);
        if (cursor.moveToFirst()) {
            do {
                Course course = new Course(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );
                courseList.add(course);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return courseList;
    }
    //Xóa 1 card
    public void deleteCourse(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Course", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
}
