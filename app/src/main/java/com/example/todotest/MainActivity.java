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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
・タスク追加してもブロック更新されない
色適応のとこが怪しい
 */
public class MainActivity extends AppCompatActivity {
    List<Task> tasks;
    TaskDao taskDao;
    int taskBlock_tate = 8,taskBlock_yoko = 7;
    int addtate = 10;
    int[][] taskBlockIDs = new int[taskBlock_tate + addtate][taskBlock_yoko];
    String[][] taskColors = new String[taskBlock_tate + addtate][taskBlock_yoko];
    View view;
    int currentTaskID = -1;
    Task taskStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AppDatabase db = AppDatabaseSingleton.getInstance(getApplicationContext());
        taskDao = db.taskDao();
        tasks = new ArrayList<Task>();
        TextView textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        taskStore = new Task("", "", 0, 0, 0, 0);
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
            /*
            for (Task task : tasks) {
                Log.d("あああ", task.name);
            }

             */

            // tasks内のtaskをブロック状に積み上げる
            PileTaskID(tasks);

            // ボタンの表示・非表示、色の指定
            UpdateButton();
            Log.d("end", "end");

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

            // 仮のタスクブロックを作成(進捗分まで）
            /*
            int[][] taskBlock = new int[task.blockSize][task.blockSize];
            int blockNum = task.blockSize * task.blockSize - task.sintyoku;
            for (int j = 0; j < taskBlock.length; j++) {
                for (int k = 0; k < taskBlock[j].length; k++) {
                    if (j * task.blockSize + k + 1 > blockNum) { break; }
                    taskBlock[j][k] = 1;
                }
            }

             */
            int[][] taskBlock = new int[task.blockSize][task.blockSize];
            for (int j = 0; j < taskBlock.length; j++) {
                for (int k = 0; k < taskBlock[j].length; k++) {
                    taskBlock[j][k] = 1;
                }
            }
            for (int j = 0; j < taskBlock.length; j++) {
                for (int k = 0; k < taskBlock[j].length; k++) {
                    if (j * task.blockSize + k + 1 > task.sintyoku) { break; }
                    taskBlock[j][task.blockSize-1 - k] = -1;
                }
            }


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


            block_x += task.blockSize - task.sintyoku % task.blockSize;
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
                //btn.setEnabled(true);

                // taskIDの取得
                String color = taskColors[addtate + i][j];
                int id = taskBlockIDs[addtate + i][j];
                Task task = taskDao.findById(id);

                // ボタンの表示・非表示
                if (id == -1) {
                    // falseにするとなぜかうまく動作しない
                    // まあ-1は押してもはじくから大丈夫でしょう
                    //btn.setEnabled(false);
                    btn.setBackgroundColor(Color.GRAY);
                    btn.setAlpha(1f);
                }
                else {
                    //btn.setEnabled(true);
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

                    Calendar calendar1 = Calendar.getInstance();
                    // Month 値は 0 から始まるので注意
                    calendar1.set(task.year, task.monthOfYear - 1, task.dayOfMonth);

                    // 1970/1/1 から設定した calendar1 のミリ秒
                    long timeMillis1 = calendar1.getTimeInMillis();

                    // 現在時刻のミリ秒
                    long currentTimeMillis = System.currentTimeMillis();

                    // 差分のミリ秒
                    long diff = timeMillis1 - currentTimeMillis;

                    // ミリ秒から秒→分→時→日へ変換
                    diff = diff / (1000 * 60 * 60 * 24);

                    float alpha = 1.0f - (float)diff / 15f;
                    if (alpha > 1.0f) { alpha = 1.0f; }
                    if (alpha < 0.1f) { alpha = 0.1f; }

                    btn.setAlpha(alpha);
                }
                Log.d(String.valueOf(id), String.valueOf(id));
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
            taskStore.id = -1;
            if (taskId != -1) {
                currentTaskID = taskId;
                Task task = taskDao.findById(taskId);
                //TextView text = findViewById(R.id.textView);

                // 無理やりコピー（直接ぶち込むとアドレスコピーで参照先消えちゃうから）
                taskStore.id = task.id;
                taskStore.name = task.name;
                taskStore.note = task.note;
                taskStore.year = task.year;
                taskStore.monthOfYear = task.monthOfYear;
                taskStore.dayOfMonth = task.dayOfMonth;
                taskStore.blockSize = task.blockSize;
                taskStore.color = task.color;
                taskStore.sintyoku = task.sintyoku;

                /*
                String str = "課題名：" + task.name;
                str += "\n" + "期限：" + task.year + "/" + task.monthOfYear + "/" + task.dayOfMonth + "まで";
                str += "\n" + "説明：" + task.note;

                 */
                //Log.d("ppp", str);
                //text.setText(""); // WorkerThread内でなぜかsetTextできない
                //Log.d("uuu", "uuu");
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
            if (taskStore.id != -1) {
                String str = "課題名：" + taskStore.name;
                str += "\n" + "期限：" + taskStore.year + "/" + taskStore.monthOfYear + "/" + taskStore.dayOfMonth + "まで";
                str += "\n" + "説明：" + taskStore.note;

                TextView text = findViewById(R.id.textView);
                text.setText(str);
            }
        }
    }
}