package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    public static boolean autoRecordPending = false;
    public static String selectedAmount = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        Button btn100 = findViewById(R.id.btn100);
        Button btn120 = findViewById(R.id.btn120);
        Button btn140 = findViewById(R.id.btn140);
        Button btn160 = findViewById(R.id.btn160);

        btn100.setOnClickListener(v -> startAutoRecord("100"));
        btn120.setOnClickListener(v -> startAutoRecord("120"));
        btn140.setOnClickListener(v -> startAutoRecord("140"));
        btn160.setOnClickListener(v -> startAutoRecord("160"));
    }

    private void startAutoRecord(String amount) {
        selectedAmount = amount;
        autoRecordPending = true;

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.daycare.babytime");
        if (launchIntent != null) {
            startActivity(launchIntent);
            Toast.makeText(this, amount + "ml 분유 기록 진행 중...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "베이비타임 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            autoRecordPending = false;
        }
    }
}
