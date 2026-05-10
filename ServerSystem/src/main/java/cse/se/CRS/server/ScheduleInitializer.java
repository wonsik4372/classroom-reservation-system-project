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

    public void convertMultiplePdfsToJson(String pdfFolderPath, String jsonOutputPath) {
        try {
            File folder = new File(pdfFolderPath);
            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

            if (pdfFiles == null || pdfFiles.length == 0) {
                System.out.println("⚠️ PDF 파일이 폴더에 존재하지 않습니다: " + pdfFolderPath);
                return;
            }

            ScheduleData totalScheduleData = new ScheduleData();

            for (File pdfFile : pdfFiles) {
                parseSinglePdf(pdfFile, totalScheduleData);
            }

            ObjectMapper mapper = new ObjectMapper();
            File outFile = new File(jsonOutputPath);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"))) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, totalScheduleData);
            }

        } catch (Exception e) {
            System.err.println("❌ 실행 오류 발생:");
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
        
        String[] rows = utf8Csv.split("\\r?\\n(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String row : rows) {
            if (row.trim().isBlank()) continue;

            List<String> cols = new ArrayList<>();
            Pattern p = Pattern.compile("\"([^\"]*)\"|(?<=,|^)([^,]*)(?:,|$)");
            Matcher m = p.matcher(row);
            while (m.find()) {
                String val = (m.group(1) != null) ? m.group(1) : m.group(2);
                cols.add(val == null ? "" : val.trim());
            }

            if (cols.isEmpty()) continue;

            String firstCol = cols.get(0).replaceAll("\\s+", " ").trim();
            
            // "1교시", "2교시" 형태나 시간 데이터 매칭 검출
            String matchedPeriod = detectPeriod(firstCol); // "1교시", "2교시" 등 매칭 추출
            if (matchedPeriod.isEmpty()) {
                continue; // 헤더이거나 유효하지 않은 행 패스
            }

            int rawColIdx = 1;
            for (int i = 0; i < DAYS_HEADER.length; i++) {
                if (DAYS_HEADER[i].equals("수") || DAYS_HEADER[i].equals("목")) {
                    while (rawColIdx < cols.size() && cols.get(rawColIdx).isEmpty() && cols.get(rawColIdx - 1).isEmpty()) {
                        rawColIdx++;
                    }
                }

                if (rawColIdx >= cols.size()) break;

                String rawCell = cols.get(rawColIdx).trim();
                rawColIdx++;

                String cleanedCell = rawCell.replaceAll("\\s+", " ").replace("\"", "").trim();

                // 셀에 실제 수업 정보가 채워져 있는 경우
                if (!cleanedCell.isEmpty()) {
                    String subject = "과목";
                    String professor = "담당교수";

                    cleanedCell = cleanedCell.replace("(학)", "").trim();
                    String[] words = cleanedCell.split(" ");
                    
                    if (words.length >= 2) {
                        professor = words[words.length - 1];
                        subject = cleanedCell.substring(0, cleanedCell.lastIndexOf(professor)).trim();
                    } else {
                        subject = cleanedCell;
                    }

                    String day = DAYS_HEADER[i];
                    
                    // 💡 [핵심] 0번으로 선배치된 데이터 중 "해당 요일의 해당 교시 시간대"를 찾아서 "1번(수업)"으로 덮어씁니다.
                    String targetTimeRange = STANDARD_TIME_SLOTS.get(matchedPeriod);
                    List<Reservation> dailyList = classroom.getSchedule().get(day);
                    if (dailyList != null) {
                        for (Reservation res : dailyList) {
                            if (res.getTime().equals(targetTimeRange)) {
                                res.setSubject(subject);
                                res.setProfessor(professor);
                                res.setStatus(1); // 정규 수업 상태 부여
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