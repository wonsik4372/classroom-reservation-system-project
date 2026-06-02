/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 예약 승인/거부 알림 DTO
 * @author wonsik
 */
public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { APPROVED, REJECTED }

    private String notificationId;  // 알림 고유 ID
    private String userId;          // 수신 학생 ID
    private String reservationId;   // 해당 예약 ID
    private Type type;              // APPROVED or REJECTED
    private String message;         // 알림 메시지
    private String rejectReason;    // 거절 사유 (REJECTED 시)
    private LocalDateTime createdAt;
    private boolean read;

    public NotificationDTO() {}

    public NotificationDTO(String notificationId, String userId, String reservationId,
                           Type type, String message, String rejectReason) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.reservationId = reservationId;
        this.type = type;
        this.message = message;
        this.rejectReason = rejectReason;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    // Getter
    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getReservationId() { return reservationId; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    // Setter
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public void setType(Type type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setRead(boolean read) { this.read = read; }
}
