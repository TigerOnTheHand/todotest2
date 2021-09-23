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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        Intent intent = getIntent();
    }

    public void onAddTaskButton(View view) {

    }

    @UiThread
    private void AsyncUpdate(TaskDao taskDao) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundTaskUpdate backgroundTaskUpdate = new BackgroundTaskUpdate(handler, taskDao);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTaskUpdate);
    };

    private class BackgroundTaskUpdate implements Runnable {
        private final Handler _handler;
        private final TaskDao _taskDao;

        public BackgroundTaskUpdate(Handler handler, TaskDao taskDao) {
            _handler = handler;
            _taskDao = taskDao;
        }

        @WorkerThread
        @Override
        public void run() {
            // データを追加
            //_taskDao.insert(_taskId, _taskName, note);

            UpdatePostExector updatePostExector = new UpdatePostExector();
            _handler.post(updatePostExector);
        }
    }

    private class UpdatePostExector implements Runnable {
        @UiThread
        @Override
        public void run() {
            finish();
        }
    }

    public void onBackToMenuButton(View view) {
        finish();
    }
}