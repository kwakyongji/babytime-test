package com.example.babytimetest;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setPadding(40, 40, 40, 40);
        tv.setText("접근성 권한을 켜주신 후\n홈 화면으로 나가서 베이비타임 위젯을 확인해주세요.\n\n[ 읽은 결과 ]\n대기 중...");
        
        resultTextView = tv;
        setContentView(tv);

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}
