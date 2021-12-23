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
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoTaskActivity extends AppCompatActivity {
    TaskDao taskDao;
    int taskID = -1;
    String taskName;
    int taskSintyoku;
    Task task;
    TextView text_Sintyoku;
    SeekBar seekBar;
    int _progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_do_task);

        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        taskDao = db.taskDao();

        text_Sintyoku = findViewById(R.id.txtTaskSintyoku);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミがドラッグされると呼ばれる
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (task.sintyoku + progress == task.blockSize * task.blockSize) {
                            text_Sintyoku.setText("課題完了！");
                        } else {
                            text_Sintyoku.setText(String.valueOf(task.sintyoku + progress) + "/" + String.valueOf(task.blockSize * task.blockSize) + "個削る");
                        }
                        _progress = progress;
                    }

                    //ツマミがタッチされた時に呼ばれる
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    //ツマミがリリースされた時に呼ばれる
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                });

        Intent intent = getIntent();
        taskID = intent.getIntExtra("TASKID", -1);
        AsyncGetTask();
    }

    // タスク取得準備処理
    @UiThread
    private void AsyncGetTask() {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundGetTask backgroundGetTask = new BackgroundGetTask(handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundGetTask);
    }

    // タスク取得非同期処理
    private class BackgroundGetTask implements Runnable {
        private final Handler _handler;

        public BackgroundGetTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            task = taskDao.findById(taskID);

            GetTaskPostExector getTaskPostExector = new GetTaskPostExector();
            _handler.post(getTaskPostExector);
        }
    }

    // タスク取得非同期処理終了後の処理
    private class GetTaskPostExector implements Runnable {

        public GetTaskPostExector() { }

        @UiThread
        @Override
        public void run() {
            TextView text = findViewById(R.id.txtTaskNameDo);
            text.setText(task.name);
            //taskSintyoku = task.sintyoku;
            text_Sintyoku.setText(String.valueOf(task.sintyoku) + "/" + String.valueOf(task.blockSize * task.blockSize) + "個削る");
            seekBar.setProgress(0);
            seekBar.setMax(task.blockSize * task.blockSize - task.sintyoku);
        }
    }

    // 課題進めるボタン
    public void DoTask(View view) {
        AsyncDoTask();
    }

    // タスク進める準備処理
    @UiThread
    private void AsyncDoTask() {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundDoTask backgroundDoTask = new BackgroundDoTask(handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundDoTask);
    }

    // タスク進める非同期処理
    private class BackgroundDoTask implements Runnable {
        private final Handler _handler;

        public BackgroundDoTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            task.sintyoku = task.sintyoku + _progress;
            if (task.sintyoku == task.blockSize * task.blockSize) {
                taskDao.delete(taskID);
            }
            taskDao.updateSintyoku(taskID, task.sintyoku);

            DoTaskPostExector doTaskPostExector = new DoTaskPostExector();
            _handler.post(doTaskPostExector);
        }
    }

    // タスク進める非同期処理終了後の処理
    private class DoTaskPostExector implements Runnable {

        public DoTaskPostExector() { }

        @UiThread
        @Override
        public void run() {
            Toast.makeText(getApplicationContext() , task.sintyoku + "個ブロックを削った！", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // メニューに戻る
    public void BackToMenuButton(View view) {
        finish();
    }
}