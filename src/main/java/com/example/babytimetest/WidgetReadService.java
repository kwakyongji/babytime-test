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

        // 1. 자동 입력 로직 (베이비타임 앱이 열렸을 때 작동)
        if (MainActivity.autoRecordPending && pkg.equals("com.daycare.babytime")) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Step 1: 상단 '분유' 버튼 클릭
                if (!isStep1Done) {
                    List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
                    for (AccessibilityNodeInfo node : formulaNodes) {
                        if (performClickParent(node)) {
                            isStep1Done = true;
                            // Step 2: 1초 뒤 '저장' 또는 '확인' 버튼 클릭
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

        // 3. 위젯 읽기 로직 ("분유 X분전" 정보만 감지)
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        findFormulaTextOnly(rootNode);
    }

    private void findFormulaTextOnly(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            // "분유" 단어가 포함되어 있고 "전"이 들어가는 최신 기록 형태만 감지 (예: "분유 6분전", "분유 3시간전")
            if (text.startsWith("분유") && (text.contains("전") || text.contains("분") || text.contains("시간"))) {
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
