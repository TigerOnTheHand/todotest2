package com.example.todotest;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    Task findById(int id);

    //@Query("INSERT INTO tasks (id, name, note) VALUES (:id, :name, :note)")
    //void insert(int id, String name, String note);

    @Query("DELETE FROM tasks WHERE id = :id")
    void delete(int id);

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Insert
    void insert(Task task);

    @Query("SELECT * FROM tasks")
    List<Task> getAll();

    @Query("UPDATE tasks SET sintyoku = :sintyoku WHERE id = :id")
    void updateSintyoku(int id, int sintyoku);

    // id指定しなくてよくね？
}
