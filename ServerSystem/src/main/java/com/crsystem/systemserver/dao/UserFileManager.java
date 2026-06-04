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
    private final String FILE_PATH = ServerPaths.USER_JSON;
    private final Gson gson;
    
    public UserFileManager() {
        // 사람이 읽기 편하게 엔터와 들여쓰기를 자동 적용해주는 옵션(PrettyPrinting)
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    // ====================
    // [SFR-701] 데이터 관리 - User.json 파일에서 사용자 목록 로드
    // [TC-42] loadAll - JSON 파일로부터 사용자 목록 정상 반환 / 파일 없으면 빈 목록 반환
    // ====================
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
    
    
    // ====================
    // [SFR-702] 데이터 관리 - 전체 사용자 목록을 User.json에 덮어쓰기
    // [TC-43] saveAll - 지정한 목록으로 JSON 파일 덮어쓰기
    // ====================
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
    
    // ====================
    // [SFR-703] 데이터 관리 - 신규 사용자를 목록에 추가 후 파일에 반영
    // [TC-44] add - 신규 사용자 추가 후 JSON 파일에 저장
    // ====================
    @Override
    public void add(User user){
        List<User> currentList = loadAll(); 
        currentList.add(user);
        saveAll(currentList);
    }
    
    // ====================
    // [SFR-704] 데이터 관리 - ID에 해당하는 사용자를 파일에서 제거
    // [SFR-705] 데이터 관리 - 존재하지 않는 ID 삭제 시 목록 유지
    // [TC-45] delete - ID 일치 사용자 제거 후 파일 저장
    // [TC-46] delete - 존재하지 않는 ID 삭제 시 목록 변화 없음
    // ====================
    @Override
    public void delete(String id){
        List<User> currentList = loadAll();
        currentList.removeIf(user -> user.getId().equals(id));
        saveAll(currentList);
    }
    
}
