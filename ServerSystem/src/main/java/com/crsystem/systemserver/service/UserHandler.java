/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.UserRequest;
import com.crsystem.common.dto.ResponseDto;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.controller.RequestHandler;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;
import com.crsystem.systemserver.dao.UserFileManager;

import java.io.ObjectOutputStream;

import java.util.List;

/**
 * 사용자 관련 제어기 
 * @author wonsik
 */
public class UserHandler implements RequestHandler{
    private UserCatalog catalog;
    private UserFileManager fileManager;

    public UserHandler() {
        this.fileManager = new UserFileManager();
        List<User> loadedUsers = fileManager.loadAll();
        this.catalog = new UserCatalog(loadedUsers);
    }
    
    @Override
    public void process(Object request, ObjectOutputStream out){
        try {
        // 클라이언트에게 요청 받음 
        UserRequest requestedUser = (UserRequest) request;
        String action = requestedUser.getActionType(); // 예: "ADD_USER"
        
        if ("ADD_USER".equals(action)) {
            // 2. 택배 상자 안에서 데이터(String, Role)만 쏙 빼서 서버 내부 메서드(User 객체 사용)로 던집니다.
            boolean isSuccess = requestAddUser(requestedUser.getRole(), 
                                               requestedUser.getId(), 
                                               requestedUser.getName());
            
            // 3. 처리 결과를 다시 택배 상자(Response DTO)에 포장해서 클라이언트로 쏩니다. (DTO 사용 지점 2)
            if (isSuccess) {
                out.writeObject(new ResponseDto("가입 완료"));
            } else {
                out.writeObject(new ResponseDto("가입 실패"));
            }
            out.flush();
        }
        
        // else if ("DELETE_USER" ... ) 
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    // ====================
    // CD 1.1 사용자 추가 
    // ====================
    public synchronized boolean requestAddUser(Role role, String id, String name) {
        // E1. 중복 검사 
        if (catalog.findUser(id) != null) {
            System.out.println("가입 실패: 이미 존재하는 아이디입니다. (" + id 
                                                                + " : " 
                                                                + catalog.findUser(id).getName()+ ")");
                return false;
            }
            
            // 생성할 사용자 생성 
            User targetUser = new User(role, id, name);
            
        try {
            // CD 1.2
            fileManager.add(targetUser); 
            // CD 1.3
            catalog.addUser(targetUser); 

            return true; // 가입 완벽 성공

        } catch (Exception e) {
            // 파일 쓰기 실패 시 예외가 이쪽으로 넘어옵니다.
            e.printStackTrace();
            return false; // 메모리에도 안 올라가고, 안전하게 가입 실패 처리
        }
    }
    
    // ====================
    // CD 1.1 사용자 삭제 
    // ====================
    public synchronized boolean requestDeleteUser(String id){
        User targetUser = catalog.findUser(id);
        // nullPointerException
        if (targetUser == null) {
            System.out.println("삭제 실패: 존재하지 않는 계정입니다.");
            return false;
        }
    
        // 관리자 계정 삭제 금지 
        if ("admin".equals(targetUser.getId())) {
            System.out.println("삭제 실패: 최고 관리자 계정은 삭제할 수 없습니다.");
            return false;
        }
        
        try {
            fileManager.delete(id);             // 파일에서 영구 삭제
            catalog.deleteUser(targetUser);     // 메모리 명단에서 쏙 빼기
            return true;                        // 삭제 완벽 성공
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
    }
}
