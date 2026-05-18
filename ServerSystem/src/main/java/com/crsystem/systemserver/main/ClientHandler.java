package com.crsystem.systemserver.main;

import com.crsystem.systemserver.service.ClassroomService;
import com.crsystem.systemserver.service.RequestHandler;
import com.crsystem.common.model.*; // 공통 모델 패키지 전체 임포트

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {    
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private HashMap<Class<?>, RequestHandler> commandMap;
    private ClassroomService classroomService; // 주입받은 서비스

    // 생성자: ClassroomService 주입 받기
    public ClientHandler(Socket socket, ClassroomService classroomService) {
        this.clientSocket = socket;
        this.classroomService = classroomService;
        this.commandMap = new HashMap<>(); 
        
        try{
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        // 라우팅 테이블 초기화 호출
        initCommandMap();
    }
    
    /**
     * [핵심 구현] 요청 DTO 타입에 따라 ClassroomService 내부 로직을 호출하는 매핑 함수
     */
    private void initCommandMap() {
        // 1. 특정 건물/층/날짜의 강의실 조회 요청 라우팅
        commandMap.put(RoomListRequest.class, (request, outStream) -> {
            try {
                RoomListRequest req = (RoomListRequest) request;
                
                // 💡 [수정완료] 실제 존재하는 getScheduleData() 메서드를 호출합니다.
                Map<String, Classroom> allRooms = classroomService.getScheduleData().getClassrooms();
                Map<String, Classroom> filteredResult = new java.util.LinkedHashMap<>();
                
                for (Map.Entry<String, Classroom> entry : allRooms.entrySet()) {
                    Classroom room = entry.getValue();
                    // ClassroomInfo에 기록된 건물번호와 층이 매칭되는 것만 선별
                    if (room.getInfo().getDeptNo() == req.getDeptNo() && room.getInfo().getFloor() == req.getFloor()) {
                        filteredResult.put(entry.getKey(), room);
                    }
                }
                
                // 필터링된 맵 객체를 그대로 클라이언트에 송신
                outStream.writeObject(filteredResult);
                outStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 2. 신규 예약 등록 요청 라우팅
        commandMap.put(ReservationRequest.class, (request, outStream) -> {
            try {
                ReservationRequest req = (ReservationRequest) request;
                
                // 💡 [수정완료] addReservation으로 변경하고, 매개변수 순서(강의실명 먼저)와 타입(int->String)을 맞춥니다.
                boolean isSuccess = classroomService.addReservation(
                    req.getClassroomName(),                 // 1. 강의실명 (순서 변경)
                    req.getDate(),                          // 2. 날짜 (순서 변경)
                    req.getTimeSlot(),                      // 3. 시간
                    req.getSubject(),                       // 4. 과목/목적
                    req.getApplicantName(),                 // 5. 신청자
                    String.valueOf(req.getRequesterType())  // 6. int 타입을 String으로 변환
                );
                
                // GUI에게 결과 전송 (성공 true / 실패 false 혹은 문자열)
                outStream.writeObject(isSuccess ? "SUCCESS" : "FAIL");
                outStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public void run() {
        Object requestObject;
        try{
            while ((requestObject = in.readObject()) != null){
                RequestHandler requestHandler = commandMap.get(requestObject.getClass());               
                if(requestHandler != null){
                    requestHandler.process(requestObject, out);
                    try {
                        out.reset(); // 캐시 비우기
                    } catch (IOException resetException) {
                        System.err.println("ObjectOutputStream reset 오류: " + resetException.getMessage());
                    }
                }
                else{
                    out.writeObject("오류: 처리할 수 없는 요청 객체입니다.");
                    out.flush(); 
                    out.reset();
                }
            }
        }
        catch (IOException e) {
            System.out.println("클라이언트 연결이 종료되었습니다: " + e.getMessage());
        }
        catch (ClassNotFoundException e) {
            System.err.println("클래스 불일치 오류: " + e.getMessage());
            e.printStackTrace();
        } 
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("핸들러 스레드 종료됨.");
        }
    }
}