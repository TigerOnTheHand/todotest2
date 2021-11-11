package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    List<Task> tasks;
    TaskDao taskDao;
    int taskBlock_tate = 6,taskBlock_yoko = 6;
    int addtate = 10;
    int[][] taskBlockIDs = new int[taskBlock_tate + addtate][taskBlock_yoko];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        taskDao = db.taskDao();
        tasks = new ArrayList<Task>();
        TextView textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
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

            // tasks内のtaskをブロック状に積み上げる
            PileTaskID(tasks);

            // ボタンの表示・非表示、色の指定
            UpdateButton();

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

            str += "\n";

            for (int i = 0; i < taskBlock_tate; i++) {
                for (int j = 0; j < taskBlock_yoko; j++) {
                    str += taskBlockIDs[i + addtate][j] + ",";
                }
                str += "\n";
            }

            text.setText(str);
        }
    }

    public void GoToAddTask(View view) {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        startActivity(intent);
    }

    // tasksを期日が近い順に並べ替え
    private List<Task> SortInOrderOfDate(List<Task> tasks) {
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

    @WorkerThread
    private void PileTaskID(List<Task> tasks) {
        // 初期化
        for (int j = 0; j < taskBlockIDs.length; j++) {
            for (int k = 0; k < taskBlockIDs[j].length; k++) {
                taskBlockIDs[j][k] = -1;
            }
        }

        int block_x = 0; // ブロック落下毎にサイズ分増加
        // 終わったらbreakで抜ける
        for (int i = 0; i < tasks.size(); i++) {

            Task task = tasks.get(i);

            // ブロックが入らない→xを右に詰める
            if (block_x + (task.blockSize - 1) > taskBlock_yoko) {
                block_x = 0;
                // それでも入らないなら終了！
                if (block_x + (task.blockSize - 1) > taskBlock_yoko) {
                    break;
                }
            }

            // 仮のタスクブロックを作成
            int[][] taskBlock = new int[task.blockSize][task.blockSize];
            for (int j = 0; j < taskBlock.length; j++) {
                for (int k = 0; k < taskBlock[j].length; k++) {
                    taskBlock[j][k] = 1;
                }
            }

            // （あとからここにブロック削る処理？）

            // しょっぱなから置けねえならもうなんも置けねえ→終了！
            if (!GetCanPutBlock(taskBlock, block_x, 0)) {
                break;
            }

            // 上から置けるかどうか見ていく
            int block_y = 0;
            for (block_y = 0; true; block_y++) {
                if (!GetCanPutBlock(taskBlock, block_x, block_y)) {
                    break;
                }
            }

            // 一個上なら置けるよね！
            block_y -= 1;
            PutBlock(taskBlock, block_x, block_y, task.id);


            block_x += task.blockSize;
        }
    }

    // ブロックがそこに置けるかどうか調べる
    @WorkerThread
    private boolean GetCanPutBlock(int[][] taskBlock, int offset_x, int offset_y) {
        for(int y = 0; y < taskBlock.length; y++) {
            for(int x = 0; x < taskBlock.length; x++) {
                if (taskBlock[y][x] == 1) {
                    // ブロックがリスト外→アウト
                    if (offset_x + x > taskBlock_yoko - 1 || offset_y + y > taskBlock_tate + addtate - 1) {
                        return false;
                    }
                    // ブロックがかぶってる→アウト
                    if (taskBlockIDs[offset_y + y][offset_x + x] != -1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @WorkerThread
    private void PutBlock(int[][] taskBlock, int offset_x, int offset_y, int taskID) {
        for(int y = 0; y < taskBlock.length; y++) {
            for(int x = 0; x < taskBlock.length; x++) {
                if (taskBlock[y][x] == 1) {
                    taskBlockIDs[offset_y + y][offset_x + x] = taskID;
                }
            }
        }
    }

    // ボタンの表示・非表示、色の指定
    @WorkerThread
    private void UpdateButton() {
        for (int i = 0; i < taskBlock_tate; i++) {
            for (int j = 0; j < taskBlock_yoko; j++) {
                // 文字列からリソースIDを取得
                String num = String.valueOf(i * taskBlock_yoko + j + 1);
                if (num.length() < 2) {
                    num = "0" + num;
                }
                int btnId = getResources().getIdentifier("imageButton" + num, "id", getPackageName());
                ImageButton btn = (ImageButton)findViewById(btnId);

                // taskIDの取得
                int id = taskBlockIDs[addtate + i][j];
                // ボタンの表示・非表示
                if (id == -1) {
                    btn.setVisibility(View.INVISIBLE);
                }
                else {
                    btn.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void OnTaskBlock(View view) {
        String btnName = getResources().getResourceEntryName(view.getId());
        int id = Integer.getInteger(btnName.substring(btnName.length() - 2));
        int taskId = taskBlockIDs[(int)Math.floor(id / taskBlock_yoko)][(int)id % taskBlock_yoko];
        Task task = taskDao.findById(taskId);
        TextView text = findViewById(R.id.textView);
        text.setText(task.note);
    }
}