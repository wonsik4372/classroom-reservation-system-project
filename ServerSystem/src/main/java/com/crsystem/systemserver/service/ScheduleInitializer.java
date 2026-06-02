package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ScheduleData;
import com.crsystem.common.dto.Classroom;
import com.crsystem.common.dto.Reservation;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.writers.CSVWriter;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class ScheduleInitializer {

    private static final Map<String, String> STANDARD_TIME_SLOTS = new LinkedHashMap<>();
    static {
        STANDARD_TIME_SLOTS.put("1교시", "09:00-09:50");
        STANDARD_TIME_SLOTS.put("2교시", "10:00-10:50");
        STANDARD_TIME_SLOTS.put("3교시", "11:00-11:50");
        STANDARD_TIME_SLOTS.put("4교시", "12:00-12:50");
        STANDARD_TIME_SLOTS.put("5교시", "13:00-13:50");
        STANDARD_TIME_SLOTS.put("6교시", "14:00-14:50");
        STANDARD_TIME_SLOTS.put("7교시", "15:00-15:50");
        STANDARD_TIME_SLOTS.put("8교시", "16:00-16:50");
        STANDARD_TIME_SLOTS.put("9교시", "17:00-17:50");
    }

    private static final String[] DAYS_HEADER = {"월", "화", "수", "목", "금"};

    // 1단계: 루트 폴더에서 각 건물 폴더 탐색
    public void convertAllBuildingsToJsons(String rootFolderPath, String outputDirPath) {
        File rootFolder = new File(rootFolderPath);

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.out.println("⚠️ 루트 폴더 경로가 존재하지 않습니다: " + rootFolderPath);
            return;
        }

        File[] buildingFolders = rootFolder.listFiles(File::isDirectory);

        if (buildingFolders == null || buildingFolders.length == 0) {
            System.out.println("⚠️ 하위 건물 폴더가 존재하지 않습니다: " + rootFolderPath);
            return;
        }

        File outDir = new File(outputDirPath);
        if (!outDir.exists()) outDir.mkdirs();

        for (File buildingFolder : buildingFolders) {
            String buildingName = buildingFolder.getName();
            File outFile = new File(outDir, buildingName + ".json");

            // JSON이 존재하고 모든 PDF보다 새로우면 재변환 스킵
            if (outFile.exists()) {
                long jsonModified = outFile.lastModified();
                boolean anyPdfNewer = collectPdfsRecursively(buildingFolder)
                        .stream()
                        .anyMatch(pdf -> pdf.lastModified() > jsonModified);
                if (!anyPdfNewer) {
                    System.out.println("⏭️ [" + buildingName + "] 변경 없음, 스킵.");
                    continue;
                }
            }

            System.out.println("🚀 [" + buildingName + "] 건물 시간표 구조화 시작 -> " + outFile.getAbsolutePath());
            convertSingleBuildingWithFloorsToJson(buildingFolder, outFile.getAbsolutePath(), buildingName);
        }
    }

    // 폴더 및 하위 폴더의 모든 PDF를 재귀적으로 수집 (스킵 여부 판단용)
    private List<File> collectPdfsRecursively(File folder) {
        List<File> result = new ArrayList<>();
        File[] items = folder.listFiles();
        if (items == null) return result;
        for (File item : items) {
            if (item.isDirectory()) {
                result.addAll(collectPdfsRecursively(item));
            } else if (item.getName().toLowerCase().endsWith(".pdf")) {
                result.add(item);
            }
        }
        return result;
    }

    // 2단계: 건물 내부의 층(Floor) 폴더들을 순회하여 복합 계층 JSON 구조로 변환
    private void convertSingleBuildingWithFloorsToJson(File buildingFolder, String jsonOutputPath, String buildingName) {
        try {
            File[] floorFolders = buildingFolder.listFiles(File::isDirectory);
            if (floorFolders == null || floorFolders.length == 0) {
                System.out.println("⚠️ [" + buildingName + "] 내부에 층(Floor) 폴더가 없습니다.");
                return;
            }

            Map<String, Map<String, Object>> floorsJsonMap = new LinkedHashMap<>();

            for (File floorFolder : floorFolders) {
                String floorName = floorFolder.getName().trim();

                File[] pdfFiles = floorFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfFiles == null || pdfFiles.length == 0) {
                    continue;
                }

                System.out.println("  └ 🏢 " + floorName + " 탐색 중... (PDF 파일: " + pdfFiles.length + "개)");

                ScheduleData floorScheduleData = new ScheduleData();

                for (File pdfFile : pdfFiles) {
                    parseSinglePdf(pdfFile, floorScheduleData);
                }

                Map<String, Object> classroomsInFloor = new LinkedHashMap<>();
                Map<String, Classroom> parsedClassrooms = floorScheduleData.getClassrooms();

                if (parsedClassrooms != null && !parsedClassrooms.isEmpty()) {
                    for (Map.Entry<String, Classroom> entry : parsedClassrooms.entrySet()) {
                        String clsName = entry.getKey();
                        Classroom clsObj = entry.getValue();

                        Map<String, Object> classroomDetails = new LinkedHashMap<>();
                        classroomDetails.put("info", clsObj.getInfo());
                        classroomDetails.put("schedule", clsObj.getSchedule());

                        classroomsInFloor.put(clsName, classroomDetails);
                    }

                    floorsJsonMap.put(floorName, classroomsInFloor);
                }
            }

            Map<String, Object> jsonWrapper = new LinkedHashMap<>();
            jsonWrapper.put("buildingName", buildingName);
            jsonWrapper.put("floors", floorsJsonMap);

            ObjectMapper mapper = new ObjectMapper();
            File outFile = new File(jsonOutputPath);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, jsonWrapper);
            }

            System.out.println("✅ [" + buildingName + "] 변환 완료!");

        } catch (Exception e) {
            System.err.println("❌ 실행 오류 발생 (" + buildingName + "):");
            e.printStackTrace();
        }
    }

    // 3단계: 단일 PDF 파일 스트림 로드 및 스프레드시트 알고리즘 파싱 연동
    private void parseSinglePdf(File pdfFile, ScheduleData scheduleData) throws IOException {
        String classroomName = pdfFile.getName().replace(".pdf", "").replace(".PDF", "") + "호";

        try (PDDocument document = PDDocument.load(pdfFile);
             ObjectExtractor extractor = new ObjectExtractor(document)) {

            try {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(1);
                String firstPageText = stripper.getText(document);
                if (firstPageText != null && !firstPageText.isBlank()) {
                    String[] lines = firstPageText.split("\\r?\\n");
                    if (lines.length > 0 && !lines[0].trim().isBlank()) {
                        String candidate = lines[0].trim();
                        if (candidate.matches("^[0-9a-zA-Z가-힣\\s]+$")) {
                            classroomName = candidate;
                        }
                    }
                }
            } catch (Exception e) {
                // 예외 발생 시 파일명 기본값 유지
            }

            Classroom classroom = scheduleData.getClassrooms().computeIfAbsent(classroomName, k -> new Classroom());
            initializeEmptySchedule(classroom);

            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();
            PageIterator pages = extractor.extract();

            while (pages.hasNext()) {
                Page page = pages.next();
                List<Table> tables = algorithm.extract(page);

                for (Table table : tables) {
                    StringWriter stringWriter = new StringWriter();
                    CSVWriter csvWriter = new CSVWriter();
                    csvWriter.write(stringWriter, table);

                    parseCleanCsv(stringWriter.toString(), classroom);
                }
            }
        }
    }

    private void initializeEmptySchedule(Classroom classroom) {
        for (String day : DAYS_HEADER) {
            List<Reservation> dailyList = classroom.getSchedule().computeIfAbsent(day, k -> new ArrayList<>());
            dailyList.clear();

            for (Map.Entry<String, String> entry : STANDARD_TIME_SLOTS.entrySet()) {
                String timeRange = entry.getValue();
                dailyList.add(new Reservation(timeRange, "빈 시간", "없음", 0));
            }
        }
    }

    private void parseCleanCsv(String csvContent, Classroom classroom) throws IOException {
        byte[] bytes = csvContent.getBytes("UTF-8");
        String utf8Csv = new String(bytes, "UTF-8");

        String[] rows = utf8Csv.split("\\r?\\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String row : rows) {
            if (row.trim().isBlank()) continue;

            String[] rawCols = row.split(",", -1);
            List<String> cols = new ArrayList<>();
            for (String col : rawCols) {
                String cleanCol = col.trim().replaceAll("^\"|\"$", "").trim();
                cols.add(cleanCol);
            }

            if (cols.isEmpty()) continue;

            String firstCol = cols.get(0).replaceAll("\\s+", " ").trim();
            String matchedPeriod = detectPeriod(firstCol);
            if (matchedPeriod.isEmpty()) continue;

            for (int i = 0; i < DAYS_HEADER.length; i++) {
                int csvColIdx = i + 1;

                if (csvColIdx >= cols.size()) break;

                String cleanedCell = cols.get(csvColIdx).replaceAll("\\s+", " ").replace("(학)", "").trim();

                if (!cleanedCell.isEmpty()) {
                    String subject = "과목";
                    String professor = "없음";

                    String[] words = cleanedCell.split(" ");

                    if (words.length >= 2) {
                        String candidateProf = words[words.length - 1];
                        if (candidateProf.matches("^[가-힣]{2,4}$")) {
                            professor = candidateProf;
                            subject = cleanedCell.substring(0, cleanedCell.lastIndexOf(professor)).trim();
                        } else {
                            subject = cleanedCell;
                        }
                    } else {
                        subject = cleanedCell;
                    }

                    String day = DAYS_HEADER[i];
                    String targetTimeRange = STANDARD_TIME_SLOTS.get(matchedPeriod);
                    List<Reservation> dailyList = classroom.getSchedule().get(day);

                    if (dailyList != null) {
                        for (Reservation res : dailyList) {
                            if (res.getTime().equals(targetTimeRange)) {
                                res.setSubject(subject);
                                res.setProfessor(professor);
                                res.setStatus(1);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private String detectPeriod(String rawText) {
        for (String period : STANDARD_TIME_SLOTS.keySet()) {
            if (rawText.contains(period)) {
                return period;
            }
        }
        for (Map.Entry<String, String> entry : STANDARD_TIME_SLOTS.entrySet()) {
            String timeRange = entry.getValue();
            String startHour = timeRange.split("-")[0];
            if (rawText.contains(startHour)) {
                return entry.getKey();
            }
        }
        return "";
    }
}
