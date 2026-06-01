package cse.se.CRS.server;

import cse.se.CRS.common.ScheduleData;
import cse.se.CRS.common.Classroom;
import cse.se.CRS.common.Reservation;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.writers.CSVWriter;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm; // 💡 기존의 잘 작동하던 알고리즘 복구
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleInitializer {

    // 대학교 표준 시간표 구조 정의 (1교시 ~ 9교시)
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

        // 하위 요소 중 '폴더(디렉토리)'만 골라냅니다. (각 폴더명 = 건물명)
        File[] buildingFolders = rootFolder.listFiles(File::isDirectory);

        if (buildingFolders == null || buildingFolders.length == 0) {
            System.out.println("⚠️ 하위 건물 폴더가 존재하지 않습니다: " + rootFolderPath);
            return;
        }

        // 출력 디렉토리가 없으면 생성
        File outDir = new File(outputDirPath);
        if (!outDir.exists()) outDir.mkdirs();

        for (File buildingFolder : buildingFolders) {
            String buildingName = buildingFolder.getName(); // 예: "정보공학관"
            
            // 폴더명을 활용해 출력될 JSON 파일명을 동적으로 지정합니다.
            String jsonOutputPath = new File(outDir, buildingName + ".json").getAbsolutePath();
            
            System.out.println("🚀 [" + buildingName + "] 건물 시간표 구조화 시작 -> " + jsonOutputPath);
            
            // 층 폴더 구조를 탐색하여 JSON으로 변환하는 전용 메소드를 호출합니다.
            convertSingleBuildingWithFloorsToJson(buildingFolder, jsonOutputPath, buildingName);
        }
    }

    // 2단계: 건물 내부의 층(Floor) 폴더들을 순회하여 복합 계층 JSON 구조로 변환
    private void convertSingleBuildingWithFloorsToJson(File buildingFolder, String jsonOutputPath, String buildingName) {
        try {
            File[] floorFolders = buildingFolder.listFiles(File::isDirectory);
            if (floorFolders == null || floorFolders.length == 0) {
                System.out.println("⚠️ [" + buildingName + "] 내부에 층(Floor) 폴더가 없습니다.");
                return;
            }

            // 💡 건물(Building) 바로 밑에 들어갈 층(Floor) 구조 매핑 테이블 (순서 보장)
            Map<String, Map<String, Object>> floorsJsonMap = new LinkedHashMap<>();

            for (File floorFolder : floorFolders) {
                String floorName = floorFolder.getName().trim(); // 예: "9층"
                
                File[] pdfFiles = floorFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfFiles == null || pdfFiles.length == 0) {
                    continue; // PDF 파일이 없는 층 디렉토리는 패스
                }

                System.out.println("  └ 🏢 " + floorName + " 탐색 중... (PDF 파일: " + pdfFiles.length + "개)");

                // 💡 각 층별로 독립된 스코프의 ScheduleData 인스턴스를 생성하여 데이터 섞임 방지
                ScheduleData floorScheduleData = new ScheduleData();

                for (File pdfFile : pdfFiles) {
                    // 보내주신 코드의 잘 작동하는 원본 파싱 로직을 그대로 태워 데이터를 완벽히 수집합니다.
                    parseSinglePdf(pdfFile, floorScheduleData);
                }

                // 💡 층 내부 파싱이 완료되면, 축적된 데이터를 추출하여 계층형 JSON 구조로 변환
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
                    
                    // 완성된 층 단위의 강의실 리스트를 상위 빌딩 구조에 매핑
                    floorsJsonMap.put(floorName, classroomsInFloor);
                }
            }

            // 💡 요구사항: buildingName 바로 다음에 floors 맵이 오도록 구조 정의
            Map<String, Object> jsonWrapper = new LinkedHashMap<>();
            jsonWrapper.put("buildingName", buildingName);
            jsonWrapper.put("floors", floorsJsonMap);

            // 파일 저장 (Jackson 라이브러리의 자동 스트림 close 메커니즘 사용으로 Stream closed 에러 완전 차단)
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
        PDDocument document = PDDocument.load(pdfFile);
        String classroomName = pdfFile.getName().replace(".pdf", "").replace(".PDF", "") + "호";
        
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

        // 💡 강의실 생성 시점에 모든 요일, 모든 교시를 "0"(빈 시간)으로 먼저 가득 채워 초기화합니다.
        Classroom classroom = scheduleData.getClassrooms().computeIfAbsent(classroomName, k -> new Classroom());
        initializeEmptySchedule(classroom);

        ObjectExtractor extractor = new ObjectExtractor(document);
        SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm(); // 💡 원본 알고리즘 유지
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
        document.close();
    }

    // 💡 모든 요일과 교시를 상태 0(빈 시간)으로 선배치하는 헬퍼 메소드
    private void initializeEmptySchedule(Classroom classroom) {
        for (String day : DAYS_HEADER) {
            List<Reservation> dailyList = classroom.getSchedule().computeIfAbsent(day, k -> new ArrayList<>());
            dailyList.clear(); // 기존 데이터 보장 청소
            
            // 1교시부터 9교시까지 빈 시간 구조 생성
            for (Map.Entry<String, String> entry : STANDARD_TIME_SLOTS.entrySet()) {
                String period = entry.getKey(); // "1교시"
                String timeRange = entry.getValue(); // "09:00-09:50"
                
                // 기본 상태: 빈 시간(0), 과목명(빈 시간), 담당교수(없음)
                dailyList.add(new Reservation(timeRange, "빈 시간", "없음", 0));
            }
        }
    }

    // 기존에 검증 완료된 CSV 행렬 분해 및 과목명/교수명 매핑 파서 코어 로직 유지
    private void parseCleanCsv(String csvContent, Classroom classroom) throws IOException {
        byte[] bytes = csvContent.getBytes("UTF-8");
        String utf8Csv = new String(bytes, "UTF-8");
        
        // 행 분리 (따옴표 내부의 줄바꿈은 무시하는 정규식)
        String[] rows = utf8Csv.split("\\r?\\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String row : rows) {
            if (row.trim().isBlank()) continue;

            // 💡 쉼표(,)를 기준으로 나누되, 연속된 쉼표 사이의 빈 값도 유실되지 않도록 -1 옵션 부여
            String[] rawCols = row.split(",", -1);
            List<String> cols = new ArrayList<>();
            for (String col : rawCols) {
                // 앞뒤 따옴표 및 불필요한 공백 제거
                String cleanCol = col.trim().replaceAll("^\"|\"$", "").trim();
                cols.add(cleanCol);
            }

            if (cols.isEmpty()) continue;

            String firstCol = cols.get(0).replaceAll("\\s+", " ").trim();
            String matchedPeriod = detectPeriod(firstCol); 
            if (matchedPeriod.isEmpty()) {
                continue; // 교시 정보가 없는 헤더 행 등은 패스
            }

            // 💡 인덱스 일대일 대응 및 스케줄 주입
            for (int i = 0; i < DAYS_HEADER.length; i++) {
                int csvColIdx = i + 1; // '월'은 인덱스 1, '화'는 인덱스 2...
                
                if (csvColIdx >= cols.size()) break;

                String cleanedCell = cols.get(csvColIdx).replaceAll("\\s+", " ").replace("(학)", "").trim();

                // 셀에 실제 수업 정보가 채워져 있는 경우에만 덮어쓰기
                if (!cleanedCell.isEmpty()) {
                    String subject = "과목";
                    String professor = "없음"; 

                    String[] words = cleanedCell.split(" ");
                    
                    // 💡 과목명/교수명 분리 예외 처리 보완 구조
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
                                res.setStatus(1); // 정규 수업 상태(1) 부여
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // 텍스트에서 교시 정규 헤더 정보를 추출하는 기존 헬퍼 로직 유지
    private String detectPeriod(String rawText) {
        for (String period : STANDARD_TIME_SLOTS.keySet()) {
            if (rawText.contains(period)) {
                return period;
            }
        }
        for (Map.Entry<String, String> entry : STANDARD_TIME_SLOTS.entrySet()) {
            String timeRange = entry.getValue();
            String startHour = timeRange.split("-")[0]; // "09:00"
            if (rawText.contains(startHour)) {
                return entry.getKey();
            }
        }
        return "";
    }
}