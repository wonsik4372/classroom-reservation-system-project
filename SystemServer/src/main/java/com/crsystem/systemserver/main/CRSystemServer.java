/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.crsystem.systemserver.main;

import com.crsystem.systemserver.service.ScheduleInitializer;

import java.io.InputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * 서버 메인
 * @author wonsik
 */
public class CRSystemServer {
    private int serverPort;

    public CRSystemServer() {}

    public static void main(String[] args) {
        // XLS → JSON 초기화
        new ScheduleInitializer().convertAllTimetableToJsons("data/timetable", "data/masterfile");

        CRSystemServer server = new CRSystemServer();
        server.loadProperties();
        server.startServer();
    }
    
    // application.properties 로드 (src/main/resources → classpath에서 읽음)
    public void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new IllegalStateException("application.properties를 찾을 수 없습니다.");
            props.load(input);
            this.serverPort = Integer.parseInt(props.getProperty("server.port", "9998"));
        } catch (Exception e) {
            System.err.println("설정 파일 로드 실패. 기본 포트 9998을 사용합니다.");
            this.serverPort = 9998;
        }
    }
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(this.serverPort)) {
            System.out.println("강의실 예약 서버가 시작되었습니다. (포트: " + this.serverPort + ")");
            
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결됨: " + clientSocket.getInetAddress());
                
                // 소켓 던져줌 
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}