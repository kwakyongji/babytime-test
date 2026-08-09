package com.example.babytimetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView resultTextView;
    public static String selectedAmount = "140";
    public static boolean autoRecordPending = false;
    public static int macroStep = 0;

    // 서비스로부터 최신 수유 시간을 전달받는 안전한 수신기
    private final BroadcastReceiver timeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("feeding_time")) {
                String timeStr = intent.getStringExtra("feeding_time");
                if (resultTextView != null) {
                    resultTextView.setText(timeStr);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);

        Button btn100 = findViewById(R.id.btn100);
        Button btn120 = findViewById(R.id.btn120);
        Button btn140 = findViewById(R.id.btn140);
        Button btn160 = findViewById(R.id.btn160);

        btn100.setOnClickListener(v -> startFormulaMacro("100"));
        btn120.setOnClickListener(v -> startFormulaMacro("120"));
        btn140.setOnClickListener(v -> startFormulaMacro("140"));
        btn160.setOnClickListener(v -> startFormulaMacro("160"));

        checkAccessibilityPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter("com.example.babytimetest.UPDATE_TIME");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timeUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timeUpdateReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(timeUpdateReceiver);
        } catch (Exception ignored) {}
    }

    private void startFormulaMacro(String amount) {
        selectedAmount = amount;
        autoRecordPending = true;
        macroStep = 0;
        Toast.makeText(this, amount + " ML 기록 시작", Toast.LENGTH_SHORT).show();
    }

    private void checkAccessibilityPermission() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException ignored) {}

        if (accessibilityEnabled == 0) {
            Toast.makeText(this, "접근성 서비스를 켜주세요.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }
}
