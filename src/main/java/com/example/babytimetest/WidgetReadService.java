package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetReadService extends AccessibilityService {

    private int currentStep = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessingMacro = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // 우리 앱 이벤트는 무시
        if (pkg.equals(getPackageName())) return;

        // 1. 원터치 분유 자동 기록 매크로 (베이비타임 앱 내부)
        if (MainActivity.autoRecordPending && pkg.equals("yducky.application.babytime")) {
            runMacroStep();
            return;
        }

        // 2. 화면에 보이는 실시간 수유 시간 탐색 (베이비타임 앱 화면 또는 위젯)
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        findLatestFormulaTime(rootNode);
    }

    private void runMacroStep() {
        if (isProcessingMacro) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // STEP 0: 메인 화면 상단 '분유' 버튼 터치
        if (currentStep == 0) {
            List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
            for (AccessibilityNodeInfo node : formulaNodes) {
                if (performClickParent(node)) {
                    currentStep = 1;
                    isProcessingMacro = true;
                    mainHandler.postDelayed(() -> isProcessingMacro = false, 1500); // 1.5초 여유
                    break;
                }
            }
        }
        // STEP 1: 생성된 목록 항목 '0 ml' 또는 '0ml' 터치
        else if (currentStep == 1) {
            List<AccessibilityNodeInfo> zeroNodes = rootNode.findAccessibilityNodeInfosByText("0 ml");
            if (zeroNodes.isEmpty()) {
                zeroNodes = rootNode.findAccessibilityNodeInfosByText("0ml");
            }

            for (AccessibilityNodeInfo node : zeroNodes) {
                if (performClickParent(node)) {
                    currentStep = 2;
                    isProcessingMacro = true;
                    mainHandler.postDelayed(() -> isProcessingMacro = false, 1500);
                    break;
                }
            }
        }
        // STEP 2: 수유량 변경 입력 및 '저장' 버튼 터치 후 복귀
        else if (currentStep == 2) {
            isProcessingMacro = true;

            // 용량 수정
            AccessibilityNodeInfo editNode = findEditableNode(rootNode);
            if (editNode != null) {
                performSetText(editNode, MainActivity.selectedAmount);
            }

            // 1초 후 저장 버튼 찾아 클릭
            mainHandler.postDelayed(() -> {
                AccessibilityNodeInfo saveRootNode = getRootInActiveWindow();
                if (saveRootNode != null) {
                    boolean clicked = false;
                    List<AccessibilityNodeInfo> saveNodes = saveRootNode.findAccessibilityNodeInfosByText("저장");
                    for (AccessibilityNodeInfo saveNode : saveNodes) {
                        if (performClickParent(saveNode)) {
                            clicked = true;
                            break;
                        }
                    }

                    if (!clicked) {
                        findAndClickSaveNode(saveRootNode);
                    }
                }

                // 저장 후 1.2초 대기 후 원터치 앱 복귀
                mainHandler.postDelayed(() -> {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    }
                    MainActivity.autoRecordPending = false;
                    currentStep = 0;
                    isProcessingMacro = false;
                }, 1200);

            }, 1000);
        }
    }

    private void findLatestFormulaTime(AccessibilityNodeInfo node) {
        if (node == null) return;

        // "3시간 31분전" 또는 "3시간 31분 전" 감지 정규식
        Pattern pattern = Pattern.compile("(\\d+시간\\s*)?(\\d+분\\s*)전");

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String matchedTime = matcher.group(0);
                
                // 첫 번째 감지된 최신 시간만 취득하고 하위 탐색 중단
                updateResultText(matchedTime + " 먹었어요");
                return;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findLatestFormulaTime(node.getChild(i));
        }
    }

    private void updateResultText(String text) {
        if (MainActivity.resultTextView != null) {
            MainActivity.resultTextView.post(() -> MainActivity.resultTextView.setText(text));
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

    private boolean findAndClickSaveNode(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.getText() != null && node.getText().toString().contains("저장")) {
            return performClickParent(node);
        }
        if (node.getContentDescription() != null && node.getContentDescription().toString().contains("저장")) {
            return performClickParent(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (findAndClickSaveNode(node.getChild(i))) {
                return true;
            }
        }
        return false;
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
        isProcessingMacro = false;
    }
}
