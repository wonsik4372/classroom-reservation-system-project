/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemserver.service.LoginService;
import com.crsystem.systemserver.service.ClassroomService;
import com.crsystem.systemserver.service.NotificationService;
import com.crsystem.systemserver.service.ReservationService;
import com.crsystem.systemserver.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wonsik
 */
public class MainController {
    // 1. 행위(Action)를 추상화할 내부 함수형 인터페이스 정의 (Command 패턴)
    @FunctionalInterface
    private interface Command {
        ResponseDTO execute(RequestDTO req);
    }

    // 2. 명령어(String)와 실행 로직(Command)을 연결할 라우팅 맵
    private static final Map<String, Command> commandMap = new HashMap<>();

    // 3. 정적 초기화 블록 (서버 구동 시 최초 1회만 메모리에 적재됨)
    static {
        // ==========================================
        // 1. [LoginService 위임]
        // ==========================================
        commandMap.put("LOGIN", req -> LoginService.getInstance().processLogin((UserDTO.Request) req.getPayload()));
        // ServerController.java 의 static 블록 내부
        commandMap.put("GET_USER_LIST", req -> UserService.getInstance().getUserList());
        commandMap.put("ADD_USER", req -> UserService.getInstance().addUser((UserDTO.Request) req.getPayload()));
        commandMap.put("DELETE_USER", req -> UserService.getInstance().deleteUser((UserDTO.Request) req.getPayload()));

        // ==========================================
        // 2. [NotificationService 위임]
        // ==========================================
        // 학생이 폴링으로 미읽음 알림 조회 (로그인 후 주기적 호출)
        commandMap.put("GET_NOTIFICATIONS", req -> {
            String userId = (String) req.getPayload();
            return NotificationService.getInstance().getAndMarkNotifications(userId);
        });

        // ==========================================
        // 3. [ReservationService 위임]
        // ==========================================
        commandMap.put("STUDENT_RESERVATION_REQUEST", req ->
            ReservationService.getInstance().createReservation((ReservationDTO.Request) req.getPayload())
        );
        commandMap.put("CREATE_RESERVATION", req ->
            ReservationService.getInstance().createReservation((ReservationDTO.Request) req.getPayload())
        );
        commandMap.put("GET_PENDING_RESERVATIONS", req ->
            ReservationService.getInstance().getPendingReservations()
        );
        commandMap.put("GET_ALL_RESERVATIONS", req ->
            ReservationService.getInstance().getAllReservations()
        );
        // 승인: 상태 변경 + 학생 알림 생성
        commandMap.put("APPROVE_RESERVATION", req ->
            ReservationService.getInstance().approveReservation((ReservationDTO.Request) req.getPayload())
        );
        // 거부: 상태 변경 + 거부 사유 포함 학생 알림 생성
        commandMap.put("REJECT_RESERVATION", req ->
            ReservationService.getInstance().rejectReservation((ReservationDTO.Request) req.getPayload())
        );

    }

    /**
     * GRASP Controller: 진입점에서 커맨드를 분석해 비즈니스 레이어로 토스합니다.
     */
    public static ResponseDTO handleRequest(RequestDTO req) {
        String cmd = req.getCommand();
        System.out.println("[ServerController] 라우팅 중... -> Command: " + cmd);
        
        // 4. if-else 분기문 없이 다형성(Map)을 이용한 즉시 바인딩 
        Command commandAction = commandMap.get(cmd);
        
        if (commandAction != null) {
            // 해당하는 커맨드가 존재하면 실행 후 결과 반환
            return commandAction.execute(req);
        }

        // 5. 미구현 방어용 응답 (Fail-Fast)
        ResponseDTO res = new ResponseDTO();
        res.setResult("FAIL");
        res.setMessage("지원하지 않는 명령어입니다: " + cmd);
        return res;
    }
}
