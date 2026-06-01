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
 *
 * @author wonsik
 */
public class ReservationController {

    private static ReservationController instance;

    private ReservationController() {
    }

    public static ReservationController getInstance() {
        if (instance == null) {
            instance = new ReservationController();
        }
        return instance;
    }

    // 예약 목록 조회
    public void getReservationList(
            String status,
            Consumer<ResponseDTO> onSuccess,
            Consumer<String> onFailure) {

        CRSystemClient.getInstance().sendRequest(
                new RequestDTO(
                        "GET_RESERVATION_LIST",
                        null
                ),
                onSuccess,
                onFailure
        );
    }

    // 예약 등록
    public void addReservation(
            ReservationDTO.Response reservation,
            Consumer<ResponseDTO> onSuccess,
            Consumer<String> onFailure) {

        CRSystemClient.getInstance().sendRequest(
                new RequestDTO(
                        "ADD_RESERVATION",
                        reservation
                ),
                onSuccess,
                onFailure
        );
    }

    // 예약 승인
    public void approveReservations(
            List<String> reservationIds,
            Consumer<ResponseDTO> onSuccess,
            Consumer<String> onFailure) {

        for (String id : reservationIds) {

            ReservationDTO.Request payload
                    = new ReservationDTO.Request();

            payload.setReservationId(id);

            CRSystemClient.getInstance().sendRequest(
                    new RequestDTO(
                            "APPROVE_RESERVATION",
                            payload
                    ),
                    onSuccess,
                    onFailure
            );
        }
    }

    // 예약 거절
    public void rejectReservation(
            String reservationId,
            String rejectReason,
            Consumer<ResponseDTO> onSuccess,
            Consumer<String> onFailure) {

        ReservationDTO.Request payload
                = new ReservationDTO.Request();

        payload.setReservationId(reservationId);
        payload.setRejectReason(rejectReason);

        CRSystemClient.getInstance().sendRequest(
                new RequestDTO(
                        "REJECT_RESERVATION",
                        payload
                ),
                onSuccess,
                onFailure
        );
    }
}
