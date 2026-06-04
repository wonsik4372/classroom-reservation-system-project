package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.model.ReservationCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 관리 전문가
 * @author wonsik
 */
public class ReservationHandler {

    private static class Holder {
        private static final ReservationHandler INSTANCE = new ReservationHandler();
    }

    private final ReservationCatalog catalog;
    private final ReservationFileManager fileManager;

    private ReservationHandler() {
        this.fileManager = new ReservationFileManager();
        this.catalog = new ReservationCatalog(fileManager.loadAll());
    }

    public static ReservationHandler getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // [SFR-401] 예약 등록 공통 - 강의실 상태·시간 중복 검증
    // [SFR-402] 교수 예약 - 보강/세미나 목적 예약 등록
    // [SFR-403] 교수 예약 - 사용 목적·참석 인원 등록
    // [SFR-404] 교수 예약 - 본인 예약 중복 시 예약 불가
    // [SFR-405] 교수 예약 - 최대 3교시 제한
    // [SFR-406] 교수 예약 - 즉시 반영 (APPROVED 상태)
    // [SFR-407] 교수 예약 - 학생 대기 예약 덮어쓰기(거부) 처리
    // [SFR-408] 학생 예약 - 개인/조별 학습 목적 사전 신청
    // [SFR-409] 학생 예약 - 동반 학생 수·학번·성명 등록
    // [SFR-410] 학생 예약 - 수용 인원 50% 초과 시 예약 불가 (Singleton 동기화)
    // [SFR-411] 학생 예약 - 최대 2교시 제한
    // [SFR-412] 학생 예약 - 최소 하루 전 사전 신청 (PENDING 상태)
    // [SFR-413] 교수 예약 - 이미 교수 예약 존재 시 추가 예약 불가
    // ====================
    public ResponseDTO addReservation(ReservationDTO.Response reservation) {
        try {
            // 사용 불가 강의실 예약 차단
            String roomStatus = getRoomStatus(reservation.getRoomName());
            if (!"사용 가능".equals(roomStatus)) {
                return new ResponseDTO(false, "예약 실패: 해당 강의실은 현재 사용 불가 상태입니다.", null);
            }

            // 하루 예약 교시 수 제한 검사
            int newPeriodCount = countPeriods(reservation.getPeriodInfo());
            int existingPeriodCount = catalog.getAllReservations().stream()
                    .filter(r -> r.getUserId().equals(reservation.getUserId())
                            && r.getDate() != null && r.getDate().equals(reservation.getDate())
                            && r.getStatus() != ReservationDTO.Status.REJECTED)
                    .mapToInt(r -> countPeriods(r.getPeriodInfo()))
                    .sum();

            int maxPeriods = reservation.getRoleType() == Role.STUDENT ? 2 : 3;
            if (existingPeriodCount + newPeriodCount > maxPeriods) {
                String roleLabel = reservation.getRoleType() == Role.STUDENT ? "학생" : "교수";
                return new ResponseDTO(false,
                        "예약 실패: " + roleLabel + "은 하루 최대 " + maxPeriods + "교시까지만 예약 가능합니다.", null);
            }

            // 학생 예약 시 수용 인원 50% 초과 여부 검증
            if (reservation.getRoleType() == Role.STUDENT) {
                int capacity = getRoomCapacity(reservation.getRoomName());
                if (capacity > 0) {
                    int maxAllowed = capacity / 2;
                    int currentOccupancy = catalog.getAllReservations().stream()
                            .filter(r -> r.getRoomName().equals(reservation.getRoomName())
                                    && r.getDate().equals(reservation.getDate())
                                    && isPeriodConflict(r.getPeriodInfo(), reservation.getPeriodInfo())
                                    && r.getStatus() != ReservationDTO.Status.REJECTED)
                            .mapToInt(ReservationDTO.Response::getPartnerCount)
                            .sum();
                    if (currentOccupancy + reservation.getPartnerCount() > maxAllowed) {
                        int remaining = Math.max(0, maxAllowed - currentOccupancy);
                        return new ResponseDTO(false,
                                "예약 실패: 해당 교시의 잔여석이 부족합니다. (잔여 " + remaining + "석)", null);
                    }
                }
            }

            for (ReservationDTO.Response existing : catalog.getAllReservations()) {
                if (isSameSlot(existing, reservation)) {
                    // 교수가 학생 슬롯을 덮어쓰는 경우: 학생 예약 거부 처리
                    if (existing.getRoleType() == Role.STUDENT
                            && reservation.getRoleType() == Role.PROFESSOR) {
                        existing.setStatus(ReservationDTO.Status.REJECTED);
                        existing.setRejectReason("교수 보강");
                    }
                }
            }

            catalog.addReservation(reservation);
            fileManager.saveAll(catalog.getAllReservations());

            return new ResponseDTO(true, "예약이 등록되었습니다.", null);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseDTO(false, "예약 등록 실패", null);
        }
    }

    // ====================
    // [SFR-201] 강의실 현황 조회 - 전체 예약 목록 반환
    // ====================
    public ResponseDTO getReservationList() {
        List<ReservationDTO.Response> reservationList = fileManager.loadAll();
        return new ResponseDTO(true, "전체 예약 조회 성공", reservationList);
    }

    // ====================
    // [SFR-501] 예약 상태 관리 - 승인 대기 중인 예약 목록 조회
    // ====================
    public ResponseDTO getPendingReservationList() {
        List<ReservationDTO.Response> pendingList = catalog.getAllReservations().stream()
                .filter(r -> r.getStatus() == ReservationDTO.Status.PENDING)
                .collect(Collectors.toList());
        return new ResponseDTO(true, "대기 예약 조회 성공", pendingList);
    }

    // ====================
    // [SFR-501] 예약 승인 - 대기 상태 예약을 APPROVED로 변경
    // [SFR-502] 예약 승인 - 승인 시 알림 생성 (NotificationService 호출)
    // [SFR-503] 예약 승인 - 단건 또는 복수 건 승인 가능
    // [SFR-504] 예약 승인 - 로그인 중인 학생에게 푸시 형태로 즉시 전달
    // ====================
    public ResponseDTO approveReservation(String reservationId) {
        try {
            ReservationDTO.Response target = catalog.findReservation(reservationId);

            if (target == null) {
                return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
            }

            if (target.getStatus() != ReservationDTO.Status.PENDING) {
                return new ResponseDTO(false, "대기 상태인 예약만 승인할 수 있습니다.", null);
            }

            target.setStatus(ReservationDTO.Status.APPROVED);
            fileManager.saveAll(catalog.getAllReservations());
            NotificationHandler.getInstance().notifyApproved(target);

            return new ResponseDTO(true, "예약이 승인되었습니다.", null);

        } catch (Exception e) {
            return new ResponseDTO(false, "승인 실패", null);
        }
    }

    // ====================
    // [STR-010] 예약 취소 - 대기/승인 상태 본인 예약만 취소 가능
    // ====================
    public ResponseDTO cancelReservation(String reservationId, String requesterId) {
        try {
            ReservationDTO.Response target = catalog.findReservation(reservationId);

            if (target == null) {
                return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
            }

            if (!target.getUserId().equals(requesterId)) {
                return new ResponseDTO(false, "본인의 예약만 취소할 수 있습니다.", null);
            }

            if (target.getStatus() == ReservationDTO.Status.REJECTED) {
                return new ResponseDTO(false, "이미 거부된 예약입니다.", null);
            }

            catalog.removeReservation(reservationId);
            fileManager.saveAll(catalog.getAllReservations());

            return new ResponseDTO(true, "예약이 취소되었습니다.", null);

        } catch (Exception e) {
            return new ResponseDTO(false, "취소 실패", null);
        }
    }

    // ====================
    // [SFR-505] 예약 거부 - 대기 상태 예약을 REJECTED로 변경
    // [SFR-506] 예약 거부 - 거부 사유 등록 필수
    // [SFR-507] 예약 거부 - 단건 또는 복수 건 거부 가능
    // [SFR-508] 예약 거부 - 로그인 여부와 관계없이 학생에게 알림 전달
    // ====================
    public ResponseDTO rejectReservation(String reservationId, String rejectReason) {
        try {
            ReservationDTO.Response target = catalog.findReservation(reservationId);

            if (target == null) {
                return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
            }

            if (target.getStatus() != ReservationDTO.Status.PENDING) {
                return new ResponseDTO(false, "대기 상태인 예약만 거부할 수 있습니다.", null);
            }

            target.setStatus(ReservationDTO.Status.REJECTED);
            target.setRejectReason(rejectReason);
            fileManager.saveAll(catalog.getAllReservations());
            NotificationHandler.getInstance().notifyRejected(target, rejectReason);

            return new ResponseDTO(true, "예약이 거부되었습니다.", null);

        } catch (Exception e) {
            return new ResponseDTO(false, "거부 실패", null);
        }
    }

    /**
     * roomName 예: "23 정보공학관 9층 911호"
     * data/masterfile/2026.json 에서 해당 강의실의 capacity 반환. 찾지 못하면 0.
     */
    private int getRoomCapacity(String roomName) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.+?)\\s+(\\d+층)\\s+(\\d+호)$")
                    .matcher(roomName);
            if (!m.matches()) return 0;
            String building = m.group(1);
            String floor    = m.group(2);
            String roomKey  = m.group(3);

            File jsonFile = new File("data/masterfile/2026.json");
            if (!jsonFile.exists()) return 0;

            JsonNode root = new ObjectMapper().readTree(jsonFile);
            for (JsonNode semNode : root) {
                JsonNode buildingNode = semNode.get(building);
                if (buildingNode == null) continue;
                JsonNode floorNode = buildingNode.get(floor);
                if (floorNode == null) continue;
                JsonNode roomNode = floorNode.get(roomKey);
                if (roomNode == null) continue;
                JsonNode infoNode = roomNode.get("info");
                if (infoNode != null && infoNode.has("capacity")) {
                    return infoNode.get("capacity").asInt(0);
                }
            }
        } catch (Exception e) {
            System.err.println("capacity 조회 실패: " + e.getMessage());
        }
        return 0;
    }

    private String getRoomStatus(String roomName) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.+?)\\s+(\\d+층)\\s+(\\d+호)$")
                    .matcher(roomName);
            if (!m.matches()) return "사용 가능";
            String building = m.group(1);
            String floor    = m.group(2);
            String roomKey  = m.group(3);

            File jsonFile = new File("data/masterfile/2026.json");
            if (!jsonFile.exists()) return "사용 가능";

            JsonNode root = new ObjectMapper().readTree(jsonFile);
            for (JsonNode semNode : root) {
                JsonNode buildingNode = semNode.get(building);
                if (buildingNode == null) continue;
                JsonNode floorNode = buildingNode.get(floor);
                if (floorNode == null) continue;
                JsonNode roomNode = floorNode.get(roomKey);
                if (roomNode == null) continue;
                JsonNode infoNode = roomNode.get("info");
                if (infoNode != null && infoNode.has("status")) {
                    return infoNode.get("status").asText("사용 가능");
                }
            }
        } catch (Exception e) {
            System.err.println("status 조회 실패: " + e.getMessage());
        }
        return "사용 가능";
    }

    private int countPeriods(String periodInfo) {
        if (periodInfo == null || periodInfo.isBlank()) return 0;
        return (int) java.util.Arrays.stream(periodInfo.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }

    private boolean isSameSlot(ReservationDTO.Response a, ReservationDTO.Response b) {
        return a.getRoomName() != null && a.getRoomName().equals(b.getRoomName())
                && a.getDate() != null && a.getDate().equals(b.getDate())
                && isPeriodConflict(a.getPeriodInfo(), b.getPeriodInfo());
    }

    private boolean isPeriodConflict(String period1, String period2) {
        if (period1 == null || period2 == null) return false;
        for (String p1 : period1.split(",")) {
            for (String p2 : period2.split(",")) {
                if (p1.trim().equals(p2.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
