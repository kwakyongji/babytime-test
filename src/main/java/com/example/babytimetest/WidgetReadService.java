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

    private int currentStep = 0; // 매크로 단계 (0: 대기, 1: 메인 분유클릭, 2: 목록 클릭, 3: 용량수정 및 저장)

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        String pkg = packageName.toString();

        // [핵심] 원터치 분유 기록 자동화 매크로
        if (MainActivity.autoRecordPending && pkg.equals("com.daycare.babytime")) {

            // STEP 1: 베이비타임 실행 -> 상단 '분유' 동그라미 아이콘 터치 (15651.jpg)
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
            // STEP 2: 목록 최상단에 새로 추가된 '분유' 탭 터치 (15655.jpg)
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
            // STEP 3: 상세 입력창 -> 용량 수정 후 '저장' 버튼 터치 ➡️ 우리 앱으로 복귀 (15657.jpg)
            else if (currentStep == 2) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                    if (rootNode == null) return;

                    // 용량 입력칸(EditText) 순회 탐색 및 입력 (100, 120, 140, 160)
                    AccessibilityNodeInfo editNode = findEditableNode(rootNode);
                    if (editNode != null) {
                        performSetText(editNode, MainActivity.selectedAmount);
                    }

                    // 용량 변경 후 0.5초 뒤 '저장' 버튼 클릭
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        AccessibilityNodeInfo saveRootNode = getRootInActiveWindow();
                        if (saveRootNode == null) return;

                        List<AccessibilityNodeInfo> saveNodes = saveRootNode.findAccessibilityNodeInfosByText("저장");
                        for (AccessibilityNodeInfo saveNode : saveNodes) {
                            if (performClickParent(saveNode)) {
                                break;
                            }
                        }

                        // [추가] 저장 완료 후 0.8초 뒤 자동으로 다시 우리 앱으로 복귀!
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            }
                            // 매크로 완료 및 상태 초기화
                            MainActivity.autoRecordPending = false;
                            currentStep = 0;
                        }, 800);

                    }, 500);

                }, 1000);
            }
            return;
        }

        // 자기 자신 앱 이벤트 무시
        if (pkg.equals(getPackageName())) return;

        // 메인 화면 위젯 시간 가공 출력
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        findFormulaTime(rootNode);
    }

    private void findFormulaTime(AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.getText() != null) {
            String text = node.getText().toString().trim();

            if (text.contains("전화") || text.contains("빅스비") || text.contains("카메라") || 
                text.contains("메시지") || text.contains("갤러리") || text.contains("설정")) {
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
