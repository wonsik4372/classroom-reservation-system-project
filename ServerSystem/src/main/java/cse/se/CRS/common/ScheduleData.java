package cse.se.CRS.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScheduleData {
    // 강의실명(예: "911") -> Classroom 객체
    private Map<String, Classroom> classrooms = new LinkedHashMap<>();

    public ScheduleData() {}

    public Map<String, Classroom> getClassrooms() { return classrooms; }
    public void setClassrooms(Map<String, Classroom> classrooms) { this.classrooms = classrooms; }
}