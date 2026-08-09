package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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

        // 베이비타임 어플 실행
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.daycare.babytime");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
