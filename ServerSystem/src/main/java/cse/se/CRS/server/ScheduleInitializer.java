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
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
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

   public void convertAllBuildingsToJsons(String rootFolderPath, String outputDirPath) {
        File rootFolder = new File(rootFolderPath);
        
        // 1. 하위 요소 중 '폴더(디렉토리)'만 골라냅니다. (각 폴더명 = 건물명)
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
            
            // 2. 폴더명을 활용해 출력될 JSON 파일명을 동적으로 지정합니다.
            String jsonOutputPath = new File(outDir, buildingName + ".json").getAbsolutePath();
            
            System.out.println("🚀 [" + buildingName + "] 건물 시간표 파싱 시작 -> " + jsonOutputPath);
            
            // 기존에 작성하신 변환 로직을 건물명 인자를 추가하여 호출합니다.
            convertSingleBuildingFolderToJson(buildingFolder.getAbsolutePath(), jsonOutputPath, buildingName);
        }
    }

    // 기존 convertMultiplePdfsToJson 메서드를 리팩토링 및 건물명 파라미터 추가
    private void convertSingleBuildingFolderToJson(String pdfFolderPath, String jsonOutputPath, String buildingName) {
        try {
            File folder = new File(pdfFolderPath);
            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

            if (pdfFiles == null || pdfFiles.length == 0) {
                System.out.println("⚠️ PDF 파일이 폴더에 존재하지 않습니다: " + pdfFolderPath);
                return;
            }

            ScheduleData totalScheduleData = new ScheduleData();
            
            /* * 💡 [중요] 만약 ScheduleData 클래스에 건물명을 담을 필드가 없다면 
             * 아래처럼 맵을 커스텀하거나 ScheduleData 내부에 `private String buildingName;` 필드와 Getter/Setter를 추가해 주세요.
             * 여기서는 ScheduleData 내부 혹은 JSON 변환 최상위에 담긴다고 가정합니다.
             */
            // totalScheduleData.setBuildingName(buildingName); // <- ScheduleData 필드 추가 시 사용 가능

            for (File pdfFile : pdfFiles) {
                parseSinglePdf(pdfFile, totalScheduleData);
            }

            // 만약 ScheduleData 소스코드를 고칠 수 없어서 래핑해서 json 안으로 밀어 넣고 싶다면 아래 주석을 참고하세요.
            Map<String, Object> jsonWrapper = new LinkedHashMap<>();
            jsonWrapper.put("buildingName", buildingName); // JSON 안에 건물명 명시
            jsonWrapper.put("classrooms", totalScheduleData.getClassrooms());

            ObjectMapper mapper = new ObjectMapper();
            File outFile = new File(jsonOutputPath);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {
                // totalScheduleData 대신 래핑한 jsonWrapper를 써주면 파일 내부에 건물명이 깔끔하게 들어갑니다.
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, jsonWrapper);
            }
            System.out.println("✅ [" + buildingName + "] 변환 완료!");

        } catch (Exception e) {
            System.err.println("❌ 실행 오류 발생 (" + buildingName + "):");
            e.printStackTrace();
        }
    }

    private void parseSinglePdf(File pdfFile, ScheduleData scheduleData) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        String classroomName = pdfFile.getName().replace(".pdf", "") + "호";
        
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

        // 💡 [핵심] 강의실 생성 시점에 모든 요일, 모든 교시를 "0"(빈 시간)으로 먼저 가득 채워 초기화합니다.
        Classroom classroom = scheduleData.getClassrooms().computeIfAbsent(classroomName, k -> new Classroom());
        initializeEmptySchedule(classroom);

        ObjectExtractor extractor = new ObjectExtractor(document);
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

    private void parseCleanCsv(String csvContent, Classroom classroom) throws IOException {
    byte[] bytes = csvContent.getBytes("UTF-8");
    String utf8Csv = new String(bytes, "UTF-8");
    
    // 행 분리 (따옴표 내부의 줄바꿈은 무시하는 정규식)
    String[] rows = utf8Csv.split("\\r?\\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    for (String row : rows) {
        if (row.trim().isBlank()) continue;

        // 💡 해결책 1: 쉼표(,)를 기준으로 나누되, 연속된 쉼표 사이의 빈 값도 유실되지 않도록 -1 옵션 부여
        // 양 끝의 따옴표나 공백 처리는 분리 후 개별 진행합니다.
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

        // 💡 해결책 2: 인덱스 변조 버그 제거 및 일대일 대응
        // 데이터 구조는 [0: 교시, 1: 월, 2: 화, 3: 수, 4: 목, 5: 금] 형태여야 합니다.
        // 추출된 데이터가 최소한 금요일(인덱스 5)까지는 존재해야 안전하게 매핑 가능합니다.
        for (int i = 0; i < DAYS_HEADER.length; i++) {
            int csvColIdx = i + 1; // '월'은 인덱스 1, '화'는 인덱스 2...
            
            if (csvColIdx >= cols.size()) break;

            String cleanedCell = cols.get(csvColIdx).replaceAll("\\s+", " ").replace("(학)", "").trim();

            // 셀에 실제 수업 정보가 채워져 있는 경우에만 덮어쓰기
            if (!cleanedCell.isEmpty()) {
                String subject = "과목";
                String professor = "없음"; // 기본값 변경 (교수가 없을 수도 있으므로)

                String[] words = cleanedCell.split(" ");
                
                // 💡 해결책 3: 과목명/교수명 분리 예외 처리 보완
                if (words.length >= 2) {
                    // 마지막 단어가 한글 2~4글자 이름 형태인 경우 대다수 교수명으로 유추 가능
                    String candidateProf = words[words.length - 1];
                    if (candidateProf.matches("^[가-힣]{2,4}$")) {
                        professor = candidateProf;
                        subject = cleanedCell.substring(0, cleanedCell.lastIndexOf(professor)).trim();
                    } else {
                        // 마지막 단어가 이름 형식이 아니면 전체를 과목명으로 판단
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

    // 텍스트에서 "1교시", "2교시" 혹은 "09:00" 등의 시간 정보를 가지고 교시 이름을 유추하는 헬퍼
    private String detectPeriod(String rawText) {
        for (String period : STANDARD_TIME_SLOTS.keySet()) {
            if (rawText.contains(period)) {
                return period;
            }
        }
        // "1교시" 문자열이 안 보이면 "09:00-09:50" 같은 시간 텍스트로 역추적
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