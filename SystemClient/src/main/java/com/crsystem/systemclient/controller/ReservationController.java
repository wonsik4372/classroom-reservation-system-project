/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemclient.main.CRSystemClient;
import java.time.LocalDate;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 예약 컨트롤러 
 * @author wonsik
 */
public class ReservationController {
    private static ReservationController instance;
    
    private static List<ReservationDTO.Response> mockDB = new ArrayList<>();
    
    private ReservationController() {
        if (mockDB.isEmpty()) {
            ReservationDTO.Response r1 = new ReservationDTO.Response();
            r1.setReservationId("RES-001");
            r1.setRoleType(Role.STUDENT);
            r1.setUserId("20201111");
            r1.setUserName("김학생");
            r1.setRoomName("공학관 101호");
            r1.setPurpose("알고리즘 스터디");
            r1.setDate(LocalDate.now());
            r1.setPeriodInfo("3교시");
            r1.setPartnerCount(4);
            r1.setStatus(ReservationDTO.Status.PENDING);
            mockDB.add(r1);

            ReservationDTO.Response r2 = new ReservationDTO.Response();
            r2.setReservationId("RES-002");
            r2.setRoleType(Role.PROFESSOR);
            r2.setUserId("19902");
            r2.setUserName("박교수");
            r2.setRoomName("이학관 202호");
            r2.setPurpose("보강");
            r2.setDate(LocalDate.now().plusDays(1));
            r2.setPeriodInfo("7교시");
            r2.setPartnerCount(30);
            r2.setStatus(ReservationDTO.Status.APPROVED);
            mockDB.add(r2);
        }
    }
    
    public static ReservationController getInstance() {
        if (instance == null) instance = new ReservationController();
        return instance;
    }

    // 조회 
    public void getReservationList(String status, java.util.function.Consumer<ResponseDTO> onSuccess, java.util.function.Consumer<String> onFailure) {
        
        List<ReservationDTO.Response> result;
        
        if ("PENDING".equals(status)) {
            // 상태가 PENDING인 것만 필터링해서 리턴
            result = mockDB.stream()
                           .filter(r -> r.getStatus() == ReservationDTO.Status.PENDING)
                           .collect(Collectors.toList());
        } else {
            // ALL이면 전부 다 리턴
            result = new ArrayList<>(mockDB);
        }
        
        // 서버에서 성공 응답이 온 것처럼 포장해서 콜백 실행
        onSuccess.accept(new ResponseDTO(true, "가짜 데이터 조회 성공", result));
    }

    // 승인 (로컬 mockDB 업데이트 + 서버에 알림 트리거 요청)
    public void approveReservations(List<String> reservationIds, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        for (ReservationDTO.Response r : mockDB) {
            if (reservationIds.contains(r.getReservationId())) {
                r.setStatus(ReservationDTO.Status.APPROVED);
                // 서버에 승인 통보 → 서버가 해당 학생 알림 생성
                sendApproveToServer(r.getReservationId(), r.getUserId());
            }
        }
        onSuccess.accept(new ResponseDTO(true, "승인 처리 완료", null));
    }

    // 거부 (로컬 mockDB 업데이트 + 서버에 알림 트리거 요청)
    public void rejectReservation(String reservationId, String rejectReason, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        for (ReservationDTO.Response r : mockDB) {
            if (r.getReservationId().equals(reservationId)) {
                r.setStatus(ReservationDTO.Status.REJECTED);
                r.setRejectReason(rejectReason);
                // 서버에 거부 통보 → 서버가 해당 학생에게 거부 사유 포함 알림 생성
                sendRejectToServer(r.getReservationId(), r.getUserId(), rejectReason);
            }
        }
        onSuccess.accept(new ResponseDTO(true, "거부 처리 완료", null));
    }

    // ==========================================
    // 서버 알림 트리거: 승인
    // ==========================================
    private void sendApproveToServer(String reservationId, String userId) {
        ReservationDTO.Request payload = new ReservationDTO.Request();
        payload.setUserId(reservationId); // userId 필드 재사용: reservationId 전달
        CRSystemClient.getInstance().sendRequest(
            new RequestDTO("APPROVE_RESERVATION", payload),
            res -> System.out.println("[ReservationController] 서버 승인 알림 전송 완료: " + reservationId),
            err -> System.err.println("[ReservationController] 서버 승인 알림 전송 실패: " + err)
        );
    }

    // ==========================================
    // 서버 알림 트리거: 거부
    // ==========================================
    private void sendRejectToServer(String reservationId, String userId, String rejectReason) {
        ReservationDTO.Request payload = new ReservationDTO.Request();
        payload.setUserId(reservationId);   // reservationId 전달
        payload.setPurpose(rejectReason);   // purpose 필드 재사용: 거부 사유 전달
        CRSystemClient.getInstance().sendRequest(
            new RequestDTO("REJECT_RESERVATION", payload),
            res -> System.out.println("[ReservationController] 서버 거부 알림 전송 완료: " + reservationId),
            err -> System.err.println("[ReservationController] 서버 거부 알림 전송 실패: " + err)
        );
    }
}


