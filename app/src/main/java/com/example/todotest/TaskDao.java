package com.example.todotest;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    Task findById(int id);

    @Query("INSERT INTO tasks (id, name, note) VALUES (:id, :name, :note)")
    void insert(int id, String name, String note);

    @Query("DELETE FROM tasks WHERE id = :id")
    void delete(int id);

    // 問題
    // 主キーがないとidを指定してtaskを取り出せない
    // なのでtask追加時に主キーを「課題の数 + 1」と設定する必要がある
}
