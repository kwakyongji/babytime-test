package com.example.babytimetest;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayList;
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

        try {
            // 분할 화면의 모든 윈도우(창) 수집
            List<AccessibilityNodeInfo> rootNodes = getAllWindowRoots();
            if (rootNodes.isEmpty()) return;

            // 1. 매크로 실행 (분할 화면)
            if (MainActivity.autoRecordPending) {
                for (AccessibilityNodeInfo root : rootNodes) {
                    runSplitScreenMacro(root);
                }
            }

            // 2. 실시간 '마지막 수유' 시간 읽기 (0.8초 주기)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReadTime > 800) {
                lastReadTime = currentTime;
                for (AccessibilityNodeInfo root : rootNodes) {
                    if (extractLastFeedingTime(root)) {
                        break; // 찾았으면 다른 창 탐색 종료
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 분할 화면의 활성/비활성 창 노드를 모두 가져오는 메쏘드
    private List<AccessibilityNodeInfo> getAllWindowRoots() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window != null) {
                        AccessibilityNodeInfo root = window.getRoot();
                        if (root != null) {
                            roots.add(root);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 윈도우 목록을 가져오지 못했을 때의 예외 처리
        if (roots.isEmpty()) {
            AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
            if (activeRoot != null) {
                roots.add(activeRoot);
            }
        }
        return roots;
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
                        List<AccessibilityNodeInfo> roots = getAllWindowRoots();
                        for (AccessibilityNodeInfo root : roots) {
                            AccessibilityNodeInfo saveNode = findNodeByText(root, "저장");
                            if (saveNode != null) {
                                performClickParent(saveNode);
                                break;
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

    private boolean extractLastFeedingTime(AccessibilityNodeInfo node) {
        if (node == null) return false;

        try {
            if (node.getText() != null && node.getText().toString().trim().equals("마지막 수유")) {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null) {
                    String feedingTime = findTimeInNodeTree(parent);
                    if (feedingTime != null) {
                        updateResultText(feedingTime + " 먹었어요");
                        return true;
                    }
                }
            }

            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    if (extractLastFeedingTime(child)) return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String findTimeInNodeTree(AccessibilityNodeInfo node) {
        if (node == null) return null;

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
                    String res = findTimeInNodeTree(child);
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
