package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubActivity extends AppCompatActivity {
    private int _taskId = -1;
    private String _taskName = "";
    private String note = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ListView lvCocktail = findViewById(R.id.lvCocktail);
        //Button btnSave = findViewById(R.id.btnSave);
        //AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());

        // リスナ
        //lvCocktail.setOnItemClickListener(new ListItemClickListener(db.taskDao()));
        //btnSave.setOnClickListener(new ButtonClickListener(db.taskDao()));
    }

    // セーブボタンリスナ
    private class ButtonClickListener implements View.OnClickListener {
        TaskDao taskDao;

        private ButtonClickListener(TaskDao cocktailDao) {
            this.taskDao = cocktailDao;
        }

        @Override
        public void onClick(View v) {
            //EditText etNote = findViewById(R.id.etNote);
            //note = etNote.getText().toString();

            AsyncUpdate(taskDao);
        }
    }

    // リストリスナ
    private class ListItemClickListener implements AdapterView.OnItemClickListener {
        TaskDao taskDao;

        private ListItemClickListener(TaskDao cocktailDao) {
            this.taskDao = cocktailDao;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            _taskId = position;
            _taskName = (String) parent.getItemAtPosition(position);
            //TextView tvCocktailName = findViewById(R.id.tvCocktailName);
            //tvCocktailName.setText(_taskName);
            //EditText etNote = findViewById(R.id.etNote);
            //etNote.setText("");

            // 保存ボタンを有効にする
            //Button btnSave = findViewById(R.id.btnSave);
            //btnSave.setEnabled(true);

            AsyncSelect(taskDao);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // リスト選択時の非同期処理準備
    @UiThread
    private void AsyncSelect(TaskDao taskDao) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundTaskSelect backgroundTaskSelect = new BackgroundTaskSelect(handler, taskDao);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTaskSelect);
    };

    // リスト選択時の非同期処理
    private class BackgroundTaskSelect implements Runnable {
        private final Handler _handler;
        private final TaskDao _taskDao;

        public BackgroundTaskSelect(Handler handler, TaskDao taskDao) {
            _handler = handler;
            this._taskDao = taskDao;
        }

        @WorkerThread
        @Override
        public void run() {
            // データ取得
            //Log.d("a", Integer.toString(_cocktailId));
            Task task = _taskDao.findById(_taskId);
            note = "";
            /*
            if (taskDao.id == _taskId) {
                note = cocktail.note;
            }
            */
            //Log.d("a", note);

            //SelectPostExector selectPostExector = new SelectPostExector(taskDao.note);
            //_handler.post(selectPostExector);
        }
    }

    // リスト選択時の非同期処理終了後の処理
    private class SelectPostExector implements Runnable {
        private String _note;

        public SelectPostExector(String note2) {
            _note = note2;
        }

        @UiThread
        @Override
        public void run() {
            //EditText etNote = findViewById(R.id.etNote);
            //etNote.setText(_note);
        }
    }

    // 保存ボタンが押された時の非同期処理準備
    @UiThread
    private void AsyncUpdate(TaskDao taskDao) {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundTaskUpdate backgroundTaskUpdate = new BackgroundTaskUpdate(handler, taskDao);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTaskUpdate);
    };

    // 保存ボタンが押された時の非同期処理
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
            // データを削除して更新
            _taskDao.delete(_taskId);
            _taskDao.insert(_taskId, _taskName, note);

            UpdatePostExector updatePostExector = new UpdatePostExector();
            _handler.post(updatePostExector);
        }
    }

    // 保存ボタンが押された時の非同期処理終了後の処理
    private class UpdatePostExector implements Runnable {
        @UiThread
        @Override
        public void run() {
            //EditText etNote = findViewById(R.id.etNote);
            //etNote.setText("");

            //TextView tvCocktailName = findViewById(R.id.tvCocktailName);
            //tvCocktailName.setText(getString(R.string.tv_name));

            //Button btnSave = findViewById(R.id.btnSave);
            //btnSave.setEnabled(false);
        }
    }
}
