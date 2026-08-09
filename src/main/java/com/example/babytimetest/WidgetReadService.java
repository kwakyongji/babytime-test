package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class WidgetReadService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // [중요] 자기 자신 앱 화면의 변화는 읽지 않도록 제외 (무한 루프 방지)
        if (event.getPackageName() != null && 
            event.getPackageName().toString().equals(getPackageName())) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        findBabyTimeText(rootNode);
    }

    private void findBabyTimeText(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            
            // 베이비타임에 실제 등장하는 단어가 있을 때만 읽기
            if (text.contains("분유") || text.contains("수유") || text.contains("모유") || 
                text.contains("이유식") || text.contains("기저귀") || text.contains("수면") || text.contains("전")) {
                
                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() -> 
                        MainActivity.resultTextView.setText("✅ 베이비타임 위젯 읽기 성공!\n\n📍 감지된 내용:\n" + text)
                    );
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findBabyTimeText(node.getChild(i));
        }
    }

    @Override
    public void onInterrupt() {}
}
