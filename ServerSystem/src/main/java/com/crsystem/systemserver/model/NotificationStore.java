/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.model;

import com.crsystem.common.dto.NotificationDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 학생별 미읽음 알림을 보관하는 인메모리 저장소
 *
 * ClientHandler가 스레드마다 실행되므로 ConcurrentHashMap + CopyOnWriteArrayList로
 * 동시 접근을 안전하게 처리
 *
 * @author wonsik
 */
public class NotificationStore {

    // Initialization-on-demand holder 패턴: 지연 초기화 + 스레드 안전
    private static class Holder {
        private static final NotificationStore INSTANCE = new NotificationStore();
    }

    // userId -> 알림 목록
    private final Map<String, CopyOnWriteArrayList<NotificationDTO>> store = new ConcurrentHashMap<>();

    private NotificationStore() {}

    public static NotificationStore getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // [SFR-601] 알림 처리 - 승인 알림을 사용자별 저장소에 추가
    // [SFR-602] 알림 처리 - 거부 알림(거부 사유 포함)을 사용자별 저장소에 추가
    // ====================
    public void addNotification(NotificationDTO notification) {
        store.computeIfAbsent(notification.getUserId(), k -> new CopyOnWriteArrayList<>())
             .add(notification);
        System.out.println("[NotificationStore] 알림 저장: userId=" + notification.getUserId()
                + ", type=" + notification.getType());
    }

    // ====================
    // [SFR-601] 알림 처리 - 로그인/폴링 시 미읽음 알림 목록 조회 (Observer Pattern)
    // [TC-35] getUnreadNotifications - 미읽음 알림 반환
    // [TC-41] getUnreadNotifications - 이미 읽은 알림은 제외
    // ====================
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        CopyOnWriteArrayList<NotificationDTO> list = store.get(userId);
        if (list == null) return List.of();
        return list.stream()
                   .filter(n -> !n.isRead())
                   .collect(Collectors.toList());
    }

    // ====================
    // [SFR-601] 알림 처리 - 지정한 알림 ID 목록을 읽음 처리
    // [TC-41] markAsRead - 지정 ID만 읽음 처리, 나머지 유지
    // ====================
    public void markAsRead(String userId, List<String> notificationIds) {
        CopyOnWriteArrayList<NotificationDTO> list = store.get(userId);
        if (list == null) return;
        // CopyOnWriteArrayList는 읽기 반복 중 수정이 안전
        for (NotificationDTO n : list) {
            if (notificationIds.contains(n.getNotificationId())) {
                n.setRead(true);
            }
        }
    }
}
