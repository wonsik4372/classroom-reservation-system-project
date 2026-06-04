/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.main;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemserver.controller.MainController;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 클라이언트 통신
 * [SFR-801] sessionId, connectTime으로 접속 식별 및 연결 유지 시간 추적
 * [SFR-802] 하트비트 무응답 3회 감지 시 비정상 종료 판단
 * [SFR-803] 비정상 종료 감지 시 소켓/스레드 자원 강제 회수
 */
public class ClientHandler implements Runnable {

    private static final int HEARTBEAT_INTERVAL_SEC = 10;
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // [SFR-801]
    private int sessionId;
    private final LocalDateTime connectTime = LocalDateTime.now();

    // [SFR-802]
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatWatchdog;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    /** [SFR-802] 클라이언트로부터 HEARTBEAT 수신 시 호출 — 무응답 카운터 리셋 */
    public void onHeartbeatReceived() {
        missedHeartbeats.set(0);
    }

    /** [SFR-802] 하트비트 감시 타이머 시작 */
    private void startHeartbeatWatchdog() {
        heartbeatWatchdog = scheduler.scheduleAtFixedRate(() -> {
            int missed = missedHeartbeats.incrementAndGet();
            System.out.printf("[SFR-802] 세션 %d 하트비트 무응답 %d회%n", sessionId, missed);
            if (missed >= MAX_MISSED_HEARTBEATS) {
                System.out.printf("[SFR-803] 세션 %d 비정상 종료 감지 → 자원 강제 회수%n", sessionId);
                forceClose();
            }
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /** [SFR-803] 소켓/스레드 자원 강제 회수 */
    private void forceClose() {
        try {
            if (!clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        if (heartbeatWatchdog != null) heartbeatWatchdog.cancel(true);
        scheduler.shutdownNow();
        SessionRegistry.getInstance().unregister(sessionId);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("[ClientHandler] 세션 %d 핸들러 스레드 반환됨.%n", sessionId);
    }

    @Override
    public void run() {
        // [SFR-801] 세션 등록
        this.sessionId = SessionRegistry.getInstance().register(this);
        System.out.printf("[SFR-801] 세션 %d 연결: %s | 연결 시각: %s%n",
                sessionId, clientSocket.getInetAddress(), connectTime);

        // [SFR-802] 하트비트 감시 시작
        startHeartbeatWatchdog();

        Object requestObject;
        try {
            while ((requestObject = in.readObject()) != null) {
                if (requestObject instanceof RequestDTO) {
                    RequestDTO req = (RequestDTO) requestObject;

                    // HEARTBEAT는 비즈니스 로직 없이 카운터만 리셋
                    if ("HEARTBEAT".equals(req.getCommand())) {
                        onHeartbeatReceived();
                        ResponseDTO pong = new ResponseDTO();
                        pong.setResult("OK");
                        pong.setMessage("PONG");
                        synchronized (out) {
                            out.writeObject(pong);
                            out.flush();
                            out.reset();
                        }
                        continue;
                    }

                    ResponseDTO res = MainController.handleRequest(req);
                    synchronized (out) {
                        out.writeObject(res);
                        out.flush();
                        out.reset();
                    }
                } else {
                    ResponseDTO errorRes = new ResponseDTO();
                    errorRes.setResult("FAIL");
                    errorRes.setMessage("오류: 처리할 수 없는 요청 규격입니다.");
                    synchronized (out) {
                        out.writeObject(errorRes);
                        out.flush();
                        out.reset();
                    }
                }
            }
        } catch (IOException e) {
            System.out.printf("[ClientHandler] 세션 %d 연결 종료: %s%n", sessionId, clientSocket.getInetAddress());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
}