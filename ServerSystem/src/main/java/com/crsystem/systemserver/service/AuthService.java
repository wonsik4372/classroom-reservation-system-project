/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;


import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;
import com.crsystem.systemserver.dao.UserFileManager;

/**
 *
 * @author wonsik
 */
public class AuthService {
    
    private static AuthService instance;
    private final UserCatalog catalog;
    
    // private 생성자
    private AuthService() {
        UserFileManager fileManager = new UserFileManager();
        this.catalog = new UserCatalog(fileManager.loadAll());
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
        
        User user = catalog.findUser(id);
        
        // 계정이 없는 경우
        if (user == null) {
            return new ResponseDTO(false, "로그인 실패: 존재하지 않는 계정입니다.", null);
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