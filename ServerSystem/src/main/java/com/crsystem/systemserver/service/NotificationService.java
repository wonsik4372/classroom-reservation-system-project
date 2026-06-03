/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.NotificationDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemserver.model.NotificationStore;

import java.util.List;
import java.util.UUID;

/**
 * 알림 처리 전문가
 * @author wonsik
 */
public class NotificationService {

    private static class Holder {
        private static final NotificationService INSTANCE = new NotificationService();
    }

    private NotificationService() {}

    public static NotificationService getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // [SFR-601] 예약 승인 알림 생성 → NotificationStore에 저장
    // ====================
    public void notifyApproved(ReservationDTO.Response reservation) {
        String message = String.format("[예약 승인] %s %s (%s) 예약이 승인되었습니다.",
                reservation.getRoomName(), reservation.getDate(), reservation.getPeriodInfo());

        NotificationDTO notification = new NotificationDTO(
                UUID.randomUUID().toString(),
                reservation.getUserId(),
                reservation.getReservationId(),
                NotificationDTO.Type.APPROVED,
                message,
                null
        );

        NotificationStore.getInstance().addNotification(notification);
    }

    // ====================
    // [SFR-602] 예약 거부 알림 생성 (거부 사유 포함) → NotificationStore에 저장
    // ====================
    public void notifyRejected(ReservationDTO.Response reservation, String rejectReason) {
        String message = String.format("[예약 거부] %s %s (%s) 예약이 거부되었습니다.",
                reservation.getRoomName(), reservation.getDate(), reservation.getPeriodInfo());

        NotificationDTO notification = new NotificationDTO(
                UUID.randomUUID().toString(),
                reservation.getUserId(),
                reservation.getReservationId(),
                NotificationDTO.Type.REJECTED,
                message,
                rejectReason
        );

        NotificationStore.getInstance().addNotification(notification);
    }

    // ====================
    // [SFR-601] [SFR-602] 클라이언트 폴링 요청 시 미읽음 알림 일괄 반환 후 읽음 처리
    // ====================
    public ResponseDTO getAndMarkNotifications(String userId) {
        List<NotificationDTO> unread = NotificationStore.getInstance().getUnreadNotifications(userId);

        if (!unread.isEmpty()) {
            List<String> ids = unread.stream()
                    .map(NotificationDTO::getNotificationId)
                    .collect(java.util.stream.Collectors.toList());
            NotificationStore.getInstance().markAsRead(userId, ids);
        }

        return new ResponseDTO(true, "알림 조회 성공", unread);
    }
}
