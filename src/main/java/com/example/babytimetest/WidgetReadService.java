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

        // 1. 자동 입력 동작 (베이비타임 실행 시)
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

        // 3. 위젯 텍스트를 "X시간 Y분전에 먹었어요" 형태로 다듬어 출력
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        findFormulaTime(rootNode);
    }

    private void findFormulaTime(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            if (text.contains("분유") && (text.contains("전") || text.contains("분") || text.contains("시간"))) {
                // "분유 6분전" -> "0시간 6분전에 먹었어요" 형태로 가공
                String formatted = text.replace("분유", "").trim();
                if (!formatted.contains("시간")) {
                    formatted = "0시간 " + formatted;
                }
                if (!formatted.endsWith("에 먹었어요")) {
                    formatted = formatted + "에 먹었어요";
                }

                String finalOutput = formatted;
                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() ->
                        MainActivity.resultTextView.setText(finalOutput)
                    );
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findFormulaTime(node.getChild(i));
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
