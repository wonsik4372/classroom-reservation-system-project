package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.model.ReservationCatalog;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 관리 전문가
 * @author wonsik
 */
public class ReservationService {

    private static class Holder {
        private static final ReservationService INSTANCE = new ReservationService();
    }

    private final ReservationCatalog catalog;
    private final ReservationFileManager fileManager;

    private ReservationService() {
        this.fileManager = new ReservationFileManager();
        this.catalog = new ReservationCatalog(fileManager.loadAll());
    }

    public static ReservationService getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // 예약 등록
    // ====================
    public ResponseDTO addReservation(ReservationDTO.Response reservation) {
        try {
            // 하루 예약 교시 수 제한 검사
            int newPeriodCount = countPeriods(reservation.getPeriodInfo());
            int existingPeriodCount = catalog.getAllReservations().stream()
                    .filter(r -> r.getUserId().equals(reservation.getUserId())
                            && r.getDate().equals(reservation.getDate())
                            && r.getStatus() != ReservationDTO.Status.REJECTED)
                    .mapToInt(r -> countPeriods(r.getPeriodInfo()))
                    .sum();

            int maxPeriods = reservation.getRoleType() == Role.STUDENT ? 2 : 3;
            if (existingPeriodCount + newPeriodCount > maxPeriods) {
                String roleLabel = reservation.getRoleType() == Role.STUDENT ? "학생" : "교수";
                return new ResponseDTO(false,
                        "예약 실패: " + roleLabel + "은 하루 최대 " + maxPeriods + "교시까지만 예약 가능합니다.", null);
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
    // 전체 예약 조회
    // ====================
    public ResponseDTO getReservationList() {
        List<ReservationDTO.Response> reservationList = fileManager.loadAll();
        return new ResponseDTO(true, "전체 예약 조회 성공", reservationList);
    }

    // ====================
    // 대기 예약 조회
    // ====================
    public ResponseDTO getPendingReservationList() {
        List<ReservationDTO.Response> pendingList = catalog.getAllReservations().stream()
                .filter(r -> r.getStatus() == ReservationDTO.Status.PENDING)
                .collect(Collectors.toList());
        return new ResponseDTO(true, "대기 예약 조회 성공", pendingList);
    }

    // ====================
    // 예약 승인
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
            NotificationService.getInstance().notifyApproved(target);

            return new ResponseDTO(true, "예약이 승인되었습니다.", null);

        } catch (Exception e) {
            return new ResponseDTO(false, "승인 실패", null);
        }
    }

    // ====================
    // 예약 취소 (본인만)
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
    // 예약 거부
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
            NotificationService.getInstance().notifyRejected(target, rejectReason);

            return new ResponseDTO(true, "예약이 거부되었습니다.", null);

        } catch (Exception e) {
            return new ResponseDTO(false, "거부 실패", null);
        }
    }

    private int countPeriods(String periodInfo) {
        if (periodInfo == null || periodInfo.isBlank()) return 0;
        return (int) java.util.Arrays.stream(periodInfo.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }

    private boolean isSameSlot(ReservationDTO.Response a, ReservationDTO.Response b) {
        return a.getRoomName().equals(b.getRoomName())
                && a.getDate().equals(b.getDate())
                && isPeriodConflict(a.getPeriodInfo(), b.getPeriodInfo());
    }

    private boolean isPeriodConflict(String period1, String period2) {
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
