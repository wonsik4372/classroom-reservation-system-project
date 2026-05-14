/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 강의실 시간표 데이터
 * @author sunho
 */

public class ScheduleData {
    // 강의실명(예: "911") -> Classroom 객체
    private Map<String, Classroom> classrooms = new LinkedHashMap<>();
    
    // 건물 번호, 강의실 번호, 요일, 교시, 강의내용, 강사 필요하지않나? 
    private int deptNo = 0;
    private int roomNo = 0;
    private String day = "";
    private int time = 0;

    public ScheduleData() {}

    public Map<String, Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(Map<String, Classroom> classrooms) { this.classrooms = classrooms; }
}
