/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import com.crsystem.common.enums.Role;
import java.io.Serializable;

/**
 *
 * @author wonsik
 */
public class UserDTO {
    
    public static class Request implements Serializable {
        private String actionType;
        private String id;
        private String pw;
        private Role role;
        private String name;

        // 직렬화(네트워크 전송)를 위한 기본 생성자
        public Request() {}

        // 로그인 등 특정 목적을 위한 오버로딩 생성자
        public Request(String actionType, String id, String pw) {
            this.actionType = actionType;
            this.id = id;
            this.pw = pw;
        }

        public Request(String actionType, String id, Role role, String name) {
            this.actionType = actionType;
            this.id = id;
            this.role = role;
            this.name = name;
        }

        // --- Getters ---
        public String getActionType() { return actionType; }
        public String getId() { return id; }
        public String getPw() { return pw; }
        public Role getRole() { return role; }
        public String getName() { return name; }

        // --- Setters ---
        public void setActionType(String actionType) { this.actionType = actionType; }
        public void setId(String id) { this.id = id; }
        public void setPw(String pw) { this.pw = pw; }
        public void setRole(Role role) { this.role = role; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class Response implements Serializable {
        private Role role;
        private String id;
        private String name;

        // 직렬화 기본 생성자
        public Response() {}

        // 서버에서 생성해서 클라이언트로 보낼 때 사용할 생성자
        public Response(Role role, String id, String name) {
            this.role = role;
            this.id = id;
            this.name = name;
        }

        // --- Getters ---
        public Role getRole() { return role; }
        public String getId() { return id; }
        public String getName() { return name; }

        // --- Setters ---
        public void setRole(Role role) { this.role = role; }
        public void setId(String id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }
    
}
