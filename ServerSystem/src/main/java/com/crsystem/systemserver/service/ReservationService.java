/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;

import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.model.ReservationCatalog;

import java.util.List;

public class ReservationService {

    private static ReservationService instance;

    private final ReservationCatalog catalog;
    private final ReservationFileManager fileManager;

    private ReservationService() {

        fileManager = new ReservationFileManager();

        catalog = new ReservationCatalog(
                fileManager.loadAll()
        );
    }

    public static ReservationService getInstance() {

        if (instance == null) {
            instance = new ReservationService();
        }

        return instance;
    }

    // 예약 등록
    public ResponseDTO addReservation(
            ReservationDTO.Response reservation) {
        try {

            for (ReservationDTO.Response oldReservation
                    : catalog.getAllReservations()) {

                boolean sameRoom
                        = oldReservation.getRoomName()
                                .equals(reservation.getRoomName());

                boolean sameDay
                        = oldReservation.getDay()
                                .equals(reservation.getDay());

                boolean samePeriod
                        = oldReservation.getPeriodInfo()
                                .equals(reservation.getPeriodInfo());

                if (sameRoom
                        && sameDay
                        && samePeriod
                        && oldReservation.getRoleType() == Role.STUDENT
                        && reservation.getRoleType() == Role.PROFESSOR) {

                    oldReservation.setStatus(
                            ReservationDTO.Status.REJECTED
                    );

                    oldReservation.setRejectReason(
                            "교수 보강"
                    );
                }
            }

            catalog.addReservation(reservation);

            fileManager.saveAll(
                    catalog.getAllReservations()
            );

            return new ResponseDTO(
                    true,
                    "예약 등록 성공",
                    null
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new ResponseDTO(
                    false,
                    "예약 등록 실패",
                    null
            );
        }
    }

    // 전체 조회
    public ResponseDTO getReservationList() {

        /*
        List<ReservationDTO.Response> list
                = catalog.getAllReservations();
         */
        List<ReservationDTO.Response> list
                = fileManager.loadAll();

        System.out.println("조회 개수 = " + list.size());

        return new ResponseDTO(
                true,
                "예약 조회 성공",
                list
        );
    }

    public ResponseDTO approveReservation(
            String reservationId) {
        try {

            for (ReservationDTO.Response r
                    : catalog.getAllReservations()) {
                if (reservationId.equals(
                        r.getReservationId())) {
                    r.setStatus(
                            ReservationDTO.Status.APPROVED
                    );
                }
            }

            fileManager.saveAll(
                    catalog.getAllReservations()
            );

            return new ResponseDTO(
                    true,
                    "승인 완료",
                    null
            );

        } catch (Exception e) {

            return new ResponseDTO(
                    false,
                    "승인 실패",
                    null
            );
        }
    }

    public ResponseDTO rejectReservation(
            String reservationId,
            String reason) {
        try {

            for (ReservationDTO.Response r
                    : catalog.getAllReservations()) {
                if (reservationId.equals(
                        r.getReservationId())) {
                    r.setStatus(
                            ReservationDTO.Status.REJECTED
                    );

                    r.setRejectReason(reason);
                }
            }

            fileManager.saveAll(
                    catalog.getAllReservations()
            );

            return new ResponseDTO(
                    true,
                    "거절 완료",
                    null
            );

        } catch (Exception e) {

            return new ResponseDTO(
                    false,
                    "거절 실패",
                    null
            );
        }
    }
}
