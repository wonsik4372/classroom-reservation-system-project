/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.model;

import com.crsystem.common.dto.ReservationDTO;

import java.util.ArrayList;
import java.util.List;

public class ReservationCatalog {

    private final List<ReservationDTO.Response> reservationList;

    public ReservationCatalog(
            List<ReservationDTO.Response> list) {

        this.reservationList
                = list != null
                        ? list
                        : new ArrayList<>();
    }

    public List<ReservationDTO.Response>
            getAllReservations() {

        return reservationList;
    }

    public void addReservation(
            ReservationDTO.Response reservation) {

        reservationList.add(reservation);
    }

    public ReservationDTO.Response
            findReservation(String reservationId) {

        for (ReservationDTO.Response r
                : reservationList) {

            if (reservationId.equals(
                    r.getReservationId())) {

                return r;
            }
        }

        return null;
    }
}
