package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.model.User;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 예약 관리 전문가
 * @author wonsik
 */
public class ReservationService {

    private static class Holder {
        private static final ReservationService INSTANCE = new ReservationService();
    }

    private final ReservationFileManager fileManager;
    private final List<ReservationDTO.Response> reservationDB;

    private ReservationService() {
        this.fileManager = new ReservationFileManager();
        this.reservationDB = new CopyOnWriteArrayList<>(fileManager.loadAll());
    }

    public static ReservationService getInstance() {
        return Holder.INSTANCE;
    }

    public ResponseDTO createReservation(ReservationDTO.Request req) {
        User user = UserService.getInstance().getUserById(req.getUserId());

        if (user == null) {
            return new ResponseDTO(false, "예약 실패: 존재하지 않는 사용자입니다.", null);
        }

        int periodCount = countPeriods(req.getPeriodInfo());
        if (user.getRole() == Role.STUDENT && periodCount > 2) {
            return new ResponseDTO(false, "예약 실패: 학생은 최대 2교시까지만 예약 가능합니다.", null);
        }
        if (user.getRole() == Role.PROFESSOR && periodCount > 3) {
            return new ResponseDTO(false, "예약 실패: 교수는 최대 3교시까지만 예약 가능합니다.", null);
        }

        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setReservationId("RES-" + System.currentTimeMillis());
        reservation.setUserId(user.getId());
        reservation.setUserName(user.getName());
        reservation.setRoleType(user.getRole());
        reservation.setRoomName(req.getRoomName());
        reservation.setDate(req.getDate());
        reservation.setDay(toKoreanDay(req.getDate().getDayOfWeek()));
        reservation.setPeriodInfo(req.getPeriodInfo());
        reservation.setPurpose(req.getPurpose());
        reservation.setPartnerCount(req.getPartnerCount());

        synchronized (reservationDB) {
            for (ReservationDTO.Response existing : reservationDB) {
                if (!isSameSlot(existing, reservation)) {
                    continue;
                }

                if (user.getRole() == Role.STUDENT) {
                    return new ResponseDTO(false, "예약 실패: 이미 예약된 시간입니다.", null);
                }

                if (user.getRole() == Role.PROFESSOR) {
                    if (existing.getRoleType() == Role.PROFESSOR) {
                        return new ResponseDTO(false, "예약 실패: 이미 교수 예약이 존재합니다.", null);
                    }

                    if (existing.getRoleType() == Role.STUDENT) {
                        existing.setStatus(ReservationDTO.Status.REJECTED);
                        existing.setRejectReason("교수 보강");
                    }
                }
            }

            reservation.setStatus(user.getRole() == Role.PROFESSOR
                    ? ReservationDTO.Status.APPROVED
                    : ReservationDTO.Status.PENDING);

            reservationDB.add(reservation);
            saveReservations();
        }

        return new ResponseDTO(true, "예약이 등록되었습니다.", reservation);
    }

    public ResponseDTO getPendingReservations() {
        List<ReservationDTO.Response> pending = reservationDB.stream()
                .filter(r -> r.getStatus() == ReservationDTO.Status.PENDING)
                .collect(Collectors.toList());
        return new ResponseDTO(true, "대기 예약 조회 성공", pending);
    }

    public ResponseDTO getAllReservations() {
        return new ResponseDTO(true, "전체 예약 조회 성공", new ArrayList<>(reservationDB));
    }

    public ResponseDTO approveReservation(ReservationDTO.Request req) {
        String reservationId = req.getReservationId();

        synchronized (reservationDB) {
            ReservationDTO.Response target = findById(reservationId);
            if (target == null) {
                return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
            }

            if (target.getStatus() != ReservationDTO.Status.PENDING) {
                return new ResponseDTO(false, "대기 상태인 예약만 승인할 수 있습니다.", null);
            }

            target.setStatus(ReservationDTO.Status.APPROVED);
            saveReservations();
            NotificationService.getInstance().notifyApproved(target);
        }

        return new ResponseDTO(true, "예약이 승인되었습니다.", null);
    }

    public ResponseDTO rejectReservation(ReservationDTO.Request req) {
        String reservationId = req.getReservationId();
        String rejectReason = req.getPurpose();

        synchronized (reservationDB) {
            ReservationDTO.Response target = findById(reservationId);
            if (target == null) {
                return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
            }

            if (target.getStatus() != ReservationDTO.Status.PENDING) {
                return new ResponseDTO(false, "대기 상태인 예약만 거부할 수 있습니다.", null);
            }

            target.setStatus(ReservationDTO.Status.REJECTED);
            target.setRejectReason(rejectReason);
            saveReservations();
            NotificationService.getInstance().notifyRejected(target, rejectReason);
        }

        return new ResponseDTO(true, "예약이 거부되었습니다.", null);
    }

    private void saveReservations() {
        fileManager.saveAll(new ArrayList<>(reservationDB));
    }

    private ReservationDTO.Response findById(String reservationId) {
        for (ReservationDTO.Response reservation : reservationDB) {
            if (reservation.getReservationId() != null && reservation.getReservationId().equals(reservationId)) {
                return reservation;
            }
        }
        return null;
    }

    private boolean isSameSlot(ReservationDTO.Response first, ReservationDTO.Response second) {
        return first.getRoomName().equals(second.getRoomName())
                && first.getDate().equals(second.getDate())
                && isPeriodConflict(first.getPeriodInfo(), second.getPeriodInfo());
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

    private int countPeriods(String periodInfo) {
        if (periodInfo == null || periodInfo.isBlank()) {
            return 0;
        }
        return periodInfo.split(",").length;
    }

    private String toKoreanDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
