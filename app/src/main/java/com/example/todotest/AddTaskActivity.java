package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener{
    Task task;
    TextView txtDate;
    EditText editTaskName, editTaskNote, editTaskBlockSize;
    int year, monthOfYear, dayOfMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        Intent intent = getIntent();
        task = new Task("default", "default", 9999, 12, 31, 2);

        txtDate = findViewById(R.id.txtDate);
        editTaskName = findViewById(R.id.editTaskName);
        editTaskNote = findViewById(R.id.editTaskNote);
        editTaskBlockSize = findViewById(R.id.editTaskBlockSize);

        //今日の日付を取得(日本のタイムゾーン)
        Calendar todayDateCalendar =
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));

        //textViewへ文字列にしてセット
        year = todayDateCalendar.get(Calendar.YEAR);
        monthOfYear = todayDateCalendar.get(Calendar.MONTH) + 1;
        dayOfMonth = todayDateCalendar.get(Calendar.DAY_OF_MONTH);
        String date = "期限：" +
                String.valueOf(year) +
                "/ " + String.valueOf(monthOfYear) +
                "/ " + String.valueOf(dayOfMonth);
        txtDate.setText(date);

        // あとまわしになってる
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("赤");
        adapter.add("青");
        adapter.add("緑");
        adapter.add("O型");
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
    }

    // 課題追加ボタン
    public void AddTask(View view) {
        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        AsyncAddTask(db.taskDao());
    }

    // 課題追加非同期処理準備
    @UiThread
    private void AsyncAddTask(TaskDao taskDao) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundAddTask backgroundAddTask = new BackgroundAddTask(handler, taskDao);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundAddTask);
    };

    // 課題追加非同期処理
    private class BackgroundAddTask implements Runnable {
        private final Handler _handler;
        private final TaskDao _taskDao;

        public BackgroundAddTask(Handler handler, TaskDao taskDao) {
            _handler = handler;
            _taskDao = taskDao;
        }

        @WorkerThread
        @Override
        public void run() {
            task.name = editTaskName.getText().toString();
            task.note = editTaskNote.getText().toString();
            task.year = year;
            task.monthOfYear = monthOfYear;
            task.dayOfMonth = dayOfMonth;
            task.blockSize = Integer.parseInt(editTaskBlockSize.getText().toString());
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            task.color = (String) spinner.getSelectedItem();

            // データを追加
            _taskDao.insert(task);

            AddTaskPostExector addTaskPostExector = new AddTaskPostExector();
            _handler.post(addTaskPostExector);
        }
    }

    // 課題追加非同期処理終了後の処理
    private class AddTaskPostExector implements Runnable {
        @UiThread
        @Override
        public void run() {
            // 課題追加アクティビティーを終了
            Toast.makeText(getApplicationContext() , "課題を追加しました", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // 日付変更時に再表示
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        String date = "期限：" +
                String.valueOf(year) +
                "/ " + String.valueOf(monthOfYear + 1) +
                "/ " + String.valueOf(dayOfMonth);
        txtDate.setText(date);

        this.year = year;
        this.monthOfYear = monthOfYear + 1;
        this.dayOfMonth = dayOfMonth;
    }

    // カレンダー型デイトピッカーを表示
    public void ShowDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePick();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    // メニューに戻る
    public void BackToMenuButton(View view) {
        finish();
    }
}