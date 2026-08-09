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

    private int currentStep = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // 자기 자신(우리 앱) 이벤트 무시
        if (pkg.equals(getPackageName())) return;

        // 1. 원터치 분유 기록 매크로 실행 (베이비타임 내부일 때)
        if (MainActivity.autoRecordPending && pkg.equals("yducky.application.babytime")) {

            // STEP 1: 메인 화면에서 '분유' 클릭
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
            // STEP 2: 수유 기록 추가 항목 선택
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
            // STEP 3: 용량 자동 입력 및 저장 후 앱 복귀
            else if (currentStep == 2) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    AccessibilityNodeInfo editNode = findEditableNode(rootNode);
                    if (editNode != null) {
                        performSetText(editNode, MainActivity.selectedAmount);
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AccessibilityNodeInfo saveRootNode = getRootInActiveWindow();
                        if (saveRootNode == null) return;

                        List<AccessibilityNodeInfo> saveNodes = saveRootNode.findAccessibilityNodeInfosByText("저장");
                        for (AccessibilityNodeInfo saveNode : saveNodes) {
                            if (performClickParent(saveNode)) {
                                break;
                            }
                        }

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

        // 2. 바탕화면 위젯 수유 시간 실시간 읽기
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        findFormulaTime(rootNode);
    }

    private void findFormulaTime(AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            // 우리 앱 내부 문구 감지 방지
            if (text.contains("베이비타임 원터치") || text.contains("최근 수유") || text.contains("원터치 기록")) {
                return;
            }

            if ((text.contains("분유") || text.contains("수유")) && (text.contains("전") || text.contains("분") || text.contains("시간"))) {
                String formatted = text;
                if (!formatted.endsWith("에 먹었어요") && !formatted.endsWith("전")) {
                    formatted = formatted + " 에 먹었어요";
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
