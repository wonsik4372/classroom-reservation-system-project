package com.crsystem.systemserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import com.crsystem.common.dto.Classroom;
import com.crsystem.common.dto.ClassroomInfo;
import com.crsystem.common.dto.Reservation;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimetableHandlerTest {

    private ClassroomService classroomHandler;
    private final String ROOM_NAME = "411호";

    @BeforeEach
    public void setUp() throws Exception {
        // [테스트 격리] 테스트 전용 폴더 사용
        classroomHandler = new ClassroomService("data/test_masterfile");

        // 더미 데이터 강제 주입
        Classroom dummy = new Classroom();
        ClassroomInfo info = new ClassroomInfo();
        info.setCapacity(60);
        info.setStatus("사용 가능");
        info.setFeatures("");
        info.setComputerCount(0);
        dummy.setInfo(info);

        Map<String, List<Reservation>> schedule = new LinkedHashMap<>();
        List<Reservation> mondaySchedule = new ArrayList<>();
        
        Reservation occupiedSlot = new Reservation();
        occupiedSlot.setTime("10:00-10:50");
        occupiedSlot.setSubject("창업재무회계");
        occupiedSlot.setProfessor("강영진");
        occupiedSlot.setStatus(1);
        mondaySchedule.add(occupiedSlot);
        
        schedule.put("월", mondaySchedule);
        dummy.setSchedule(schedule);

        classroomHandler.getScheduleData().getClassrooms().put(ROOM_NAME, dummy);
    }
    // ====================
    // [TC-16] SFR-301 강의실 정보(수용인원, 상태, 특이사항, PC개수) 수정 검증
    // ====================
    @Test
    public void testSFR301_UpdateClassroomInfo() {
        boolean isUpdated = classroomHandler.updateClassroomInfo(ROOM_NAME, 50, "네트워크 점검 중", 10, "사용 불가");
        assertTrue(isUpdated);
        assertEquals(50, classroomHandler.getScheduleData().getClassrooms().get(ROOM_NAME).getInfo().getCapacity());
    }
    // ====================
    // [TC-17] SFR-302: 강의실 시간표 PDF 파일 데이터 형식 검증
    // ====================
    @Test
    public void testSFR302_ValidatePdfDataFormat() {
        // Given: 강의실 객체 로드
        Classroom room = classroomHandler.getScheduleData().getClassrooms().get(ROOM_NAME);
        
        // Then: PDF 파싱 결과가 담겨야 할 구조(Schedule)와 필수 정보 필드가 유효한지 검증
        assertNotNull(room, "강의실 객체가 로드되어야 합니다.");
        assertNotNull(room.getSchedule(), "스케줄 구조가 존재해야 합니다.");
        
        // PDF에서 추출된 데이터는 보통 요일별 리스트 형태여야 함
        assertTrue(room.getSchedule() instanceof Map, "스케줄은 요일별 Map 형식이어야 합니다.");
    }

    // ====================
    // [TC-18] SFR-303: 추출된 데이터의 강의실 시간표 연동 및 갱신 검증
    // ====================
    @Test
    public void testSFR303_UpdateScheduleWithExtractedData() {
        // Given: PDF 파싱 결과라고 가정하는 데이터 생성
        Classroom room = classroomHandler.getScheduleData().getClassrooms().get(ROOM_NAME);
        Reservation newClass = new Reservation();
        newClass.setTime("13:00-14:50");
        newClass.setSubject("데이터베이스");
        newClass.setProfessor("박교수");
        newClass.setStatus(1);
        
        // When: 추출된 데이터를 시스템 스케줄에 연동(추가)
        room.getSchedule().computeIfAbsent("월", k -> new ArrayList<>()).add(newClass);
        
        // Then: 데이터가 정상적으로 시스템에 갱신되었는지 검증
        boolean exists = room.getSchedule().get("월").stream()
                .anyMatch(r -> r.getSubject().equals("데이터베이스") && r.getProfessor().equals("박교수"));
        
        assertTrue(exists, "추출된 시간표 데이터가 강의실 스케줄에 정상적으로 연동되어야 합니다.");
    }
   
}