package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class WidgetReadService extends AccessibilityService {

    private int currentStep = 0; // 매크로 단계 (0: 대기, 1: 메인 분유클릭, 2: 목록 클릭, 3: 저장 및 복귀)

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // 🚨 자기 자신(우리 앱) 이벤트는 완전히 무시
        if (pkg.equals(getPackageName())) return;

        // 1. 원터치 분유 기록 매크로 실행 (베이비타임 앱 내부일 때만)
        if (MainActivity.autoRecordPending && pkg.equals("com.daycare.babytime")) {

            // STEP 1: 베이비타임 메인 -> '분유' 동그라미 터치
            if (currentStep == 0) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
                    for (AccessibilityNodeInfo node : formulaNodes) {
                        if (performClickParent(node)) {
                            currentStep = 1;
                            break;
                        }
                    }
                }, 800);
            }
            // STEP 2: 상단에 추가된 새 '분유' 항목 클릭
            else if (currentStep == 1) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
                    for (int i = formulaNodes.size() - 1; i >= 0; i--) {
                        AccessibilityNodeInfo node = formulaNodes.get(i);
                        if (performClickParent(node)) {
                            currentStep = 2;
                            break;
                        }
                    }
                }, 1000);
            }
            // STEP 3: 용량 입력 및 '저장' 후 우리 앱으로 복귀
            else if (currentStep == 2) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    // 선택된 용량 입력
                    AccessibilityNodeInfo editNode = findEditableNode(rootNode);
                    if (editNode != null) {
                        performSetText(editNode, MainActivity.selectedAmount);
                    }

                    // 0.5초 뒤 '저장' 터치
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AccessibilityNodeInfo saveRootNode = getRootInActiveWindow();
                        if (saveRootNode == null) return;

                        List<AccessibilityNodeInfo> saveNodes = saveRootNode.findAccessibilityNodeInfosByText("저장");
                        for (AccessibilityNodeInfo saveNode : saveNodes) {
                            if (performClickParent(saveNode)) {
                                break;
                            }
                        }

                        // 저장 완료 후 0.8초 뒤 우리 앱 화면으로 자동 복귀
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            }
                            MainActivity.autoRecordPending = false;
                            currentStep = 0;
                        }, 800);

                    }, 500);

                }, 1000);
            }
            return;
        }

        // 2. 외부 화면/위젯 수유 시간 실시간 감지 (우리 앱 제외)
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        findFormulaTime(rootNode);
    }

    private void findFormulaTime(AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            // 예외 문구 필터링
            if (text.contains("전화") || text.contains("빅스비") || text.contains("카메라") || 
                text.contains("메시지") || text.contains("갤러리") || text.contains("설정") ||
                text.contains("원터치") || text.contains("기록기")) {
                return;
            }

            if (text.contains("분유") && (text.contains("전") || text.contains("분") || text.contains("시간"))) {
                String formatted = text.replace("분유", "").trim();
                if (!formatted.contains("시간")) {
                    formatted = "0시간 " + formatted;
                }
                if (!formatted.endsWith("에 먹었어요")) {
                    formatted = formatted + "에 먹었어요";
                }

                String finalOutput = formatted;
                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() -> MainActivity.resultTextView.setText(finalOutput));
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findFormulaTime(node.getChild(i));
        }
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() || (node.getClassName() != null && node.getClassName().toString().contains("EditText"))) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findEditableNode(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private boolean performClickParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        return performClickParent(node.getParent());
    }

    private boolean performSetText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    @Override
    public void onInterrupt() {
        currentStep = 0;
    }
}
