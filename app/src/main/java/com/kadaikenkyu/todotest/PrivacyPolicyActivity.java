package com.kadaikenkyu.todotest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        TextView textView = findViewById(R.id.textView2);
        textView.setMovementMethod(new ScrollingMovementMethod());

        // メニューに戻るボタン
        Button buttonBackToMenu = findViewById(R.id.btnBackToMenu4);
        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}