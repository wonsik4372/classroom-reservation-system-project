package com.crsystem.systemserver.model;

import com.crsystem.common.dto.ReservationDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 예약 목록
 *
 * ClientHandler 스레드들이 동시에 조회/추가할 수 있으므로
 * CopyOnWriteArrayList로 Thread Safety를 보장한다.
 *
 * @author wonsik
 */
public class ReservationCatalog {

    private final CopyOnWriteArrayList<ReservationDTO.Response> reservationList;

    public ReservationCatalog(List<ReservationDTO.Response> list) {
        this.reservationList = new CopyOnWriteArrayList<>(
                list != null ? list : new ArrayList<>()
        );
    }

    public List<ReservationDTO.Response> getAllReservations() {
        return new ArrayList<>(reservationList);
    }

    public void addReservation(ReservationDTO.Response reservation) {
        reservationList.add(reservation);
    }

    public ReservationDTO.Response findReservation(String reservationId) {
        for (ReservationDTO.Response r : reservationList) {
            if (reservationId.equals(r.getReservationId())) {
                return r;
            }
        }
        return null;
    }

    public void removeReservation(String reservationId) {
        reservationList.removeIf(r -> reservationId.equals(r.getReservationId()));
    }
}
