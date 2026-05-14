// 예약 정보 제공용 데이터 모델
package com.crsystem.systemclient.reservation;

import java.time.LocalDate;

public class ReservationInfo {

    public LocalDate date;      // 예약 날짜
    public String day;          // 요일
    public String periodInfo;   // 예약된 시간(교시)
    public String userType;     // 학생/교수 테스트 구분용
    public String purpose;      // 예약 사유
    public int partnerCount;    // 동반 인원
    public String status;       // 예약 확정/대기 중/예약 거절
    public String rejectReason = "-"; // 거절 사유
}
