/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author sunho
 */
public class ScheduleData {
    // 강의실명(예: "911") -> Classroom 객체
    private Map<String, Classroom> classrooms = new LinkedHashMap<>();

    public ScheduleData() {}

    public Map<String, Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(Map<String, Classroom> classrooms) { this.classrooms = classrooms; }
}
