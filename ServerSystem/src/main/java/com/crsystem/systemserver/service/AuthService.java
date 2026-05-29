/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;


import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;
import com.crsystem.systemserver.dao.UserFileManager;

/**
 *
 * @author wonsik
 */
public class AuthService {
    
    private static AuthService instance;
    //  final UserCatalog catalog;
    
    // private 생성자
    private AuthService() {
    }
    
    // 인스턴스 반환
    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    // 비즈니스 로직
    public ResponseDTO processLogin(UserDTO.Request req) {
        String id = req.getId();
        String pw = req.getPw();
        Role requestedRole = req.getRole();
        
        // UserService를 통해 Catalog 접근 
        User user = UserService.getInstance().getUserById(id);
        
        // 계정이 없는 경우
        if (user == null) {
            return new ResponseDTO(false, "로그인 실패: 존재하지 않는 계정입니다.", null);
        }
        
        // 권한 검증 
        boolean isRoleValid = false;
        
        if (user.getRole() == requestedRole) {
            isRoleValid = true; // 본인 권한 로그인 
        } else if (user.getRole() == Role.ASSISTANT && requestedRole == Role.PROFESSOR) {
            isRoleValid = true; // 조교가 교수 화면 접근 가능 
        }

        // 권한 접근 제한 
        if (!isRoleValid) {
            return new ResponseDTO(false, "로그인 실패: 해당 권한으로 로그인할 수 없습니다.", null);
        }

        // 비밀번호 불일치
        if (!user.verifyPassword(pw)) {
            return new ResponseDTO(false, "로그인 실패: 비밀번호가 일치하지 않습니다.", null);
        }
        
        // 응답 생성 
        UserDTO.Response userInfo = new UserDTO.Response(user.getRole(), user.getId(), user.getName());
        
        // 응답 반환 
        return new ResponseDTO(true, "로그인 성공!", userInfo);
    }
}