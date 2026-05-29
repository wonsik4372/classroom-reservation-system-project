/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ResponseDTO; // 봉투(Envelope) DTO
import com.crsystem.common.dto.UserDTO;   // 알맹이(Payload) DTO
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;
import com.crsystem.systemserver.dao.UserFileManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wonsik
 */
public class UserService {
    
    // 싱글톤 패턴 적용
    private static UserService instance;
    private final UserCatalog catalog;
    private final UserFileManager fileManager;

    private UserService() {
        this.fileManager = new UserFileManager();
        // 초기화 시 파일에서 목록 적재 
        this.catalog = new UserCatalog(fileManager.loadAll());
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    // ==========================================
    // 사용자 목록 조회 (GET_USER_LIST)
    // ==========================================
    public ResponseDTO getUserList() {
        ResponseDTO response = new ResponseDTO();
        try {
            // 사용자 목록 조회 
            List<User> userList = catalog.getAllUsers();
            
            // 사용자 -> DTO 변환 
            List<UserDTO.Response> responseList = new ArrayList<>();
            for (User user : userList) {
                responseList.add(new UserDTO.Response(user.getRole(), user.getId(), user.getName()));
            }

            response.setResult("SUCCESS");
            response.setMessage("조회 성공");
            response.setPayload(responseList);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setResult("FAIL");
            response.setMessage("서버 오류: 조회 실패");
        }
        return response;
    }

    // ==========================================
    // 3. 사용자 추가 (ADD_USER)
    // ==========================================
    public ResponseDTO addUser(UserDTO.Request req) {
        ResponseDTO response = new ResponseDTO();
        String id = req.getId();

        // 동기화 사용 중복 검사 
        synchronized(catalog) {
            if (catalog.findUser(id) != null) {
                response.setResult("FAIL");
                response.setMessage("가입 실패: 이미 존재하는 아이디입니다. (" + id + ")");
                return response;
            }
        }

        User targetUser;
        try {
            // User 생성 
            targetUser = new User(id, req.getRole(), req.getName());
        } catch (IllegalArgumentException e) {
            // 에러 메시지 클라이언트로 반환
            response.setResult("FAIL");
            response.setMessage(e.getMessage());
            return response;
        }

        try {
            // 느리면 밖에서 처리 
            fileManager.add(targetUser); 
            
            // 성공 시 갱신 
            synchronized(catalog) {
                catalog.addUser(targetUser); 
            }
            
            response.setResult("SUCCESS");
            response.setMessage("가입 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            response.setResult("FAIL");
            response.setMessage("서버 오류: 파일 저장에 실패했습니다.");
        }
        return response;
    }

    // ==========================================
    // 4. 사용자 삭제 (DELETE_USER)
    // ==========================================
    public ResponseDTO deleteUser(UserDTO.Request req) {
        ResponseDTO response = new ResponseDTO();
        String id = req.getId();
        User targetUser;
    
        // 검증 및 삭제 
        synchronized(catalog) {
            targetUser = catalog.findUser(id);
            
            if (targetUser == null) {
                response.setResult("FAIL");
                response.setMessage("삭제 실패: 존재하지 않는 계정입니다.");
                return response;
            }

            if (targetUser.isAdmin()) {
                response.setResult("FAIL");
                response.setMessage("삭제 실패: 최고 관리자 계정은 삭제할 수 없습니다.");
                return response;
            }
            
            // 파일 삭제 전 메모리 선점 방지
            catalog.deleteUser(targetUser); 
        }

        // 파일 삭제 및 롤백 
        try {
            fileManager.delete(id); 
            response.setResult("SUCCESS");
            response.setMessage("삭제 성공!");
        } catch (Exception e) {
            e.printStackTrace();
            // 실패시 복구 
            synchronized(catalog) {
                catalog.addUser(targetUser); 
            }
            response.setResult("FAIL");
            response.setMessage("서버 오류: 파일 삭제 실패로 취소되었습니다.");
        }
        return response;
    }
}
