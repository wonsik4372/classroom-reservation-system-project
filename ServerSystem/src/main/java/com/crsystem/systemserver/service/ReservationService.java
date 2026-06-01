/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;

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

    // 서버 인메모리 예약 저장소 (멀티스레드 안전)
    private final List<ReservationDTO.Response> reservationDB = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ReservationService() {}

    public static ReservationService getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // [SFR-401] 학생은 강의실 예약을 신청할 수 있어야 한다
    // ====================
    public ResponseDTO studentReservationRequest(ReservationDTO.Request req) {
        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setReservationId("RES-" + System.currentTimeMillis());
        reservation.setUserId(req.getUserId());
        reservation.setRoomName(req.getRoomName());
        reservation.setDate(req.getDate());
        reservation.setPeriodInfo(req.getPeriodInfo());
        reservation.setPurpose(req.getPurpose());
        reservation.setPartnerCount(req.getPartnerCount());
        reservation.setStatus(ReservationDTO.Status.PENDING);

        reservationDB.add(reservation);
        System.out.println("[ReservationService] 예약 요청 접수: " + reservation.getReservationId());

        return new ResponseDTO(true, "예약 요청이 접수되었습니다.", reservation.getReservationId());
    }

    // ====================
    // [SFR-501] 조교는 예약 신청 대기 목록을 조회할 수 있어야 한다
    // ====================
    public ResponseDTO getPendingReservations() {
        List<ReservationDTO.Response> pending = reservationDB.stream()
                .filter(r -> r.getStatus() == ReservationDTO.Status.PENDING)
                .collect(Collectors.toList());
        return new ResponseDTO(true, "대기 예약 조회 성공", pending);
    }

    // 전체 예약 목록 조회
    public ResponseDTO getAllReservations() {
        return new ResponseDTO(true, "전체 예약 조회 성공", new ArrayList<>(reservationDB));
    }

    // ====================
    // [SFR-502] 조교는 예약을 승인할 수 있어야 한다
    // [SFR-408] 승인 시 로그인된 학생에게 push, 미로그인 학생은 로그인 시 즉각 알림
    // ====================
    public ResponseDTO approveReservation(ReservationDTO.Request req) {
        String reservationId = req.getUserId(); // 재사용: userId 필드에 reservationId 전달

        ReservationDTO.Response target = findById(reservationId);
        if (target == null) {
            return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
        }

        if (target.getStatus() != ReservationDTO.Status.PENDING) {
            return new ResponseDTO(false, "대기 상태인 예약만 승인할 수 있습니다.", null);
        }

        target.setStatus(ReservationDTO.Status.APPROVED);
        System.out.println("[ReservationService] 예약 승인: " + reservationId);

        // 학생에게 승인 알림 생성
        NotificationService.getInstance().notifyApproved(target);

        return new ResponseDTO(true, "예약이 승인되었습니다.", null);
    }

    // ====================
    // [SFR-503] 조교는 거부 사유와 함께 예약을 거부할 수 있어야 한다
    // [SFR-409] 거부 시 거부 사유를 포함해 로그인된 학생에게 push, 미로그인 학생은 로그인 시 즉각 알림
    // ====================
    public ResponseDTO rejectReservation(ReservationDTO.Request req) {
        String reservationId = req.getUserId(); // 재사용: userId 필드에 reservationId 전달
        String rejectReason = req.getPurpose(); // 재사용: purpose 필드에 거부 사유 전달

        ReservationDTO.Response target = findById(reservationId);
        if (target == null) {
            return new ResponseDTO(false, "해당 예약을 찾을 수 없습니다: " + reservationId, null);
        }

        if (target.getStatus() != ReservationDTO.Status.PENDING) {
            return new ResponseDTO(false, "대기 상태인 예약만 거부할 수 있습니다.", null);
        }

        target.setStatus(ReservationDTO.Status.REJECTED);
        target.setRejectReason(rejectReason);
        System.out.println("[ReservationService] 예약 거부: " + reservationId + ", 사유: " + rejectReason);

        // 학생에게 거부 알림 생성 (거부 사유 포함)
        NotificationService.getInstance().notifyRejected(target, rejectReason);

        return new ResponseDTO(true, "예약이 거부되었습니다.", null);
    }

    private ReservationDTO.Response findById(String reservationId) {
        for (ReservationDTO.Response r : reservationDB) {
            if (r.getReservationId().equals(reservationId)) {
                return r;
            }
        }
        return null;
    }
}
