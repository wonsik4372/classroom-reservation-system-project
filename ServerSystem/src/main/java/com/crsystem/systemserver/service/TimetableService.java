package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimetableService {

    private static class Holder {
        private static final TimetableService INSTANCE = new TimetableService();
    }

    private static final String MASTERFILE_DIR = "data/masterfile/";
    private final ObjectMapper mapper = new ObjectMapper();

    private TimetableService() {}

    public static TimetableService getInstance() {
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
}
