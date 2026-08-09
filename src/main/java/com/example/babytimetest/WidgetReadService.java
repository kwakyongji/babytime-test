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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // 우리 앱 내부 이벤트는 무시
        if (pkg.equals(getPackageName())) return;

        // 1. 원터치 분유 자동 기록 매크로 (베이비타임 실행 중일 때)
        if (MainActivity.autoRecordPending && pkg.equals("yducky.application.babytime")) {

            // STEP 1: 메인 화면 상단 카테고리의 '분유' 버튼 클릭 (0ml 기록 생성)
            if (currentStep == 0) {
                mainHandler.postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    List<AccessibilityNodeInfo> formulaNodes = rootNode.findAccessibilityNodeInfosByText("분유");
                    for (AccessibilityNodeInfo node : formulaNodes) {
                        if (performClickParent(node)) {
                            currentStep = 1;
                            break;
                        }
                    }
                }, 600);
            }
            // STEP 2: 생성된 목록의 '0 ml' (또는 '0ml') 항목 터치하여 상세 수정창 진입
            else if (currentStep == 1) {
                mainHandler.postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    List<AccessibilityNodeInfo> zeroNodes = rootNode.findAccessibilityNodeInfosByText("0 ml");
                    if (zeroNodes.isEmpty()) {
                        zeroNodes = rootNode.findAccessibilityNodeInfosByText("0ml");
                    }

                    for (AccessibilityNodeInfo node : zeroNodes) {
                        if (performClickParent(node)) {
                            currentStep = 2;
                            break;
                        }
                    }
                }, 800);
            }
            // STEP 3: 상세 화면에서 선택한 용량 입력 후 우측 상단 '저장' 클릭 및 앱 복귀
            else if (currentStep == 2) {
                mainHandler.postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    // 용량 입력창 찾아서 값 변경
                    AccessibilityNodeInfo editNode = findEditableNode(rootNode);
                    if (editNode != null) {
                        performSetText(editNode, MainActivity.selectedAmount);
                    }

                    // 용량 입력 후 '저장' 버튼 클릭
                    mainHandler.postDelayed(() -> {
                        AccessibilityNodeInfo saveRootNode = getRootInActiveWindow();
                        if (saveRootNode == null) return;

                        List<AccessibilityNodeInfo> saveNodes = saveRootNode.findAccessibilityNodeInfosByText("저장");
                        for (AccessibilityNodeInfo saveNode : saveNodes) {
                            if (performClickParent(saveNode)) {
                                break;
                            }
                        }

                        // 작업 완료 후 우리 앱으로 복귀
                        mainHandler.postDelayed(() -> {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            }
                            MainActivity.autoRecordPending = false;
                            currentStep = 0;
                        }, 800);

                    }, 500);

                }, 800);
            }
            return;
        }

        // 2. 바탕화면 위젯 수유 시간 정교 읽기 ("3시간 18분 전" 감지)
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        findFormulaTimeFromWidget(rootNode);
    }

    private void findFormulaTimeFromWidget(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            // "3시간 18분 전", "18분 전", "1시간 전" 정규식 패턴 감지
            Pattern pattern = Pattern.compile("^(?:\\d+시간\\s*)?(?:\\d+분\\s*)?전$");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String formattedText = text + " 먹었어요";

                if (MainActivity.resultTextView != null) {
                    MainActivity.resultTextView.post(() -> MainActivity.resultTextView.setText(formattedText));
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findFormulaTimeFromWidget(node.getChild(i));
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
