package com.kadaikenkyu.todotest;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "year")
    public int year;
    @ColumnInfo(name = "monthOfYear")
    public int monthOfYear;
    @ColumnInfo(name = "dayOfMonth")
    public int dayOfMonth;

    @ColumnInfo(name = "blockSize")
    public int blockSize;

    @ColumnInfo(name = "color")
    public String color;

    @ColumnInfo(name = "sintyoku")
    public int sintyoku;

    public Task(String name, String note, int year, int monthOfYear, int dayOfMonth, int blockSize) {
        this.name = name;
        this.note = note;
        this.year = year;
        this.monthOfYear = monthOfYear;
        this.dayOfMonth = dayOfMonth;
        this.blockSize = blockSize;
    }

    public int Id() {
        return id;
    }
}
