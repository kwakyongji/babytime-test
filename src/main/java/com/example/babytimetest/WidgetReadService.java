package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetReadService extends AccessibilityService {

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessingMacro = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // 1. 원터치 매크로 실행
        if (MainActivity.autoRecordPending) {
            runSplitScreenMacro(rootNode);
        }

        // 2. 실시간 시간 추출
        extractLastFeedingTime(rootNode);
    }

    private void runSplitScreenMacro(AccessibilityNodeInfo rootNode) {
        if (isProcessingMacro || rootNode == null) return;

        // STEP 0: 베이비타임 '분유' 버튼 클릭
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

            AccessibilityNodeInfo editNode = findEditableNode(rootNode);
            if (editNode != null) {
                performSetText(editNode, MainActivity.selectedAmount);
            }

            mainHandler.postDelayed(() -> {
                AccessibilityNodeInfo currentRoot = getRootInActiveWindow();
                if (currentRoot != null) {
                    AccessibilityNodeInfo saveNode = findNodeByText(currentRoot, "저장");
                    if (saveNode != null) {
                        performClickParent(saveNode);
                    }
                }

                mainHandler.postDelayed(() -> {
                    MainActivity.autoRecordPending = false;
                    MainActivity.macroStep = 0;
                    isProcessingMacro = false;
                }, 800);

            }, 600);
        }
    }

    private void extractLastFeedingTime(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null && node.getText().toString().trim().equals("마지막 수유")) {
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                String feedingTime = findTimeInNodeTree(parent);
                if (feedingTime != null) {
                    updateResultText(feedingTime + " 먹었어요");
                    return;
                }
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                extractLastFeedingTime(child);
            }
        }
    }

    private String findTimeInNodeTree(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();
            if (!text.contains("기저귀") && !text.contains("대변") && !text.contains("수면")) {
                Pattern pattern = Pattern.compile("(\\d+시간\\s*)?(\\d+분\\s*)전");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return matcher.group(0).trim();
                }
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String res = findTimeInNodeTree(child);
                if (res != null) return res;
            }
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

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByText(child, targetText);
                if (result != null) return result;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (node.isEditable() || (node.getClassName() != null && node.getClassName().toString().contains("EditText"))) {
            return node;
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findEditableNode(child);
                if (result != null) return result;
            }
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
