/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import java.io.Serializable;

/**
 *
 * @author wonsik
 */

public class User implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String id;      // 학번 or 교원 번호 or 직원 번호 
    private String pw;      // 비번 (초기에는 id와 같음)
    private String role;    // 권한 
    private String name;    // 이름 
    
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
