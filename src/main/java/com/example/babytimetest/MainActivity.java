package com.example.babytimetest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static TextView resultTextView;
    public static boolean autoRecordPending = false;
    public static String selectedAmount = "100";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        Button btn100 = findViewById(R.id.btn100);
        Button btn120 = findViewById(R.id.btn120);
        Button btn140 = findViewById(R.id.btn140);
        Button btn160 = findViewById(R.id.btn160);

        btn100.setOnClickListener(v -> startBabyTimeAutoRecord("100"));
        btn120.setOnClickListener(v -> startBabyTimeAutoRecord("120"));
        btn140.setOnClickListener(v -> startBabyTimeAutoRecord("140"));
        btn160.setOnClickListener(v -> startBabyTimeAutoRecord("160"));
    }

    private void startBabyTimeAutoRecord(String amount) {
        selectedAmount = amount;
        autoRecordPending = true;

        Toast.makeText(this, amount + "ml 자동 기록을 시작합니다.", Toast.LENGTH_SHORT).show();

        // 확인된 베이비타임 실행
        Intent intent = getPackageManager().getLaunchIntentForPackage("yducky.application.babytime");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "베이비타임 앱을 열 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    }
}
