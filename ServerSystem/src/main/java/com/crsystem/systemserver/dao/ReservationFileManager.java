package com.crsystem.systemserver.dao;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.systemserver.controller.FileManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationFileManager implements FileManager<ReservationDTO.Response> {

    private final String FILE_PATH = ServerPaths.RESERVATION_JSON;
    private final Gson gson;

    public ReservationFileManager() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    // ====================
    // [SFR-701] 데이터 관리 - Reservation.json 파일에서 전체 예약 목록 로드
    // ====================
    @Override
    public List<ReservationDTO.Response> loadAll() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<ReservationDTO.Response>>() {}.getType();
            List<ReservationDTO.Response> reservationList = gson.fromJson(reader, listType);
            return reservationList != null ? reservationList : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ====================
    // [SFR-702] 데이터 관리 - 전체 예약 목록을 Reservation.json에 덮어쓰기
    // ====================
    @Override
    public void saveAll(List<ReservationDTO.Response> reservationList) {
        File file = new File(FILE_PATH);

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(reservationList, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================
    // [SFR-703] 데이터 관리 - 신규 예약 추가 후 파일에 반영
    // ====================
    @Override
    public void add(ReservationDTO.Response reservation) {
        List<ReservationDTO.Response> reservationList = loadAll();
        reservationList.add(reservation);
        saveAll(reservationList);
    }

    // ====================
    // [SFR-704] 데이터 관리 - 예약 ID에 해당하는 예약을 파일에서 제거
    // ====================
    @Override
    public void delete(String id) {
        List<ReservationDTO.Response> reservationList = loadAll();
        reservationList.removeIf(r -> id.equals(r.getReservationId()));
        saveAll(reservationList);
    }
}
