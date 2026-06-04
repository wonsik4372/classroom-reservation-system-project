package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

public class TimetableHandler {

    private static class Holder {
        private static final TimetableHandler INSTANCE = new TimetableHandler();
    }

    private static final String MASTERFILE_DIR = "data/masterfile/";
    private final ObjectMapper mapper = new ObjectMapper();

    private TimetableHandler() {}

    public static TimetableHandler getInstance() {
        return Holder.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public ResponseDTO getTimetable(String year) {
        String fileName = (year != null && !year.isBlank()) ? year : "2026";
        File jsonFile = new File(MASTERFILE_DIR + fileName + ".json");

        if (!jsonFile.exists()) {
            return new ResponseDTO(false, "시간표 파일을 찾을 수 없습니다: " + fileName + ".json", null);
        }

        try {
            Map<String, Object> data = mapper.readValue(jsonFile, LinkedHashMap.class);
            return new ResponseDTO(true, "시간표 조회 성공", data);
        } catch (IOException e) {
            System.err.println("❌ 시간표 JSON 로드 실패: " + e.getMessage());
            return new ResponseDTO(false, "시간표 로드 실패: " + e.getMessage(), null);
        }
    }

    /**
     * payload 구조: { "roomName": "23 정보공학관 9층 911호", "capacity": 60,
     *                 "computerCount": 0, "status": "사용 가능", "features": "" }
     */
    @SuppressWarnings("unchecked")
    public synchronized ResponseDTO updateRoomInfo(Map<String, Object> payload) {
        String roomName = (String) payload.get("roomName");
        File jsonFile = new File(MASTERFILE_DIR + "2026.json");

        if (!jsonFile.exists()) {
            return new ResponseDTO(false, "시간표 파일이 없습니다.", null);
        }

        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.+?)\\s+(\\d+층)\\s+(\\d+호)$")
                    .matcher(roomName);
            if (!m.matches()) return new ResponseDTO(false, "강의실 이름 형식 오류: " + roomName, null);

            String building = m.group(1);
            String floor    = m.group(2);
            String roomKey  = m.group(3);

            Map<String, Object> root = mapper.readValue(jsonFile, LinkedHashMap.class);

            for (Object semObj : root.values()) {
                Map<String, Object> semMap = (Map<String, Object>) semObj;
                Map<String, Object> buildingMap = (Map<String, Object>) semMap.get(building);
                if (buildingMap == null) continue;
                Map<String, Object> floorMap = (Map<String, Object>) buildingMap.get(floor);
                if (floorMap == null) continue;
                Map<String, Object> roomData = (Map<String, Object>) floorMap.get(roomKey);
                if (roomData == null) continue;

                Map<String, Object> info = (Map<String, Object>) roomData.get("info");
                if (info == null) { info = new LinkedHashMap<>(); roomData.put("info", info); }

                info.put("capacity",      payload.get("capacity"));
                info.put("computerCount", payload.get("computerCount"));
                info.put("status",        payload.get("status"));
                info.put("features",      payload.get("features"));

                mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, root);
                System.out.println("💾 강의실 정보 수정 완료: " + roomName);
                return new ResponseDTO(true, "강의실 정보가 수정되었습니다.", null);
            }

            return new ResponseDTO(false, "해당 강의실을 찾을 수 없습니다: " + roomName, null);

        } catch (IOException e) {
            System.err.println("❌ 강의실 정보 수정 실패: " + e.getMessage());
            return new ResponseDTO(false, "수정 실패: " + e.getMessage(), null);
        }
    }

    public ResponseDTO getAvailableYears() {
        File folder = new File(MASTERFILE_DIR);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        java.util.List<String> years = new java.util.ArrayList<>();
        if (files != null) {
            for (File file : files) {
                years.add(file.getName().replace(".json", ""));
            }
        }
        years.sort((a, b) -> b.compareTo(a));

        return new ResponseDTO(true, "연도 목록 조회 성공", years);
    }
}
