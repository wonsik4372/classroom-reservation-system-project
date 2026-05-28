/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.model;

import com.crsystem.common.enums.Role;
/**
 * 사용자 정보 entity
 * @author wonsik
 */

public class User {
    private Role role;    // 권한
    private String id;      // 학번 or 교원 번호 or 직원 번호 
    private String pw;      // 비번 (초기에는 id와 같음) 
    private String name;    // 이름 
    
    // 로그인을 위한 생성자 
    public User(Role role, String id, String pw) {
        this.role = role;
        this.id = id;
        this.pw = pw; 
    }
    
    // 관리자가 사용자 추가를 위한 생성자 
    public User(String id, Role role, String name){
        this.id = id;
        this.role = role;
        this.name = name;       
    }
    
    // Getter
    public String getId() {
        return id;
    }

    public String getPw() {
        return pw;
    }

    public Role getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
    
    // Setter 
    public void setPw(String pw) {
        this.pw = pw;
    }
    
    // Method
    // 비번 검증 
    public boolean verifyPassword(String pw) {
        return this.pw.equals(pw);
    }
    
    // 사용자가 관리자인지 검증 | 삭제 금지 
    public boolean isAdmin() {
        return "admin".equals(this.id) || this.role == Role.ADMIN; 
    }
    
}
