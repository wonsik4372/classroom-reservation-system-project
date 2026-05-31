package com.crsystem.systemclient.view.Reservation;

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
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationFileManager {

    private static final String FILE_NAME = "reservation.json";

    private static Gson createGson() {

        return new GsonBuilder()
                .registerTypeAdapter(
                        LocalDate.class,
                        (JsonSerializer<LocalDate>)
                                (src, typeOfSrc, context)
                                -> new JsonPrimitive(src.toString())
                )
                .registerTypeAdapter(
                        LocalDate.class,
                        (JsonDeserializer<LocalDate>)
                                (json, typeOfT, context)
                                -> LocalDate.parse(json.getAsString())
                )
                .setPrettyPrinting()
                .create();
    }

    public static void saveReservations() {

        try {

            Gson gson = createGson();

            FileWriter writer = new FileWriter(FILE_NAME);

            gson.toJson(
                    CRSystemReservation.reservationList,
                    writer
            );

            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<ReservationDTO.Response> loadReservations() {

        try {

            File file = new File(FILE_NAME);

            if (!file.exists()) {
                return new ArrayList<>();
            }

            Gson gson = createGson();

            FileReader reader = new FileReader(FILE_NAME);

            Type listType =
                    new TypeToken<List<ReservationDTO.Response>>() {
                    }.getType();

            List<ReservationDTO.Response> list =
                    gson.fromJson(reader, listType);

            reader.close();

            return list == null
                    ? new ArrayList<>()
                    : list;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}