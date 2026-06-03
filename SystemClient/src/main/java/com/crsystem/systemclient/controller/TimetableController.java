package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.main.CRSystemClient;

import java.util.function.Consumer;

public class TimetableController {

    private static class Holder {
        private static final TimetableController INSTANCE = new TimetableController();
    }

    private TimetableController() {}

    public static TimetableController getInstance() {
        return Holder.INSTANCE;
    }

    public void getTimetable(String year,
                             Consumer<ResponseDTO> onSuccess,
                             Consumer<String> onFailure) {
        CRSystemClient.getInstance().sendRequest(
                new RequestDTO("GET_TIMETABLE", year),
                onSuccess,
                onFailure
        );
    }
}
