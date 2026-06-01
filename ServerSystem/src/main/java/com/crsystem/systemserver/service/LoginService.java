/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;


import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.model.User;

/**
 * 로그인 전문가 
 * @author wonsik
 */
public class LoginService {
    
    // Initialization-on-demand holder: JVM 클래스 로딩 메커니즘으로 동기화 없이 스레드 안전
    private static class Holder {
        private static final LoginService INSTANCE = new LoginService();
    }

    private LoginService() {}

    public static LoginService getInstance() {
        return Holder.INSTANCE;
    }

    // ====================
    // [SFR-001] 올바른 ID/PW로 로그인
    // [SFR-002] 역할(학생/교수/조교/관리자)에 맞는 화면으로 분기
    // [SFR-006] 조교 계정으로 교수 화면 접근 허용
    // ====================
    public ResponseDTO processLogin(UserDTO.Request req) {
        String id = req.getId();
        String pw = req.getPw();
        Role requestedRole = req.getRole();
        
        // UserService를 통해 Catalog 접근 
        User user = UserService.getInstance().getUserById(id);
        
        // 계정이 없는 경우
        if (user == null) {
            return new ResponseDTO(false, "로그인 실패: 존재하지 않는 계정입니다.", null);
        }
        
        // 권한 검증 
        boolean isRoleValid = false;
        
        if (user.getRole() == requestedRole) {
            isRoleValid = true; // 본인 권한 로그인 
        } else if (user.getRole() == Role.ASSISTANT && requestedRole == Role.PROFESSOR) {
            isRoleValid = true; // 조교가 교수 화면 접근 가능 
        }

        // 권한 접근 제한 
        if (!isRoleValid) {
            return new ResponseDTO(false, "로그인 실패: 해당 권한으로 로그인할 수 없습니다.", null);
        }

        // 비밀번호 불일치
        if (!user.verifyPassword(pw)) {
            return new ResponseDTO(false, "로그인 실패: 비밀번호가 일치하지 않습니다.", null);
        }
        
        // 응답 생성
        UserDTO.Response userInfo = new UserDTO.Response(user.getRole(), user.getId(), user.getName());

        // ====================
        // [SFR-408] 로그인 시 미읽음 승인 알림 즉시 전달
        // [SFR-409] 로그인 시 미읽음 거부 알림(거부 사유 포함) 즉시 전달
        // ====================
        // 학생인 경우 미읽음 알림 첨부 (로그인 즉시 알림 전달)
        if (user.getRole() == Role.STUDENT) {
            java.util.List<com.crsystem.common.dto.NotificationDTO> pending =
                com.crsystem.systemserver.model.NotificationStore.getInstance()
                    .getUnreadNotifications(user.getId());
            if (!pending.isEmpty()) {
                java.util.List<String> ids = pending.stream()
                    .map(com.crsystem.common.dto.NotificationDTO::getNotificationId)
                    .collect(java.util.stream.Collectors.toList());
                com.crsystem.systemserver.model.NotificationStore.getInstance()
                    .markAsRead(user.getId(), ids);
                userInfo.setPendingNotifications(pending);
            }
        }

        // 응답 반환
        return new ResponseDTO(true, "로그인 성공!", userInfo);
    }
}