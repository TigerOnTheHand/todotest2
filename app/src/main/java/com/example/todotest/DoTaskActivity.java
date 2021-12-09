package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoTaskActivity extends AppCompatActivity {
    TaskDao taskDao;
    int taskID = -1;

    // ここから findだるいいいい
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_task);

        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        taskDao = db.taskDao();

        Intent intent = getIntent();
        taskID = intent.getIntExtra("TASKID", -1);
        Toast.makeText(getApplicationContext() , String.valueOf(taskDao.findById(taskID).name), Toast.LENGTH_SHORT).show();
    }

    // ブロック表示非同期処理準備
    @UiThread
    private void AsyncShowBlock() {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundShowBlock backgroundShowBlock = new BackgroundShowBlock(handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundShowBlock);
    }

    // ブロック表示非同期処理
    private class BackgroundShowBlock implements Runnable {
        private final Handler _handler;

        public BackgroundShowBlock(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            ShowBlockPostExector showBlockPostExector = new ShowBlockPostExector();
            _handler.post(showBlockPostExector);
        }
    }

    // ブロック表示非同期処理終了後の処理
    private class ShowBlockPostExector implements Runnable {

        public ShowBlockPostExector() { }

        @UiThread
        @Override
        public void run() {

        }
    }
}