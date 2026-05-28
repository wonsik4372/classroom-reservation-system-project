/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.dao;

import com.crsystem.systemserver.controller.FileManager;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;

import java.util.List;
import java.util.ArrayList;
// import java.nio.file.Files;
/**
 *
 * @author wonsik
 */
public class UserFileManager implements FileManager <User>{
    private final String FILE_PATH = "src/main/resources/masterfile/User.json";
    
    // 전부 불러오기
    @Override
    public List<User> loadAll() {
        // 파일 읽어서 List<User> 반환
        return new ArrayList<>(); 
    }
    
    
    // 전체 데이터 덮어쓰기 (저장)
    @Override
    public void saveAll(List<User> list) {
        
    }
    
    // 단일 객체 추가 (또는 수정)
    @Override
    public void add(User user){
        List<User> currentList = loadAll(); 
        // 사용자 추가 
        currentList.add(user);
        // 덮어쓰기
        saveAll(currentList);
    }
    
    // 고유 ID로 삭제
    @Override
    public void delete(String id){
        List<User> currentList = loadAll();
        // 사용자 삭제 
        currentList.removeIf(user -> user.getId().equals(id));
        // 덮어쓰기 
        saveAll(currentList);
    }
    
}
