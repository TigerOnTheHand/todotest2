package com.example.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    List<Task> tasks;
    TaskDao taskDao;
    int taskBlock_tate = 8,taskBlock_yoko = 7;
    int addtate = 10;
    int[][] taskBlockIDs = new int[taskBlock_tate + addtate][taskBlock_yoko];
    String[][] taskColors = new String[taskBlock_tate + addtate][taskBlock_yoko];
    View view;
    int currentTaskID = -1;

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

    public void GoToAddTask(View view) {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        startActivity(intent);
    }

    public void GoToDoTask(View view) {
        if (currentTaskID == -1) {
            Toast.makeText(getApplicationContext() , "課題を選択してください", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, DoTaskActivity.class);
        intent.putExtra("TASKID", currentTaskID);
        startActivity(intent);
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
            for (Task task : tasks) {
                Log.d("あああ", task.name);
            }

            // tasks内のtaskをブロック状に積み上げる
            PileTaskID(tasks);

            // ボタンの表示・非表示、色の指定
            UpdateButton();
            Log.d("aaa", "aaa");

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
/*
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
*/
            text.setText(str);

        }
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
                taskColors[j][k] = "";
            }
        }

        int block_x = 0; // ブロック落下毎にサイズ分増加
        // 終わったらbreakで抜ける
        for (int i = 0; i < tasks.size(); i++) {

            Task task = tasks.get(i);

            // 仮のタスクブロックを作成
            int[][] taskBlock = new int[task.blockSize][task.blockSize];
            for (int j = 0; j < taskBlock.length; j++) {
                for (int k = 0; k < taskBlock[j].length; k++) {
                    taskBlock[j][k] = 1;
                }
            }

            // （あとからここにブロック削る処理？）



            // ブロックが入らない→xを右に詰める
            if (block_x + (task.blockSize - 1) > taskBlock_yoko - 1) {
                block_x = 0;
                // それでも入らないなら終了！
                if (block_x + (task.blockSize - 1) > taskBlock_yoko - 1) {
                    break;
                }
            }

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
            PutBlock(taskBlock, block_x, block_y, task.id, task.color);


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
    private void PutBlock(int[][] taskBlock, int offset_x, int offset_y, int id, String color) {
        for(int y = 0; y < taskBlock.length; y++) {
            for(int x = 0; x < taskBlock.length; x++) {
                if (taskBlock[y][x] == 1) {
                    taskBlockIDs[offset_y + y][offset_x + x] = id;
                    taskColors[offset_y + y][offset_x + x] = color;
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
                btn.setEnabled(true);

                // taskIDの取得
                String color = taskColors[addtate + i][j];
                int id = taskBlockIDs[addtate + i][j];

                // ボタンの表示・非表示
                if (id == -1) {
                    btn.setEnabled(false);
                    btn.setBackgroundColor(Color.GRAY);
                }
                else {
                    btn.setEnabled(true);
                    if (color.equals("赤")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_red);
                    }
                    if (color.equals("青")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_blue);
                    }
                    if (color.equals("緑")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_green);
                    }
                    if (color.equals("黄色")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_yellow);
                    }
                    if (color.equals("ピンク")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_pink);
                    }
                    if (color.equals("オレンジ")) {
                        btn.setBackgroundResource(R.drawable.taskbutton_orange);
                    }
                }
            }
        }
    }

    public void OnTaskBlock(View view) {
        this.view = view;
        AsyncFind();
    }

    // ブロック表示非同期処理準備
    @UiThread
    private void AsyncFind() {
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);

        BackgroundFind backgroundFind = new BackgroundFind(handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundFind);
    }

    // ブロック表示非同期処理
    private class BackgroundFind implements Runnable {
        private final Handler _handler;

        public BackgroundFind(Handler handler) {
            _handler = handler;
        }

        @WorkerThread
        @Override
        public void run() {
            String btnName = getResources().getResourceEntryName(view.getId());
            String idstr = btnName.substring(btnName.length() - 2);

            int id = Integer.valueOf(idstr) - 1;
            int taskId = taskBlockIDs[(int)Math.floor(id / taskBlock_yoko) + addtate][(int)id % taskBlock_yoko];
            if (taskId != -1) {
                currentTaskID = taskId;
                Task task = taskDao.findById(taskId);
                TextView text = findViewById(R.id.textView);
                String str = "課題名：" + task.name;
                str += "\n" + "期限：" + task.year + "/" + task.monthOfYear + "/" + task.dayOfMonth + "まで";
                str += "\n" + "説明：" + task.note;
                text.setText(str);
            }

            FindPostExector FindPostExector = new FindPostExector();
            _handler.post(FindPostExector);
        }
    }

    // ブロック表示非同期処理終了後の処理
    private class FindPostExector implements Runnable {

        public FindPostExector() { }

        @UiThread
        @Override
        public void run() {

        }
    }
}