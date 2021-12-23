package com.kadaikenkyu.todotest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import static android.app.Notification.VISIBILITY_PUBLIC;

// 通知のオンオフ、通知の間隔を設定
public class AlarmSettingActivity extends AppCompatActivity {

    private AlarmManager am;
    private PendingIntent pending;
    private int requestCode = 1;
    Notification notification = null;
    public static String DATA_ID = "Data";
    public static String IS_ALARM_ON_ID = "IsAlarmOn";
    public static String ALARM_DATE_SPAN_ID = "AlarmDateSpan";
    public static String ALARM_HOUROFDAY_ID = "HourOfDay";
    public static String ALARM_MINUTE_ID = "Minute";

    SharedPreferences data;
    SharedPreferences.Editor editor;

    Spinner spinner;
    TextView txtTime;
    EditText editText;
    TimePickerDialog dialog;
    TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            editor.putInt(ALARM_HOUROFDAY_ID, hourOfDay);
            editor.putInt(ALARM_MINUTE_ID, minute);
            editor.apply();

            String _hourOfDay = String.valueOf(hourOfDay);
            String _minute = String.valueOf(minute);
            if (_minute.length() == 1) { _minute = "0" + _minute; }
            txtTime.setText("通知時刻：" + _hourOfDay + ":" + _minute);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarmsetting);

        data = getSharedPreferences(DATA_ID, MODE_PRIVATE);
        editor = data.edit();

        // 通知オンオフspinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("オフ");
        adapter.add("オン");
        spinner = (Spinner) findViewById(R.id.spinner2);
        spinner.setAdapter(adapter);
        boolean isAlartOn = data.getBoolean(IS_ALARM_ON_ID, false);
        if (isAlartOn) {
            int spinnerPosition = adapter.getPosition("オン");
            spinner.setSelection(spinnerPosition);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }

            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                //選択されたアイテム名と位置（index)を内部変数へ保存
                String item =  parent.getSelectedItem().toString();
                if (item == "オン") {
                    editor.putBoolean(IS_ALARM_ON_ID, true);
                } else {
                    editor.putBoolean(IS_ALARM_ON_ID, false);
                }
                editor.apply();
            }
        });

        // 通知時間
        txtTime = findViewById(R.id.txtAlartTime);
        String hourOfDay = String.valueOf(data.getInt(ALARM_HOUROFDAY_ID, 0));
        String minute = String.valueOf(data.getInt(ALARM_MINUTE_ID, 0));
        if (minute.length() == 1) { minute = "0" + minute; }
        txtTime.setText("通知時刻：" + hourOfDay + ":" + minute);

        // 通知テストボタン
        Button buttonTestAlarm = findViewById(R.id.btnTestAlarm);
        buttonTestAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 通知

                /*
                // 3秒後の時刻を取得
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                //calendar.add(Calendar.SECOND, 3);

                //設定した日時で発行するIntentを生成
                Intent intent = new Intent(getApplicationContext(), AlarmNotification.class);
                intent.putExtra("RequestCode", requestCode);
                intent.putExtra("Message","通知テストです。");
                pending = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // アラームをセットする
                am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending);

                Toast.makeText(getApplicationContext(), "通知を送信中......", Toast.LENGTH_SHORT).show();

                 */

                String channelId = "default";

                // 通知タイトルと内容
                String title = getString(R.string.app_name);
                String message = "通知テストです。";

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                //アンドロイドのバージョンで振り分け
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    //APIが「26」以上の場合

                    // Notification　Channel 設定
                    NotificationChannel channel = new NotificationChannel(channelId, title , NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription(message); //通知の説明のセット
                    channel.enableVibration(true);
                    channel.canShowBadge();
                    channel.enableLights(true);
                    channel.setLightColor(Color.BLUE);
                    // the channel appears on the lockscreen
                    channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
                    channel.setSound(defaultSoundUri, null);
                    channel.setShowBadge(true);

                    //通知チャンネルの作成
                    notificationManager.createNotificationChannel(channel);

                    //通知の生成と設定とビルド
                    notification = new Notification.Builder(AlarmSettingActivity.this, channelId)
                            .setContentTitle(title)                             //通知タイトル
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)//通知用アイコン
                            .setContentText(message)                            //通知内容
                            .setAutoCancel(true)
                            .build();                                           //通知のビルド
                } else {
                    //APIが「25」以下の場合

                    //通知の生成と設定とビルド
                    notification = new Notification.Builder(AlarmSettingActivity.this)
                            .setContentTitle(title)                             //通知タイトル
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)//通知用アイコン
                            .setContentText(message)                            //通知内容
                            .setVibrate(new long[]{0, 200, 100, 200})
                            .setAutoCancel(true)
                            .build();                                           //通知のビルド
                }

                // 通知
                notificationManager.notify(R.string.app_name, notification);
                Toast.makeText(getApplicationContext(), "通知を送信中......", Toast.LENGTH_SHORT).show();
            }
        });

        // 通知時間を設定するボタン
        Button buttonSetAlartTime = findViewById(R.id.btnSetAlartTime);
        buttonSetAlartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                // 現在の時間の取得
                int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                // 現在の分の取得
                int minute = calendar.get(Calendar.MINUTE);
                // ダイアログの生成、及び初期値の設定
                dialog = new TimePickerDialog(AlarmSettingActivity.this, android.R.style.Theme_Material, onTimeSetListener, hourOfDay, minute, true);
                // ダイアログを表示する
                dialog.show();
            }
        });

        // 通知開始残り日数入力EditText
        editText = findViewById(R.id.editAlarmDay);
        editText.setText(String.valueOf(data.getInt(ALARM_DATE_SPAN_ID, 0)));
        editText.addTextChangedListener( new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) { return; }
                editor.putInt(ALARM_DATE_SPAN_ID, Integer.parseInt(s.toString()));
                editor.apply();
            }
        } );

        // メニューに戻るボタン
        Button buttonBackToMenu = findViewById(R.id.btnBackToMenu3);
        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}