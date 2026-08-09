package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetReadService extends AccessibilityService {

    private int currentStep = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessingMacro = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        CharSequence packageName = event.getPackageName();
        String pkg = packageName != null ? packageName.toString() : "";

        // 우리 앱 내부 이벤트는 제외
        if (pkg.equals(getPackageName())) return;

        // 1. 원터치 분유 자동 기록 매크로 (베이비타임 앱 실행 중)
        if (MainActivity.autoRecordPending && pkg.equals("yducky.application.babytime")) {
            runMacroStep();
            return;
        }

        // 2. 바탕화면 위젯 / 베이비타임 앱의 수유 시간 추출
        extractLatestFeedingTime(rootNode);
    }

    private void runMacroStep() {
        if (isProcessingMacro) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // STEP 0: 메인 화면 상단 '분유' 버튼 클릭 (딜레이 충분히 부여)
        if (currentStep == 0) {
            List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
            for (AccessibilityNodeInfo node : formulaNodes) {
                if (performClickParent(node)) {
                    currentStep = 1;
                    isProcessingMacro = true;
                    mainHandler.postDelayed(() -> isProcessingMacro = false, 1500);
                    break;
                }
            }
        }
        // STEP 1: 생성된 목록 항목 '0 ml' 또는 '0ml' 클릭
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
        // STEP 2: 용량 변경 입력 및 우측 상단 '저장' 클릭 후 앱 복귀
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

    // 화면 내(위젯 포함)에서 수유 시간 텍스트 추출
    private void extractLatestFeedingTime(AccessibilityNodeInfo rootNode) {
        List<String> foundTimes = new ArrayList<>();
        collectTimeTexts(rootNode, foundTimes);

        if (!foundTimes.isEmpty()) {
            String latestTime = foundTimes.get(0);
            updateResultText(latestTime + " 먹었어요");
        }
    }

    private void collectTimeTexts(AccessibilityNodeInfo node, List<String> timesList) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            
            // "분유 3시간 18분 전", "3시간 18분전" 등 패턴 매칭
            Pattern pattern = Pattern.compile("(?:분유\\s*)?((\\d+시간\\s*)?\\d+분\\s*전)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String cleanTime = matcher.group(1).trim();
                timesList.add(cleanTime);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectTimeTexts(node.getChild(i), timesList);
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
