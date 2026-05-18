package com.crsystem.systemserver.main;

import com.crsystem.systemserver.main.ClientHandler;
import com.crsystem.systemserver.service.ClassroomService; // 💡 본인의 서비스 로직 임포트
import com.crsystem.systemserver.service.ScheduleInitializer; // 💡 본인의 초기화 로직 임포트

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CRSystemServer {
    private int serverPort;
    private ClassroomService classroomService; // 💡 파일 제어 서비스 인스턴스 전역 유지

    public static void main(String[] args) {
        CRSystemServer server = new CRSystemServer();
        server.loadConfigFile();    
        server.initBackendFiles();  // 💡 [추가] 가동 시 PDF 파싱 및 JSON 초기화 실행
        server.startServer();       
    }
    
    // config.txt 읽기
    public void loadConfigFile(){
        try(BufferedReader reader = new BufferedReader(new FileReader(("config.txt")))){
            String line;
            while((line = reader.readLine())!=null){    
                if(line.startsWith("server_port")){                        
                    this.serverPort = Integer.parseInt(line.split("=")[1].trim());     
                }
            }
        }
        catch(Exception e){
            System.err.println("⚙️ config.txt 로드 실패. 기본 포트 9998을 사용합니다.");
            this.serverPort = 9998;
        }
    }
    
    // 💡 [추가] 백엔드 데이터 엔진 초기화
    private void initBackendFiles() {
        String pdfFolderPath = "src/main/resources/pdf";
        String masterJsonPath = "src/main/resources/schedule_master.json";

        System.out.println("🔄 [시스템] PDF 시간표 분석 및 마스터 JSON 동기화 중...");
        ScheduleInitializer initializer = new ScheduleInitializer();
        initializer.convertMultiplePdfsToJson(pdfFolderPath, masterJsonPath);
        
        // 싱글톤 성격의 서비스 객체 생성
        this.classroomService = new ClassroomService(masterJsonPath);
        System.out.println("✅ [시스템] 파일 데이터베이스 동기화 완료.");
    }
    
    // 서버 시작
    public void startServer(){
        try(ServerSocket serverSocket = new ServerSocket(this.serverPort)){
            System.out.println("강의실 예약 관리 서버가 시작되었습니다. (포트: " + this.serverPort + ")");
            
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트가 연결되었습니다. : " + clientSocket.getInetAddress());
                
                // 💡 [수정] 핸들러를 생성할 때 공용 classroomService 인스턴스를 주입합니다.
                ClientHandler clientHandler = new ClientHandler(clientSocket, this.classroomService);
                new Thread(clientHandler).start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}