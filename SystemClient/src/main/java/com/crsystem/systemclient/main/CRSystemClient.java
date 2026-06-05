/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.crsystem.systemclient.main;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.view.LoginGUI;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * 클라이언트 메인 
 * @author wonsik
 */
public class CRSystemClient {

    // 싱글톤 인스턴스
    private static CRSystemClient instance;

    private static final int HEARTBEAT_INTERVAL_SEC = 8;

    private String serverIp;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream writer;
    private ObjectInputStream reader;

    // [SFR-802] 하트비트 전송 스케줄러
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatTask;

    // 외부에서 객체 생성 차단
    private CRSystemClient() {}

    // 인스턴스 접근 메서드
    public static CRSystemClient getInstance() {
        if (instance == null) {
            instance = new CRSystemClient();
        }
        return instance;
    }

    public static void main(String[] args) {
        CRSystemClient client = CRSystemClient.getInstance();

        try {
            // 서버 연결 시도
            client.connectToServer();            

            // 성공 시 LoginGUI 실행
            SwingUtilities.invokeLater(() -> {
                new LoginGUI().setVisible(true);
            });            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "서버에 연결할 수 없습니다.\n" + e.getMessage(), 
                "연결 실패", JOptionPane.ERROR_MESSAGE);
            System.exit(1); 
        }
    }

    // ====================
    // 설정 로드 및 네트워크 초기화
    // ====================

    private void loadProperties() throws IOException {
        Properties props = new Properties();
        // 루트 디렉토리의 application.properties 파일을 읽음
        try (InputStream input = new FileInputStream("application.properties")) {
            props.load(input);
            this.serverIp = props.getProperty("server.ip", "127.0.0.1"); // 없으면 기본값
            this.serverPort = Integer.parseInt(props.getProperty("server.port", "9998"));
        }
    }

    public void connectToServer() throws Exception {
        loadProperties(); 

        System.out.println("서버 연결 시도: " + serverIp + ":" + serverPort);
        this.socket = new Socket(serverIp, serverPort);

        // 데드락 방지를 위해 반드시 OutputStream 먼저 생성 및 flush
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.writer.flush();
        this.reader = new ObjectInputStream(socket.getInputStream());

        System.out.println("네트워크 연결 및 스트림 설정 완료.");
        startHeartbeat();
    }

    // [SFR-802] 서버로 주기적 HEARTBEAT 전송
    private void startHeartbeat() {
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (socket == null || socket.isClosed()) return;
            try {
                RequestDTO heartbeat = new RequestDTO("HEARTBEAT", null);
                synchronized (this) {
                    writer.writeObject(heartbeat);
                    writer.flush();
                    writer.reset();
                    // PONG 응답 소비 (블로킹 최소화)
                    reader.readObject();
                }
            } catch (Exception e) {
                System.out.println("[Heartbeat] 서버 응답 없음: " + e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    // ====================
    // 비동기 통신 로직 (프리징 방지)
    // ====================

    /**
     * @param request 서버로 보낼 DTO 객체
     * @param onSuccess 통신 성공 시 GUI를 갱신할 콜백 함수
     * @param onFailure 통신 실패 시 에러 메시지를 띄울 콜백 함수
     */
    public void sendRequest(Object request, Consumer<ResponseDTO> onSuccess, Consumer<String> onFailure) {
        if (socket == null || socket.isClosed() || writer == null || reader == null) {
            onFailure.accept("네트워크가 연결되어 있지 않습니다.");
            return;
        }

        // 백그라운드 스레드에서 I/O 블로킹 처리
        // synchronized: 여러 요청이 동시에 같은 스트림에 write하면 StreamCorruptedException 발생
        // → 요청 1건이 완전히 끝난 후 다음 요청이 시작되도록 직렬화
        new Thread(() -> {
            synchronized (this) {
                try {
                    // 요청 전송
                    writer.writeObject(request);
                    writer.flush();
                    writer.reset(); // 객체 캐시 초기화

                    // 응답 대기
                    Object responseObj = reader.readObject();

                    // 성공 콜백 실행
                    if (responseObj instanceof ResponseDTO) {
                        ResponseDTO response = (ResponseDTO) responseObj;
                        SwingUtilities.invokeLater(() -> onSuccess.accept(response));
                    } else {
                        SwingUtilities.invokeLater(() -> onFailure.accept("알 수 없는 응답 객체입니다."));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> onFailure.accept("통신 중 오류 발생: " + e.getMessage()));
                }
            }
        }).start();
    }

    // 자원 해제
    public void close() {
        if (heartbeatTask != null) heartbeatTask.cancel(true);
        heartbeatScheduler.shutdownNow();
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
    
