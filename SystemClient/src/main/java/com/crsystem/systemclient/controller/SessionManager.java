/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.controller;

import com.crsystem.common.dto.UserDTO;

/*
 * 로그인된 사용자의 세션 상태를 관리하는 클래스
 * @author wonsik
 */
public class SessionManager {

    private static SessionManager instance;

    private UserDTO.Response currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // 로그인 성공 시 세션 저장
    public void login(UserDTO.Response user) {
        this.currentUser = user;
        System.out.println("[SessionManager] 로그인: " + user.getId() + " (" + user.getRole() + ")");
    }

    // 로그아웃 시 세션 초기화 + 알림 폴링 중단
    public void logout() {
        System.out.println("[SessionManager] 로그아웃: " + (currentUser != null ? currentUser.getId() : "null"));
        NotificationController.getInstance().stopPolling();
        this.currentUser = null;
    }

    // 현재 로그인된 사용자 반환
    public UserDTO.Response getCurrentUser() {
        return currentUser;
    }

    // 로그인 여부 확인
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
