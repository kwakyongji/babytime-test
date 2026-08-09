package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    public static boolean autoRecordPending = false; // 자동 기록 동작 상태 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);
        Button recordButton = findViewById(R.id.recordButton);

        // 분유 자동 기록 버튼 클릭 이벤트
        recordButton.setOnClickListener(v -> {
            autoRecordPending = true; // 기록 동작 실행 플래그 ON

            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.daycare.babytime");
            if (launchIntent != null) {
                startActivity(launchIntent);
                Toast.makeText(this, "베이비타임 앱을 실행하여 분유를 기록합니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "베이비타임 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                autoRecordPending = false;
            }
        });
    }
}
