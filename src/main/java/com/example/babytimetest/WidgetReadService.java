package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class WidgetReadService extends AccessibilityService {

    private boolean isStep1Done = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // 1. 자동 입력 동작 (베이비타임 앱이 열렸을 때)
        if (MainActivity.autoRecordPending && pkg.equals("com.daycare.babytime")) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                if (!isStep1Done) {
                    List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
                    for (AccessibilityNodeInfo node : formulaNodes) {
                        if (performClickParent(node)) {
                            isStep1Done = true;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                AccessibilityNodeInfo newRoot = getRootInActiveWindow();
                                if (newRoot != null) {
                                    List<AccessibilityNodeInfo> saveNodes = newRoot.findAccessibilityNodeInfosByText("저장");
                                    if (saveNodes.isEmpty()) {
                                        saveNodes = newRoot.findAccessibilityNodeInfosByText("확인");
                                    }
                                    for (AccessibilityNodeInfo saveNode : saveNodes) {
                                        performClickParent(saveNode);
                                    }
                                }
                                MainActivity.autoRecordPending = false;
                                isStep1Done = false;
                            }, 1000);
                            break;
                        }
                    }
                }
            }
            return;
        }

        // 2. 자기 자신 앱 이벤트 무시
        if (pkg.equals(getPackageName())) return;

        // 3. 위젯 읽기 로직 ("분유 X분전" 진짜 기록만 감지)
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        findFormulaTextOnly(rootNode);
    }

    private void findFormulaTextOnly(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            // [차단 목록] 독바 아이콘이나 시스템 문구는 완전 무시
            if (text.contains("전화") || text.contains("빅스비") || text.contains("카메라") || 
                text.contains("메시지") || text.contains("갤러리") || text.contains("설정")) {
                return;
            }

            // 오직 "분유" 단어가 함께 들어있는 경우만 허용
            if (text.contains("분유") && (text.contains("전") || text.contains("분") || text.contains("시간"))) {
                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() ->
                        MainActivity.resultTextView.setText("🍼 최근 분유 기록:\n\n" + text)
                    );
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findFormulaTextOnly(node.getChild(i));
        }
    }

    private boolean performClickParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        return performClickParent(node.getParent());
    }

    @Override
    public void onInterrupt() {}
}
