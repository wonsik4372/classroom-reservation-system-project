package com.crsystem.common.model;

import java.io.Serializable;

/**
 * 신규 예약을 신청하기 위한 요청 패킷
 */
public class ReservationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;           // 예약 일자
    private String classroomName;  // 강의실 번호 (ex: "911")
    private String timeSlot;       // 시간대 (ex: "11:00-11:50")
    private String subject;        // 목적 또는 과목명
    private String applicantName;  // 신청자 이름/학번
    private int requesterType;     // 2: 학생, 3: 교수

    public ReservationRequest(String date, String classroomName, String timeSlot, String subject, String applicantName, int requesterType) {
        this.date = date;
        this.classroomName = classroomName;
        this.timeSlot = timeSlot;
        this.subject = subject;
        this.applicantName = applicantName;
        this.requesterType = requesterType;
    }

    public String getDate() { return date; }
    public String getClassroomName() { return classroomName; }
    public String getTimeSlot() { return timeSlot; }
    public String getSubject() { return subject; }
    public String getApplicantName() { return applicantName; }
    public int getRequesterType() { return requesterType; }
}