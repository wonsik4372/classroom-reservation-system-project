/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.*;
import com.crsystem.common.enums.Role;
import com.crsystem.systemclient.main.CRSystemClient;
import java.util.function.Consumer;

/**
 * 사용자 컨트롤러 
 * @author wonsik
 */

public class UserController {
    private static UserController instance;
    private UserController() {}
    public static UserController getInstance() {
        if (instance == null) instance = new UserController();
        return instance;
    }
    
    // 로그인 
    public void login(String id, String pw, Role role, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        UserDTO.Request payload = new UserDTO.Request();
        payload.setId(id);
        payload.setPw(pw);
        payload.setRole(role);
        CRSystemClient.getInstance().sendRequest(new RequestDTO("LOGIN", payload), onSuccess, onFailure);
    }
    
    // 조회 
    public void getUserList(Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        CRSystemClient.getInstance().sendRequest(new RequestDTO("GET_USER_LIST", null), onSuccess, onFailure);
    }

    // 추가 
    public void addUser(String id, String name, Role role, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        UserDTO.Request payload = new UserDTO.Request();
        payload.setId(id);
        payload.setPw(id);
        payload.setName(name);
        payload.setRole(role);
        CRSystemClient.getInstance().sendRequest(new RequestDTO("ADD_USER", payload), onSuccess, onFailure);
    }

    // 삭제 
    public void deleteUser(String id, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        UserDTO.Request payload = new UserDTO.Request();
        payload.setId(id);
        CRSystemClient.getInstance().sendRequest(new RequestDTO("DELETE_USER", payload), onSuccess, onFailure);
    }
}
