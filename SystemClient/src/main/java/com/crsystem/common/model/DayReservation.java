/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author sunho
 */
public class DayReservation {
    private String date; // "2026-05-24"
    private List<SingleBooking> bookings = new ArrayList<>();

    public DayReservation() {}

    public DayReservation(String date) {
        this.date = date;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<SingleBooking> getBookings() { return bookings; }
    public void setBookings(List<SingleBooking> bookings) { this.bookings = bookings; }

    // 예약 한 건을 표현하는 이너 클래스 (static inner class)
    public static class SingleBooking {
        private String classroomName; // "912호"
        private String time;          // "11:00-11:50"
        private String subject;       // "알고리즘 스터디"
        private String professor;     // "홍길동"
        private int status;           // 2: 학생, 3: 교수

        public SingleBooking() {}

        public SingleBooking(String classroomName, String time, String subject, String professor, int status) {
            this.classroomName = classroomName;
            this.time = time;
            this.subject = subject;
            this.professor = professor;
            this.status = status;
        }

        // Getter & Setter
        public String getClassroomName() { return classroomName; }
        public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getProfessor() { return professor; }
        public void setProfessor(String professor) { this.professor = professor; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
    }
}
