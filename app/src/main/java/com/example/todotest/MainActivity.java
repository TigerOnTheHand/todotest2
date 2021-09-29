package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    List<Task> tasks;
    TaskDao taskDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        taskDao = db.taskDao();

        // ブロック表示
        AsyncShowBlock();
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
            // データ取得
            tasks.clear();
            tasks = taskDao.getAll();



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

    public void GoToAddTask(View view) {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        startActivity(intent);
    }
}