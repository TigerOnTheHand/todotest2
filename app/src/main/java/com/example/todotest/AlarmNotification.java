package com.example.todotest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static android.app.Notification.VISIBILITY_PUBLIC;

public class AlarmNotification extends BroadcastReceiver {
    Notification notification = null;

    @Override   // データを受信した
    public void onReceive(Context context, Intent intent) {

        //Log.d("AlarmBroadcastReceiver", "onReceive() pid=" + android.os.Process.myPid());

        int requestCode = intent.getIntExtra("RequestCode", 0);
        String strmess = intent.getStringExtra("Message");

        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "default";

        // app name
        String title = context.getString(R.string.app_name);

        // メッセージ
        /*
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat dataFormat =
                new SimpleDateFormat("HH:mm:ss", Locale.JAPAN);
        String cTime = dataFormat.format(currentTime);
        String message = "時間になりました。 " + cTime; // メッセージ　+ 11:22:331
         */
        String message = strmess;

        NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

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
            notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)                             //通知タイトル
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)//通知用アイコン
                    .setContentText(message)                            //通知内容
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();                                           //通知のビルド
        } else {
            //APIが「25」以下の場合

            //通知の生成と設定とビルド
            notification = new Notification.Builder(context)
                    .setContentTitle(title)                             //通知タイトル
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)//通知用アイコン
                    .setContentText(message)                            //通知内容
                    .setVibrate(new long[]{0, 200, 100, 200})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();                                           //通知のビルド
        }

        // 通知
        notificationManager.notify(R.string.app_name, notification);
    }
}
