package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    public static boolean autoRecordPending = false;
    public static String selectedAmount = "140";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        setupButton(R.id.btn100, "100");
        setupButton(R.id.btn120, "120");
        setupButton(R.id.btn140, "140");
        setupButton(R.id.btn160, "160");
    }

    private void setupButton(int buttonId, String amount) {
        Button btn = findViewById(buttonId);
        btn.setOnClickListener(v -> {
            selectedAmount = amount;
            autoRecordPending = true;

            // 베이비타임 앱 실행 -> 서비스가 감지하여 매크로 수행
            Intent intent = getPackageManager().getLaunchIntentForPackage("yducky.application.babytime");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }
}
