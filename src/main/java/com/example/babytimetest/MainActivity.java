package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static TextView resultTextView;
    public static String selectedAmount = "140";
    public static boolean autoRecordPending = false;
    public static int macroStep = 0;

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
