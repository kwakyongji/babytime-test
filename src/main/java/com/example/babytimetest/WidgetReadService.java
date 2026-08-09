package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetReadService extends AccessibilityService {

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessingMacro = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // 1. 원터치 매크로 실행 (분할 화면 모드)
        if (MainActivity.autoRecordPending) {
            runSplitScreenMacro(rootNode);
        }

        // 2. 실시간 '마지막 수유' 시간 추출 (분할 화면 모드)
        extractLastFeedingTime(rootNode);
    }

    // 분할 화면에 떠 있는 베이비타임 화면을 대상으로 순차 클릭 수행
    private void runSplitScreenMacro(AccessibilityNodeInfo rootNode) {
        if (isProcessingMacro) return;

        // STEP 0: 베이비타임 화면의 '분유' 아이콘 클릭
        if (MainActivity.macroStep == 0) {
            AccessibilityNodeInfo formulaNode = findNodeByText(rootNode, "분유");
            if (formulaNode != null && performClickParent(formulaNode)) {
                MainActivity.macroStep = 1;
                isProcessingMacro = true;
                mainHandler.postDelayed(() -> isProcessingMacro = false, 800);
            }
        }
        // STEP 1: 용량 입력 및 '저장' 클릭
        else if (MainActivity.macroStep == 1) {
            isProcessingMacro = true;

            // 1) 용량 텍스트 입력창 찾아서 값 설정
            AccessibilityNodeInfo editNode = findEditableNode(rootNode);
            if (editNode != null) {
                performSetText(editNode, MainActivity.selectedAmount);
            }

            // 2) 0.6초 대기 후 저장 버튼 클릭
            mainHandler.postDelayed(() -> {
                AccessibilityNodeInfo currentRoot = getRootInActiveWindow();
                if (currentRoot != null) {
                    AccessibilityNodeInfo saveNode = findNodeByText(currentRoot, "저장");
                    if (saveNode != null) {
                        performClickParent(saveNode);
                    }
                }

                // 매크로 완료 처리
                mainHandler.postDelayed(() -> {
                    MainActivity.autoRecordPending = false;
                    MainActivity.macroStep = 0;
                    isProcessingMacro = false;
                }, 800);

            }, 600);
        }
    }

    // '마지막 수유' 바로 옆/아래에 연결된 시간 문자열만 핀포인트로 읽어오기
    private void extractLastFeedingTime(AccessibilityNodeInfo node) {
        if (node == null) return;

        // "마지막 수유" 라는 라벨 노드를 탐색
        if (node.getText() != null && node.getText().toString().trim().equals("마지막 수유")) {
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                // 부모 노드나 형제 노드들 중 시간 패턴(예: 3시간 54분전)을 찾음
                String feedingTime = findTimeInNodeTree(parent);
                if (feedingTime != null) {
                    updateResultText(feedingTime + " 먹었어요");
                    return;
                }
            }
        }

        // 전체 트리 재귀 탐색
        for (int i = 0; i < node.getChildCount(); i++) {
            extractLastFeedingTime(node.getChild(i));
        }
    }

    private String findTimeInNodeTree(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            // 기저귀, 대변, 수면 등의 키워드가 들어있는 노드는 걸러냄
            if (!text.contains("기저귀") && !text.contains("대변") && !text.contains("수면")) {
                Pattern pattern = Pattern.compile("(\\d+시간\\s*)?(\\d+분\\s*)전");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return matcher.group(0).trim();
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            String res = findTimeInNodeTree(node.getChild(i));
            if (res != null) return res;
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String targetText) {
        if (node == null) return null;

        if (node.getText() != null && node.getText().toString().contains(targetText)) {
            return node;
        }
        if (node.getContentDescription() != null && node.getContentDescription().toString().contains(targetText)) {
            return node;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByText(node.getChild(i), targetText);
            if (result != null) return result;
        }
        return null;
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

    private void updateResultText(String text) {
        if (MainActivity.resultTextView != null) {
            MainActivity.resultTextView.post(() -> MainActivity.resultTextView.setText(text));
        }
    }

    @Override
    public void onInterrupt() {
        MainActivity.autoRecordPending = false;
        MainActivity.macroStep = 0;
        isProcessingMacro = false;
    }
}
