/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.dao;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.systemserver.controller.FileManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReservationFileManager
        implements FileManager<ReservationDTO.Response> {

    private final String FILE_PATH
            = "src/main/resources/masterfile/Reservation.json";

    private final Gson gson;

    public ReservationFileManager() {
        gson = new GsonBuilder()
                .registerTypeAdapter(
                        java.time.LocalDate.class,
                        (com.google.gson.JsonSerializer<java.time.LocalDate>) (src, typeOfSrc, context)
                        -> new com.google.gson.JsonPrimitive(
                                src.toString()
                        )
                )
                .registerTypeAdapter(
                        java.time.LocalDate.class,
                        (com.google.gson.JsonDeserializer<java.time.LocalDate>) (json, typeOfT, context)
                        -> java.time.LocalDate.parse(
                                json.getAsString()
                        )
                )
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

            Type listType
                    = new TypeToken<
                            ArrayList<ReservationDTO.Response>>() {
                    }.getType();

            List<ReservationDTO.Response> list
                    = gson.fromJson(reader, listType);

            return list != null
                    ? list
                    : new ArrayList<>();

        } catch (Exception e) {

            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void saveAll(
            List<ReservationDTO.Response> list) {

        File file = new File(FILE_PATH);

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Writer writer
                = new FileWriter(file)) {

            gson.toJson(list, writer);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void add(
            ReservationDTO.Response reservation) {

        List<ReservationDTO.Response> list
                = loadAll();

        list.add(reservation);

        saveAll(list);
    }

    @Override
    public void delete(String id) {

        List<ReservationDTO.Response> list
                = loadAll();

        list.removeIf(
                r -> id.equals(
                        r.getReservationId()
                )
        );

        saveAll(list);
    }
}
