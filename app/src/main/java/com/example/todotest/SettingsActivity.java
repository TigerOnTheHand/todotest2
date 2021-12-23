package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);

        //add some items
        adapter.add("課題管理アプリの使い方");
        adapter.add("このアプリについて");
        adapter.add("通知設定");
        adapter.add("プライバシーポリシー");
        adapter.add("タスクを全て削除");
        ListView listView = (ListView) findViewById(R.id.list_view);

        //ListView set ArrayAdapter
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView
                ListView listView = (ListView)parent;
                // 選択された項目
                String selectedItemStr = (String)listView.getItemAtPosition(position);

                if (selectedItemStr == "課題管理アプリの使い方") {
                    Toast.makeText(getApplicationContext(), "まだ実装してないんよー", Toast.LENGTH_SHORT).show();
                }
                if (selectedItemStr == "このアプリについて") {
                    Toast.makeText(getApplicationContext(), "まだ実装してないんよー", Toast.LENGTH_SHORT).show();
                }
                if (selectedItemStr == "通知設定") {
                    Intent intent = new Intent(SettingsActivity.this, AlarmSettingActivity.class);
                    startActivity(intent);
                }
                if (selectedItemStr == "プライバシーポリシー") {
                    Intent intent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
                    startActivity(intent);
                }
                if (selectedItemStr == "タスクを全て削除") {

                    // Toast.makeText(getApplicationContext(), "まだ実装してないんよー", Toast.LENGTH_SHORT).show();
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("タスクを全て削除")
                            .setMessage("タスクを全て消去しますか？")
                            .setPositiveButton("はい！", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK button pressed
                                    AsyncDeleteAllTask();
                                }
                            })
                            .setNegativeButton("いいえ", null)
                            .show();

                }

            }
        });

        // メニューに戻るボタン
        Button buttonBackToMenu = findViewById(R.id.btnBackToMenu5);
        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    // タスク全消去準備処理
    @UiThread
    private void AsyncDeleteAllTask() {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        SettingsActivity.BackgroundDeleteAllTask backgroundDeleteAllTask = new SettingsActivity.BackgroundDeleteAllTask(handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundDeleteAllTask);
    }

    // タスク全消去非同期処理
    private class BackgroundDeleteAllTask implements Runnable {
        private final Handler _handler;

        public BackgroundDeleteAllTask(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
            TaskDao taskDao = db.taskDao();
            taskDao.deleteAll();

            SettingsActivity.DeleteAllTaskPostExector deleteAllTaskPostExector = new SettingsActivity.DeleteAllTaskPostExector();
            _handler.post(deleteAllTaskPostExector);
        }
    }

    // タスク全消去非同期処理終了後の処理
    private class DeleteAllTaskPostExector implements Runnable {

        public DeleteAllTaskPostExector() { }

        @UiThread
        @Override
        public void run() {
            Toast.makeText(getApplicationContext() , "タスクを全て消去しました。", Toast.LENGTH_SHORT).show();
        }
    }
}