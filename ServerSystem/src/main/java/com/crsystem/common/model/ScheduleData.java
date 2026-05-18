/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전체 강의실의 시간표 데이터를 담는 래퍼 클래스
 */
public class ScheduleData {
    // 강의실명(예: "911") -> Classroom 객체
    private Map<String, Classroom> classrooms = new LinkedHashMap<>();
    
    private int deptNo = 0;
    private int roomNo = 0;
    private String day = "";
    private int time = 0;

    public ScheduleData() {}

    public Map<String, Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(Map<String, Classroom> classrooms) { this.classrooms = classrooms; }

    public int getDeptNo() { return deptNo; }
    public void setDeptNo(int deptNo) { this.deptNo = deptNo; }

    public int getRoomNo() { return roomNo; }
    public void setRoomNo(int roomNo) { this.roomNo = roomNo; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }
}