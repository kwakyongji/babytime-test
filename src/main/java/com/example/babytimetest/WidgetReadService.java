package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WidgetReadService extends AccessibilityService {

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessingMacro = false;
    private long lastReadTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        // 핵심: 베이비타임 패키지("yducky.application.babytime")의 이벤트일 때만 탐색 진행!
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !pkg.toString().equals("yducky.application.babytime")) {
            return;
        }

        try {
            AccessibilityNodeInfo rootNode = event.getSource();
            if (rootNode == null) {
                rootNode = getRootInActiveWindow();
            }
            if (rootNode == null) return;

            // 1. 원터치 매크로 실행
            if (MainActivity.autoRecordPending) {
                runSplitScreenMacro(rootNode);
            }

            // 2. 실시간 '마지막 수유' 시간 읽기 (1초 주기 쿨타임 적용)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReadTime > 1000) {
                lastReadTime = currentTime;
                extractLastFeedingTime(rootNode, 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runSplitScreenMacro(AccessibilityNodeInfo rootNode) {
        if (isProcessingMacro || rootNode == null) return;

        try {
            // STEP 0: 베이비타임 '분유' 버튼 클릭
            if (MainActivity.macroStep == 0) {
                AccessibilityNodeInfo formulaNode = findNodeByText(rootNode, "분유");
                if (formulaNode != null && performClickParent(formulaNode)) {
                    MainActivity.macroStep = 1;
                    isProcessingMacro = true;
                    mainHandler.postDelayed(() -> isProcessingMacro = false, 1000);
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
                    try {
                        AccessibilityNodeInfo currentRoot = getRootInActiveWindow();
                        if (currentRoot != null) {
                            AccessibilityNodeInfo saveNode = findNodeByText(currentRoot, "저장");
                            if (saveNode != null) {
                                performClickParent(saveNode);
                            }
                        }
                    } catch (Exception ignored) {}

                    mainHandler.postDelayed(() -> {
                        MainActivity.autoRecordPending = false;
                        MainActivity.macroStep = 0;
                        isProcessingMacro = false;
                    }, 1000);

                }, 800);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MainActivity.autoRecordPending = false;
            MainActivity.macroStep = 0;
            isProcessingMacro = false;
        }
    }

    // 트리 탐색 깊이 제한(depth <= 15)으로 스택 오버플로우 방지
    private boolean extractLastFeedingTime(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 15) return false;

        try {
            if (node.getText() != null && node.getText().toString().trim().equals("마지막 수유")) {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null) {
                    String feedingTime = findTimeInNodeTree(parent, 0);
                    if (feedingTime != null) {
                        sendTimeBroadcast(feedingTime + " 먹었어요");
                        return true;
                    }
                }
            }

            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (extractLastFeedingTime(child, depth + 1)) return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String findTimeInNodeTree(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 10) return null;

        try {
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
                    String res = findTimeInNodeTree(child, depth + 1);
                    if (res != null) return res;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String targetText) {
        if (node == null) return null;

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean performClickParent(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            if (node.isClickable()) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            return performClickParent(node.getParent());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean performSetText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        try {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        } catch (Exception e) {
            return false;
        }
    }

    // MainActivity로 안전하게 방송을 전송하는 메서드
    private void sendTimeBroadcast(String feedingTimeText) {
        Intent intent = new Intent("com.example.babytimetest.UPDATE_TIME");
        intent.putExtra("feeding_time", feedingTimeText);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        MainActivity.autoRecordPending = false;
        MainActivity.macroStep = 0;
        isProcessingMacro = false;
    }
}
