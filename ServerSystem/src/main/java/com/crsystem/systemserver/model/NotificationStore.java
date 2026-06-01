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

    // 알림 추가
    public void addNotification(NotificationDTO notification) {
        store.computeIfAbsent(notification.getUserId(), k -> new CopyOnWriteArrayList<>())
             .add(notification);
        System.out.println("[NotificationStore] 알림 저장: userId=" + notification.getUserId()
                + ", type=" + notification.getType());
    }

    // 해당 유저의 미읽음 알림 목록 조회 (읽음 처리 전 스냅샷 반환)
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        CopyOnWriteArrayList<NotificationDTO> list = store.get(userId);
        if (list == null) return List.of();
        return list.stream()
                   .filter(n -> !n.isRead())
                   .collect(Collectors.toList());
    }

    // 알림 ID 목록에 해당하는 알림을 읽음 처리
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
