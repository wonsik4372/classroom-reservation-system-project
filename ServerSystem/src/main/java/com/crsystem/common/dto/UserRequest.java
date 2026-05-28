/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import com.crsystem.common.enums.Role;

import java.io.Serializable;

/**
 * 사용자 정보에 대한 요청 | 클라이언트 -> 서버 
 * @author wonsik
 */

public class UserRequest implements Serializable {
    private Role role;    // 권한 1: 관리자 / 2: 조교 / 3: 교수 / 4: 학생 
    private String id;      // 학번 or 교원 번호 or 직원 번호 
    private String pw;      // 비번 (초기에는 id와 같음)
    private String name;    // 이름 
    private String actionType;  // 동작 이름 
    
    // 로그인을 위한 생성자 
    public UserRequest(String id, String pw) {
        this.id = id;
        this.pw = pw; 
    }
    
    // 로그인 테스트 용
    public UserRequest(Role role, String id, String pw) {
        this.role = role;
        this.id = id;
        this.pw = pw; 
    }
    
    // 관리자가 사용자 추가를 위한 생성자 
    public UserRequest(String id, Role role, String name) {
        this.id = id;
        this.role = role;
        this.name = name;       
    }
    
    // Getter
    public Role getRole() {
        return role;
    }
    
    public String getId() {
        return id;
    }

    public String getPw() {
        return pw;
    }

    public String getName() {
        return name;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    // Setter 
    public void setPw(String pw) {
        this.pw = pw;
    }
}
