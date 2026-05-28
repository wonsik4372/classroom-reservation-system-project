/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.controller.RequestHandler;
import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.model.UserCatalog;

import java.io.ObjectOutputStream;

/**
 *
 * @author wonsik
 */
public class LoginHandler implements RequestHandler{
    private UserCatalog catalog;
    
    public LoginHandler(UserCatalog catalog){
        this.catalog = catalog;
    }
    
    @Override
    public void process(Object request, ObjectOutputStream out) {
        try {
        // 로그인 입력값
        User user = (User) request;
        
        //로그인 성공 여부
        boolean isSuccess = requestLogin(user.getId(), user.getPw());
        
        // 응답 전송 
        String responseMessage = isSuccess ? "로그인 성공!" : "로그인 실패: 정보 또는 권한이 맞지 않습니다.";
            out.writeObject(responseMessage);
            out.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    // 산출물 수정 필요 
    // boolean -> Role 권한은 로그인 이후 화면을 위해서만 사용
    public boolean requestLogin(String id, String pw){
        
        User user = catalog.findUser(id);
        
        // 사용자가 존재하지 않으면 실패
        if (user == null) {
            return false;
        }

        // 비밀번호 검증 
        return user.verifyPassword(pw);       
    }
}
