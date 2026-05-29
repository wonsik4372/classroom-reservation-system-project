/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.dao;

import com.crsystem.systemserver.controller.FileManager;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;

// import java.nio.file.Files;
/**
 *
 * @author wonsik
 */
public class UserFileManager implements FileManager <User>{
    private final String FILE_PATH = "src/main/resources/masterfile/User.json";
    private final Gson gson;
    
    public UserFileManager() {
        // 사람이 읽기 편하게 엔터와 들여쓰기를 자동 적용해주는 옵션(PrettyPrinting)
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    // ==========================================
    // 1. 전부 불러오기 (JSON -> List<User>)
    // ==========================================
    @Override
    public List<User> loadAll() {
        File file = new File(FILE_PATH);
        
        // 파일이 존재하지 않으면 빈 리스트 반환 (최초 실행 시 에러 방지)
        if (!file.exists()) {
            return new ArrayList<>(); 
        }

        try (Reader reader = new FileReader(file)) {
            // Gson에게 "이 JSON은 List<User> 형태야"라고 알려주는 타입 토큰
            Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
            List<User> users = gson.fromJson(reader, userListType);
            
            return users != null ? users : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("User.json 파일을 읽는 중 오류가 발생했습니다.");
            return new ArrayList<>();
        }
    }
    
    
    // 전체 데이터 덮어쓰기 (저장)
    @Override
    public void saveAll(List<User> list) {
        File file = new File(FILE_PATH);
        
        // 부모 폴더(masterfile)가 없으면 자동으로 생성
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("User.json 파일에 저장하는 중 오류가 발생했습니다.");
        }
    }
    
    // 단일 객체 추가 (또는 수정)
    @Override
    public void add(User user){
        List<User> currentList = loadAll(); 
        currentList.add(user);
        saveAll(currentList);
    }
    
    // 고유 ID로 삭제
    @Override
    public void delete(String id){
        List<User> currentList = loadAll();
        currentList.removeIf(user -> user.getId().equals(id));
        saveAll(currentList);
    }
    
}
