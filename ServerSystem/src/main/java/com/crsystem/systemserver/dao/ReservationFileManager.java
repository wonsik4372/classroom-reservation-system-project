package com.crsystem.systemserver.dao;

import com.crsystem.common.dto.ReservationDTO;
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

public class ReservationFileManager {

    private static final String FILE_PATH = ServerPaths.RESERVATION_JSON;
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

    public List<ReservationDTO.Response> loadAll() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<ReservationDTO.Response>>() {}.getType();
            List<ReservationDTO.Response> reservations = gson.fromJson(reader, listType);
            return reservations != null ? reservations : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveAll(List<ReservationDTO.Response> reservations) {
        File file = new File(FILE_PATH);

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(reservations, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
