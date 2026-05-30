/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.main.CRSystemClient;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 예약 컨트롤러 
 * @author wonsik
 */
public class ReservationController {
    private static ReservationController instance;
    private ReservationController() {}
    public static ReservationController getInstance() {
        if (instance == null) instance = new ReservationController();
        return instance;
    }

    // 조회 
    public void getReservationList(String status, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        // status가 null이면 전체, 특정 상태면 해당 상태만 조회하도록 서버에 요청
        RequestDTO request = new RequestDTO("GET_RESERVATION_LIST", status);
        CRSystemClient.getInstance().sendRequest(request, onSuccess, onFailure);
    }

    // 승인 
    public void approveReservations(List<String> reservationIds, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        RequestDTO request = new RequestDTO("APPROVE_RESERVATIONS", reservationIds);
        CRSystemClient.getInstance().sendRequest(request, onSuccess, onFailure);
    }

    // 거부 
    public void rejectReservation(String reservationId, String rejectReason, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        // ID와 사유를 묶어서 전송 (Map 또는 전용 DTO 활용)
        Map<String, String> payload = Map.of("id", reservationId, "reason", rejectReason);
        RequestDTO request = new RequestDTO("REJECT_RESERVATION", payload);
        CRSystemClient.getInstance().sendRequest(request, onSuccess, onFailure);
    }
}
