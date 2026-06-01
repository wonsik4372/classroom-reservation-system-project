package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.main.CRSystemClient;
import java.util.List;
import java.util.function.Consumer;

/**
 * 예약 컨트롤러
 * @author wonsik
 */
public class ReservationController {

    private static class Holder {
        private static final ReservationController INSTANCE = new ReservationController();
    }

    private List<ReservationDTO.Response> reservationCache = new java.util.ArrayList<>();

    private ReservationController() {}

    public static ReservationController getInstance() {
        return Holder.INSTANCE;
    }

    public List<ReservationDTO.Response> getReservationCache() {
        return reservationCache;
    }

    public void updateCache(Object payload) {
        if (payload instanceof List<?>) {
            reservationCache = (List<ReservationDTO.Response>) payload;
        }
    }

    public void createReservation(ReservationDTO.Request request,
                                  Consumer<ResponseDTO> onSuccess,
                                  Consumer<String> onFailure) {
        CRSystemClient.getInstance().sendRequest(
                new RequestDTO("CREATE_RESERVATION", request),
                onSuccess,
                onFailure
        );
    }

    public void getReservationList(String status,
                                   Consumer<ResponseDTO> onSuccess,
                                   Consumer<String> onFailure) {
        String command = "PENDING".equals(status) ? "GET_PENDING_RESERVATIONS" : "GET_ALL_RESERVATIONS";
        CRSystemClient.getInstance().sendRequest(
                new RequestDTO(command, null),
                onSuccess,
                onFailure
        );
    }

    public void approveReservations(List<String> reservationIds,
                                    Consumer<ResponseDTO> onSuccess,
                                    Consumer<String> onFailure) {
        if (reservationIds == null || reservationIds.isEmpty()) {
            onFailure.accept("승인할 예약을 선택해주세요.");
            return;
        }

        approveNext(reservationIds, 0, onSuccess, onFailure);
    }

    public void rejectReservation(String reservationId,
                                  String rejectReason,
                                  Consumer<ResponseDTO> onSuccess,
                                  Consumer<String> onFailure) {
        ReservationDTO.Request payload = new ReservationDTO.Request();
        payload.setReservationId(reservationId);
        payload.setPurpose(rejectReason);

        CRSystemClient.getInstance().sendRequest(
                new RequestDTO("REJECT_RESERVATION", payload),
                onSuccess,
                onFailure
        );
    }

    private void approveNext(List<String> reservationIds,
                             int index,
                             Consumer<ResponseDTO> onSuccess,
                             Consumer<String> onFailure) {
        if (index >= reservationIds.size()) {
            onSuccess.accept(new ResponseDTO(true, "승인 처리 완료", null));
            return;
        }

        ReservationDTO.Request payload = new ReservationDTO.Request();
        payload.setReservationId(reservationIds.get(index));

        CRSystemClient.getInstance().sendRequest(
                new RequestDTO("APPROVE_RESERVATION", payload),
                response -> {
                    if (!response.isSuccess()) {
                        onFailure.accept(response.getMessage());
                        return;
                    }
                    approveNext(reservationIds, index + 1, onSuccess, onFailure);
                },
                onFailure
        );
    }
}
