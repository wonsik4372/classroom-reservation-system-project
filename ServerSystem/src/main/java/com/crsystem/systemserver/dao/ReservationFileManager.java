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

    @Override
    public void add(ReservationDTO.Response reservation) {
        List<ReservationDTO.Response> reservationList = loadAll();
        reservationList.add(reservation);
        saveAll(reservationList);
    }

    @Override
    public void delete(String id) {
        List<ReservationDTO.Response> reservationList = loadAll();
        reservationList.removeIf(r -> id.equals(r.getReservationId()));
        saveAll(reservationList);
    }
}
