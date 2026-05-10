package cse.se.CRS.common;

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