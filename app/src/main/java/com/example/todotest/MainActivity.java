package com.example.todotest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
        tasks = new ArrayList<Task>();

    }

    @Override
    protected void onStart() {
        super.onStart();

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

            // tasksを期日が近い順に並べ替え
            tasks = SortInOrderOfDate(tasks);

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
            TextView text = findViewById(R.id.textView);
            String str = "";
            for (Task task : tasks) {
                str += task.name + ",";
            }
            text.setText(str);
        }
    }

    public void GoToAddTask(View view) {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        startActivity(intent);
    }

    // tasksを期日が近い順に並べ替え
    public List<Task> SortInOrderOfDate(List<Task> tasks) {
        boolean isSorted = false;
        while (!isSorted) {
            boolean isChangedOrder = false;
            for(int i = 0;i < tasks.size() - 1;i++) {
                // 年を見て入れ替え
                if (tasks.get(i).year > tasks.get(i + 1).year) {
                    Task w = tasks.get(i);
                    tasks.set(i, tasks.get(i + 1));
                    tasks.set(i + 1, w);
                    isChangedOrder = true;
                }
                else if(tasks.get(i).year == tasks.get(i + 1).year) {
                    // 年が同じ→月を見て入れ替え
                    if (tasks.get(i).monthOfYear > tasks.get(i + 1).monthOfYear) {
                        Task w = tasks.get(i);
                        tasks.set(i, tasks.get(i + 1));
                        tasks.set(i + 1, w);
                        isChangedOrder = true;
                    }
                }
                else if(tasks.get(i).monthOfYear == tasks.get(i + 1).monthOfYear) {
                    // 月が同じ→日を見て入れ替え
                    if (tasks.get(i).dayOfMonth > tasks.get(i + 1).dayOfMonth) {
                        Task w = tasks.get(i);
                        tasks.set(i, tasks.get(i + 1));
                        tasks.set(i + 1, w);
                        isChangedOrder = true;
                    }
                }
            }

            if (!isChangedOrder) {
                isSorted = true;
            }
        }

        return tasks;
    }
}