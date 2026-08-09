package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class WidgetReadService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        findBabyTimeText(rootNode);
    }

    private void findBabyTimeText(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString();
            if (text.contains("분유") || text.contains("수유") || text.contains("전")) {
                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() -> 
                        MainActivity.resultTextView.setText("✅ 베이비타임 위젯 읽기 성공!\n\n감지된 텍스트:\n" + text)
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
