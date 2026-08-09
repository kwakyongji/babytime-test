package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class WidgetReadService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 자기 자신 앱 화면의 변화는 무시 (무한 루프 방지)
        if (event.getPackageName() != null && 
            event.getPackageName().toString().equals(getPackageName())) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<String> collectedTexts = new ArrayList<>();
        collectWidgetText(rootNode, collectedTexts);

        // '분유', '수유', '기저귀', '대변', '소변', '수면' 등의 핵심 정보가 들어있는 경우만 정제해서 표시
        StringBuilder result = new StringBuilder();
        for (String text : collectedTexts) {
            if (text.contains("분유") || text.contains("수유") || text.contains("수면") || 
                text.contains("대변") || text.contains("소변") || text.contains("기저귀") || 
                text.contains("ml")) {
                
                // 불필요한 시스템 텍스트나 앱 자체 문구 제외
                if (!text.contains("성공") && !text.contains("전화") && !text.contains("빅스비")) {
                    result.append("• ").append(text).append("\n");
                }
            }
        }

        if (result.length() > 0 && MainActivity.resultTextView != null) {
            String finalMsg = "✅ 베이비타임 위젯 감지 완료!\n\n[현재 기록 상태]\n" + result.toString();
            MainActivity.resultTextView.post(() -> 
                MainActivity.resultTextView.setText(finalMsg)
            );
        }
    }

    private void collectWidgetText(AccessibilityNodeInfo node, List<String> list) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            if (!text.isEmpty()) {
                list.add(text);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectWidgetText(node.getChild(i), list);
        }
    }

    @Override
    public void onInterrupt() {}
}
