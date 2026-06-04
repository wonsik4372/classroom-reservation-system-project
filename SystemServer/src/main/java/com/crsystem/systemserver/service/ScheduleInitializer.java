package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ScheduleData;
import com.crsystem.common.dto.Classroom;
import com.crsystem.common.dto.ClassroomInfo;
import com.crsystem.common.dto.Reservation;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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

    public void convertAllTimetableToJsons(String rootFolderPath, String outputDirPath) {
        File rootFolder = new File(rootFolderPath);

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.out.println("⚠️ 루트 폴더 경로가 존재하지 않습니다: " + rootFolderPath);
            return;
        }

        File[] yearFolders = rootFolder.listFiles(File::isDirectory);

        if (yearFolders == null || yearFolders.length == 0) {
            System.out.println("⚠️ 하위 년도 폴더가 존재하지 않습니다: " + rootFolderPath);
            return;
        }

        File outDir = new File(outputDirPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        for (File yearFolder : yearFolders) {
            String yearName = yearFolder.getName();
            File outFile = new File(outDir, yearName + ".json");

            if (outFile.exists()) {
                long jsonModified = outFile.lastModified();
                boolean anyExcelNewer = collectExcelsRecursively(yearFolder)
                        .stream()
                        .anyMatch(excel -> excel.lastModified() > jsonModified);
                if (!anyExcelNewer) {
                    System.out.println("⏭️ [" + yearName + ".json] 변경 사항 없음, 스킵합니다.");
                    continue;
                }
            }

            System.out.println("📅 [" + yearName + "] 년도 마스터 시간표 동기화 및 구조화 시작...");
            
            Map<String, Object> yearJsonStructure = parseYearFolderHierarchy(yearFolder);

            ObjectMapper mapper = new ObjectMapper();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, yearJsonStructure);
                System.out.println("🎉 [" + yearName + ".json] 마스터 데이터 생성 완료 -> " + outFile.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("❌ 파일 저장 중 오류 발생 (" + yearName + ".json): " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private List<File> collectExcelsRecursively(File folder) {
        List<File> result = new ArrayList<>();
        File[] items = folder.listFiles();
        if (items == null) return result;
        for (File item : items) {
            if (item.isDirectory()) {
                result.addAll(collectExcelsRecursively(item));
            } else {
                String name = item.getName().toLowerCase();
                if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private Map<String, Object> parseYearFolderHierarchy(File yearFolder) {
        Map<String, Object> yearMap = new LinkedHashMap<>();
        
        File[] semesterFolders = yearFolder.listFiles(File::isDirectory);
        if (semesterFolders == null) return yearMap;

        for (File semesterFolder : semesterFolders) {
            String semesterName = semesterFolder.getName();
            System.out.println(" ├─ 📝 [학기] " + semesterName);
            
            Map<String, Object> buildingsMap = new LinkedHashMap<>();
            File[] buildingFolders = semesterFolder.listFiles(File::isDirectory);
            if (buildingFolders == null) continue;

            for (File buildingFolder : buildingFolders) {
                String buildingName = buildingFolder.getName();
                System.out.println(" │   ├─ 🏢 [건물] " + buildingName);
                
                Map<String, Map<String, Object>> floorsMap = new LinkedHashMap<>();
                File[] floorFolders = buildingFolder.listFiles(File::isDirectory);
                if (floorFolders == null) continue;

                for (File floorFolder : floorFolders) {
                    String floorName = floorFolder.getName().trim();
                    
                    File[] excelFiles = floorFolder.listFiles((dir, name) -> {
                        String lower = name.toLowerCase();
                        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
                    });
                    if (excelFiles == null || excelFiles.length == 0) continue;
                    
                    System.out.println(" │   │   ├─ 🏷️ [층] " + floorName + " (엑셀 파일: " + excelFiles.length + "개)");

                    ScheduleData floorScheduleData = new ScheduleData();
                    for (File excelFile : excelFiles) {
                        try {
                            parseSingleExcel(excelFile, floorScheduleData);
                        } catch (IOException e) {
                            System.err.println(" │   │   │  ❌ 엑셀 파싱 실패: " + excelFile.getName());
                        }
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
                        floorsMap.put(floorName, classroomsInFloor);
                    }
                }
                buildingsMap.put(buildingName, floorsMap);
            }
            yearMap.put(semesterName, buildingsMap);
        }
        return yearMap;
    }

    private void parseSingleExcel(File excelFile, ScheduleData scheduleData) throws IOException {
        String classroomName = excelFile.getName().replaceAll("(?i)\\.xlsx?$", "");
        if (!classroomName.endsWith("호")) {
            classroomName += "호";
        }

        Classroom classroom = scheduleData.getClassrooms().computeIfAbsent(classroomName, k -> new Classroom());
        initializeEmptySchedule(classroom);

        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream is = new FileInputStream(excelFile);
             Workbook workbook = excelFile.getName().toLowerCase().endsWith(".xlsx") ?
                                 new XSSFWorkbook(is) : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int headerRowIdx = -1;
            Map<Integer, String> colToDay = new HashMap<>();

            boolean isLab = false;
            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 10); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String currentDay = null;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = dataFormatter.formatCellValue(row.getCell(c)).replaceAll("\\s+", "");
                    if (cellVal.contains("실습실")) {
                        isLab = true;
                    }
                    if (cellVal.contains("구분")) {
                        headerRowIdx = r;
                    }
                    if (headerRowIdx != -1) {
                        if (Arrays.asList(DAYS_HEADER).contains(cellVal)) {
                            currentDay = cellVal;
                        }
                        if (currentDay != null) {
                            colToDay.put(c, currentDay);
                        }
                    }
                }
                if (headerRowIdx != -1) break;
            }

            ClassroomInfo info = classroom.getInfo();
            if (isLab) {
                info.setFeatures("실습실");
                info.setCapacity(40);
                info.setComputerCount(40);
            } else {
                info.setCapacity(60);
                info.setComputerCount(0);
            }

            if (headerRowIdx == -1) return; 

            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String periodCell = "";
                for (int c = 0; c <= 3; c++) {
                    String val = dataFormatter.formatCellValue(row.getCell(c));
                    if (val.contains("교시")) {
                        periodCell = val;
                        break;
                    }
                }

                String matchedPeriod = detectPeriod(periodCell);
                if (matchedPeriod.isEmpty()) continue; 

                String targetTimeRange = STANDARD_TIME_SLOTS.get(matchedPeriod);
                Row nextRow = sheet.getRow(r + 1); 

                for (Map.Entry<Integer, String> entry : colToDay.entrySet()) {
                    int colIdx = entry.getKey();
                    String day = entry.getValue();

                    String rawSubject = dataFormatter.formatCellValue(row.getCell(colIdx));
                    String subject = rawSubject.replaceAll("\\(학\\)|\\(대\\)|\\(행\\)|\\(경\\)|\\(산\\)|\\(교\\)|\\(중\\)|\\(영\\)", "").trim();

                    if (!subject.isEmpty() && !subject.equals("빈 시간")) {
                        String professor = "없음";
                        
                        // 💡 주변 셀 탐색 로직: 셀 병합으로 인해 교수가 좌우 한두 칸 밀려있어도 무조건 찾아냄
                        if (nextRow != null) {
                            for (int offset = -2; offset <= 2; offset++) {
                                int pColIdx = colIdx + offset;
                                if (pColIdx >= 0 && pColIdx < nextRow.getLastCellNum()) {
                                    String pVal = dataFormatter.formatCellValue(nextRow.getCell(pColIdx)).trim();
                                    
                                    // 공백이 아니고 범례(예: (학))가 아닌 문자열을 교수로 취급
                                    if (!pVal.isEmpty() && !pVal.matches(".*\\([가-힣]\\).*")) {
                                        professor = pVal;
                                        break;
                                    }
                                }
                            }
                        }

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