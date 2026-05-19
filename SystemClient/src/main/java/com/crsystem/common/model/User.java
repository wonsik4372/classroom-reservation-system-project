/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;

import java.io.Serializable;

/**
 * 사용자 정보 
 * @author wonsik
 */

public class User implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String id;      // 학번 or 교원 번호 or 직원 번호 
    private String pw;      // 비번 (초기에는 id와 같음)
    private String role;    // 권한 
    private String name;    // 이름 
    
    // 로그인을 위한 생성자 
    public User(String id, String pw, String role) {
        this.id = id;
        this.pw = pw; 
        this.role = role;
    }
    
    // 관리자가 사용자 추가를 위한 생성자 
    public User(String id, String pw, String role, String name){
        this.id = id;
        this.pw = pw;
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

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
    
    // Setter 
    public void setPw(String pw) {
        this.pw = pw;
    }
    
}
