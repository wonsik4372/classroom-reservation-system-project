/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.NotificationDTO;
import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.main.CRSystemClient;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.List;

/**
 * 예약 결과 알림 컨트롤러
 *
 * - 로그인 직후: showImmediateNotifications()로 미읽음 알림 즉시 표시
 * - 로그인 유지 중: startPolling()으로 5초 주기 폴링 → Push 형태 알림
 * - 로그아웃 시 : stopPolling()으로 폴링 중단
 *
 * @author wonsik
 */
public class NotificationController {

    private static NotificationController instance;

    private static final int POLLING_INTERVAL_MS = 5000; // 5초 주기

    private javax.swing.Timer pollingTimer;
    private String currentUserId;

    private NotificationController() {}

    public static NotificationController getInstance() {
        if (instance == null) {
            instance = new NotificationController();
        }
        return instance;
    }

    // ====================
    // [SFR-408] [SFR-409] 미로그인 상태에서 쌓인 알림을 로그인 즉시 표시
    // ====================
    public void showImmediateNotifications(List<NotificationDTO> notifications, Component parent) {
        if (notifications == null || notifications.isEmpty()) return;

        SwingUtilities.invokeLater(() -> {
            for (NotificationDTO n : notifications) {
                showNotificationPopup(n, parent);
            }
        });
    }

    // ====================
    // [SFR-408] [SFR-409] 로그인 중인 학생에게 5초 주기 폴링으로 push 형태 알림 전달
    // ====================
    public void startPolling(String userId, Component parent) {
        this.currentUserId = userId;

        if (pollingTimer != null && pollingTimer.isRunning()) {
            pollingTimer.stop();
        }

        pollingTimer = new javax.swing.Timer(POLLING_INTERVAL_MS, e -> pollNotifications(parent));
        pollingTimer.setInitialDelay(POLLING_INTERVAL_MS); // 첫 폴링은 즉시 표시 이후 딜레이
        pollingTimer.start();

        System.out.println("[NotificationController] 알림 폴링 시작: userId=" + userId);
    }

    // ==========================================
    // 로그아웃 시 폴링 중단
    // ==========================================
    public void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.stop();
        }
        currentUserId = null;
        System.out.println("[NotificationController] 알림 폴링 중단");
    }

    // ==========================================
    // 서버에 미읽음 알림 요청
    // ==========================================
    private void pollNotifications(Component parent) {
        if (currentUserId == null) return;

        CRSystemClient.getInstance().sendRequest(
            new RequestDTO("GET_NOTIFICATIONS", currentUserId),
            (ResponseDTO response) -> {
                if (response.isSuccess()) {
                    List<NotificationDTO> notifications =
                        (List<NotificationDTO>) response.getPayload();
                    if (notifications != null && !notifications.isEmpty()) {
                        for (NotificationDTO n : notifications) {
                            showNotificationPopup(n, parent);
                        }
                    }
                }
            },
            errorMessage -> System.err.println("[NotificationController] 알림 조회 실패: " + errorMessage)
        );
    }

    // ==========================================
    // 알림 팝업 표시 (승인/거부 메시지 구분)
    // ==========================================
    private void showNotificationPopup(NotificationDTO notification, Component parent) {
        String title;
        int messageType;
        String body;

        if (notification.getType() == NotificationDTO.Type.APPROVED) {
            title = "예약 승인 알림";
            messageType = JOptionPane.INFORMATION_MESSAGE;
            body = notification.getMessage();
        } else {
            title = "예약 거부 알림";
            messageType = JOptionPane.WARNING_MESSAGE;
            String reason = notification.getRejectReason() != null
                    ? notification.getRejectReason() : "사유 없음";
            body = notification.getMessage() + "\n\n거부 사유: " + reason;
        }

        JOptionPane.showMessageDialog(parent, body, title, messageType);
    }
}
