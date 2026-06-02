/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

/**
 *
 * @author sunho
 */
public class Reservation {
    private String time;       // 시간대 (예: "09:00-09:50")
    private String subject;    // 과목명 또는 예약 목적 (예: "데이터사이언스", "개인자습")
    private String professor;  // 담당 교수 또는 예약자 이름 (학생 학번/이름 등)
    private int status;        // 💡 상태 번호 (0: 빈시간, 1: 정규수업, 2: 학생예약, 3: 교수예약)

    // 기본 생성자 (Jackson 역직렬화용)
    public Reservation() {}

    // 생성자
    public Reservation(String time, String subject, String professor, int status) {
        this.time = time;
        this.subject = subject;
        this.professor = professor;
        this.status = status;
    }

    // Getter & Setter
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getProfessor() { return professor; }
    public void setProfessor(String professor) { this.professor = professor; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
