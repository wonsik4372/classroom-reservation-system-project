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
        validateIdFormat(role, id);
        
        this.id = id;
        this.role = role;
        this.name = name;       
        this.pw = id;
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
    // ====================
    // [SFR-003] 학생 ID는 8자리 숫자, 초기 비밀번호는 학번과 동일
    // [SFR-004] 교수/조교 ID는 5자리 숫자
    // [SFR-005] 관리자 ID는 4~10자리 영문/숫자
    // ====================
    private void validateIdFormat(Role role, String id) {
        if (role == Role.PROFESSOR || role == Role.ASSISTANT) {
            if (!id.matches("^\\d{5}$")) {
                throw new IllegalArgumentException("가입 실패: 교직원 ID는 5자리 숫자여야 합니다.");
            }
        } else if (role == Role.STUDENT) {
            if (!id.matches("^\\d{8}$")) {
                throw new IllegalArgumentException("가입 실패: 학생 ID는 8자리 숫자여야 합니다.");
            }
        } else if (role == Role.ADMIN) {
            if (!id.matches("^[a-zA-Z0-9]{4,10}$")) {
                throw new IllegalArgumentException("가입 실패: 관리자 ID는 4~10자리 영문/숫자여야 합니다.");
            }
        }
    }
    
    // 비번 검증 
    public boolean verifyPassword(String pw) {
        return this.pw.equals(pw);
    }
    
    // 사용자가 관리자인지 검증 | 삭제 금지 
    public boolean isAdmin() {
        return "admin".equals(this.id) || this.role == Role.ADMIN; 
    }
    
}
