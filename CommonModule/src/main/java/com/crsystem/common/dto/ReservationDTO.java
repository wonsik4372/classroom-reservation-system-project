/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import com.crsystem.common.enums.Role;
import java.io.Serializable;
import java.time.LocalDate;

/**
 *
 * @author wonsik
 */
public class ReservationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 예약 상태를 안전하게 관리하기 위한 Enum
    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    // 1. 클라이언트(학생) -> 서버 : 예약 생성 요청 시 사용하는 껍데기
    public static class Request implements Serializable {
        private static final long serialVersionUID = 1L;

        private String reservationId; // 예약 고유 ID (승인/거부 요청 시 사용)
        private String userId;        // 예약자 ID
        private String roomName;      // 강의실 번호
        private LocalDate date;       // 예약 날짜
        private String periodInfo;    // 교시
        private String purpose;       // 사유
        private int partnerCount;     // 동반 인원

        public Request() {}

        // Getter
        public String getReservationId() {
            return reservationId;
        }

        public String getUserId() {
            return userId;
        }

        public String getRoomName() {
            return roomName;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getPeriodInfo() {
            return periodInfo;
        }

        public String getPurpose() {
            return purpose;
        }

        public int getPartnerCount() {
            return partnerCount;
        }
        
        // Setter
        public void setReservationId(String reservationId) {
            this.reservationId = reservationId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public void setPeriodInfo(String periodInfo) {
            this.periodInfo = periodInfo;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public void setPartnerCount(int partnerCount) {
            this.partnerCount = partnerCount;
        }
    }

    // 2. 서버 -> 클라이언트(조교) : 화면에 뿌려줄 완벽한 예약 정보
    public static class Response implements Serializable {
        private static final long serialVersionUID = 1L;
        
        // 예약 정보 
        private String reservationId; // 예약 고유 ID (생성시간+ID 등)
        private String roomName;      // 강의실
        private LocalDate date;       // 날짜
        private String day;           // 요일
        private String periodInfo;    // 교시 (시간)
        private String purpose;       // 사유
        private int partnerCount;     // 인원 수
        private Status status;        // PENDING, APPROVED, REJECTED
        private String rejectReason;  // 거절 사유
        
        // 사용자 정보 
        private String userId;        // 예약자 학번
        private String userName;      // 예약자 이름
        private Role roleType;        // 예약자 구분(학생, 교수)

        public Response() {}

        // Getter
        public String getReservationId() {
            return reservationId;
        }

        public String getRoomName() {
            return roomName;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDay() {
            return day;
        }

        public String getPeriodInfo() {
            return periodInfo;
        }

        public String getPurpose() {
            return purpose;
        }

        public int getPartnerCount() {
            return partnerCount;
        }

        public Status getStatus() {
            return status;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public Role getRoleType() {
            return roleType;
        }

        // Setter
        public void setReservationId(String reservationId) {
            this.reservationId = reservationId;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public void setPeriodInfo(String periodInfo) {
            this.periodInfo = periodInfo;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public void setPartnerCount(int partnerCount) {
            this.partnerCount = partnerCount;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public void setRejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setRoleType(Role roleType) {
            this.roleType = roleType;
        }
    }
}
