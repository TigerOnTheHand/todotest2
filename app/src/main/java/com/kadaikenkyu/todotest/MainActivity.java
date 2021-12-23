package com.kadaikenkyu.todotest;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Calendar;
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
    float[][] taskDiffs = new float[taskBlock_tate + addtate][taskBlock_yoko];

    View blockView; // 押されたブロックのView
    int currentTaskID = -1;
    Task taskStore;

    private AlarmManager am;
    private PendingIntent pending;
    private int requestCode = 1;

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

    // 通知を設定
    private void SetAlart() {
        if (pending != null && am != null) {
            am.cancel(pending); // 通知をリセット
        }

        // 課題が無い、または通知オフならはじく
        SharedPreferences data = getSharedPreferences(AlarmSettingActivity.DATA_ID, MODE_PRIVATE);
        if (tasks.size() != 0 && data.getBoolean(AlarmSettingActivity.IS_ALARM_ON_ID, false)) {

            // tasksを期日が近い順に並べ替え
            tasks = SortInOrderOfDate(tasks);

            // 今日に一番近い課題を取得
            Task latestTask = tasks.get(0);
            // 今日との日付の差がalarmDateSpan以上だったら課題を取得しない
            int alarmDateSpan = data.getInt(AlarmSettingActivity.ALARM_DATE_SPAN_ID, 0);
            Calendar today = Calendar.getInstance();
            Calendar taskDay = Calendar.getInstance();
            taskDay.set(latestTask.year, latestTask.monthOfYear - 1, latestTask.dayOfMonth);
            int span = GetDiffDays(taskDay, today);
            Log.d("ppp", String.valueOf(span));
            if (span > alarmDateSpan) { return; }
            // 指定日の課題を全て取得
            List<Task> t = GetTasksWhereDate(latestTask.year, latestTask.monthOfYear, latestTask.dayOfMonth);

            // 時刻を指定
            Calendar calendar = Calendar.getInstance();
            calendar.set(latestTask.year, latestTask.monthOfYear - 1, latestTask.dayOfMonth);
            calendar.set(Calendar.HOUR_OF_DAY, data.getInt(AlarmSettingActivity.ALARM_HOUROFDAY_ID, 0));
            calendar.set(Calendar.MINUTE, data.getInt(AlarmSettingActivity.ALARM_MINUTE_ID, 0));
            /*
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.SECOND, 3);
             */

            //設定した日時で発行するIntentを生成
            Intent intent = new Intent(getApplicationContext(), AlarmNotification.class);
            intent.putExtra("RequestCode", requestCode);
            String message = latestTask.monthOfYear + "/" + latestTask.dayOfMonth + "までの課題があります：";
            for (Task tas: t) {
                message += tas.name + "、";
            }
            message = message.substring(0, message.length() - 1);
            intent.putExtra("Message", message);
            pending = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // 通知をセットする(通知間隔は一日ごとで設定)
            am = (AlarmManager) getSystemService(ALARM_SERVICE);
            // am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pending);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pending);
        }
    }

    // 二つの日付の日数の差を返す
    private int GetDiffDays(Calendar calendar1, Calendar calendar2) {
        //==== ミリ秒単位での差分算出 ====//
        long diffTime = calendar1.getTimeInMillis() - calendar2.getTimeInMillis();

        //==== 日単位に変換 ====//
        int MILLIS_OF_DAY = 1000 * 60 * 60 * 24;
        int diffDays = (int)(diffTime / MILLIS_OF_DAY);

        return diffDays;
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

    public void GoToSettings(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
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

            // tasks内のtaskをブロック状に積み上げる
            PileTaskID(tasks);

            // ボタンの表示・非表示、色の指定
            //UpdateButton();
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
            UpdateButton();

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


            // 通知
            SetAlart();
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
                taskDiffs[j][k] = 0;
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

            int size = task.blockSize;
            if (task.blockSize * task.blockSize - task.sintyoku < task.blockSize) {
                size = task.blockSize * task.blockSize - task.sintyoku;
            }
            // ブロックが入らない→xを左に詰める
            if (block_x + (size - 1) > taskBlock_yoko - 1) {
                block_x = 0;
                // それでも入らないなら終了！
                if (block_x + (size - 1) > taskBlock_yoko - 1) {
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

            // ついでにdiffをWorkerThread内で計算しときたい...
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
            PutBlock(taskBlock, block_x, block_y, task.id, task.color, (float)diff);


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
    private void PutBlock(int[][] taskBlock, int offset_x, int offset_y, int id, String color, float diff) {
        for(int y = 0; y < taskBlock.length; y++) {
            for(int x = 0; x < taskBlock.length; x++) {
                if (taskBlock[y][x] == 1) {
                    taskBlockIDs[offset_y + y][offset_x + x] = id;
                    taskColors[offset_y + y][offset_x + x] = color;
                    taskDiffs[offset_y + y][offset_x + x] = diff;
                }
            }
        }
    }

    // ボタンの表示・非表示、色の指定
    //@WorkerThread
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

                    Log.d(String.valueOf(id), "taskname:");
                    //Task task = taskDao.findById(id);
                    //Log.d(String.valueOf(id), String.valueOf(task.name));

                    float diff = taskDiffs[addtate + i][j];
                    float alpha = 1.0f - (float)(Math.sqrt(diff) / 	7.745966692);
                    if (alpha > 1.0f) { alpha = 1.0f; }
                    if (alpha < 0.2f) { alpha = 0.2f; }

                    btn.setAlpha(alpha);
                }

                Log.d(String.valueOf(i * taskBlock_yoko + j + 1), String.valueOf(id));
            }
        }
    }

    public void OnTaskBlock(View view) {
        this.blockView = view;
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
            String btnName = getResources().getResourceEntryName(blockView.getId());
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
                str += "\n" + "進捗：" + taskStore.sintyoku + "/" + taskStore.blockSize * taskStore.blockSize + "個";
                str += "\n" + "期限：" + taskStore.year + "/" + taskStore.monthOfYear + "/" + taskStore.dayOfMonth + "まで";
                str += "\n" + "説明：" + taskStore.note;

                TextView text = findViewById(R.id.textView);
                text.setText(str);
            }
        }
    }

    // 日付で課題を取得
    private List<Task> GetTasksWhereDate(int year, int monthOfYear, int dayOfMonth) {
        List<Task> ta = new ArrayList<Task>();

        for (Task task: tasks) {
            if (task.year == year && task.monthOfYear == monthOfYear && task.dayOfMonth == dayOfMonth) {
                ta.add(task);
            }
        }

        return ta;
    }
}