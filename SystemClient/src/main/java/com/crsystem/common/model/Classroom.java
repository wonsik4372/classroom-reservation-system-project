/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;

/**
 * 강의실 정보와 시간표 병합된 데이터
 * @author sunho
 */

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Classroom {
    private ClassroomInfo info = new ClassroomInfo(); // 기본값 세팅된 정보 객체
    private Map<String, List<Reservation>> schedule = new LinkedHashMap<>(); // 요일별 시간표

    public Classroom() {}

    public ClassroomInfo getInfo() { return info; }
    public void setInfo(ClassroomInfo info) { this.info = info; }

    public Map<String, List<Reservation>> getSchedule() { return schedule; }
    public void setSchedule(Map<String, List<Reservation>> schedule) { this.schedule = schedule; }
}