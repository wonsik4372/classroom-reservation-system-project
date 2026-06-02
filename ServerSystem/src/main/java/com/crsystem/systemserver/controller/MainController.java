package com.crsystem.systemserver.controller;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemserver.service.LoginService;
import com.crsystem.systemserver.service.NotificationService;
import com.crsystem.systemserver.service.ReservationService;
import com.crsystem.systemserver.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
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
        commandMap.put("LOGIN", req ->
            LoginService.getInstance().processLogin((UserDTO.Request) req.getPayload())
        );

        // ==========================================
        // 2. [UserService 위임]
        // ==========================================
        commandMap.put("GET_USER_LIST", req ->
            UserService.getInstance().getUserList()
        );
        commandMap.put("ADD_USER", req ->
            UserService.getInstance().addUser((UserDTO.Request) req.getPayload())
        );
        commandMap.put("DELETE_USER", req ->
            UserService.getInstance().deleteUser((UserDTO.Request) req.getPayload())
        );

        // ==========================================
        // 3. [NotificationService 위임]
        // ==========================================
        commandMap.put("GET_NOTIFICATIONS", req -> {
            String userId = (String) req.getPayload();
            return NotificationService.getInstance().getAndMarkNotifications(userId);
        });

        // ==========================================
        // 4. [ReservationService 위임]
        // ==========================================
        commandMap.put("ADD_RESERVATION", req ->
            ReservationService.getInstance().addReservation(
                (ReservationDTO.Response) req.getPayload()
            )
        );
        commandMap.put("GET_RESERVATION_LIST", req ->
            ReservationService.getInstance().getReservationList()
        );
        // 조교 화면 대기 탭용 (PENDING 상태만 필터링)
        commandMap.put("GET_PENDING_RESERVATIONS", req ->
            ReservationService.getInstance().getPendingReservationList()
        );
        commandMap.put("APPROVE_RESERVATION", req ->
            ReservationService.getInstance().approveReservation(
                ((ReservationDTO.Request) req.getPayload()).getReservationId()
            )
        );
        commandMap.put("REJECT_RESERVATION", req ->
            ReservationService.getInstance().rejectReservation(
                ((ReservationDTO.Request) req.getPayload()).getReservationId(),
                ((ReservationDTO.Request) req.getPayload()).getRejectReason()
            )
        );
        commandMap.put("CANCEL_RESERVATION", req ->
            ReservationService.getInstance().cancelReservation(
                ((ReservationDTO.Request) req.getPayload()).getReservationId(),
                ((ReservationDTO.Request) req.getPayload()).getUserId()
            )
        );
    }

    /**
     * GRASP Controller: 진입점에서 커맨드를 분석해 비즈니스 레이어로 토스합니다.
     */
    public static ResponseDTO handleRequest(RequestDTO req) {
        String cmd = req.getCommand();
        System.out.println("[ServerController] 라우팅 중... -> Command: " + cmd);

        // if-else 분기문 없이 다형성(Map)을 이용한 즉시 바인딩
        Command commandAction = commandMap.get(cmd);

        if (commandAction != null) {
            return commandAction.execute(req);
        }

        // 미구현 방어용 응답 (Fail-Fast)
        ResponseDTO res = new ResponseDTO();
        res.setResult("FAIL");
        res.setMessage("지원하지 않는 명령어입니다: " + cmd);
        return res;
    }
}
