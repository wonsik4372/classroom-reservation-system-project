package com.crsystem.systemserver.main;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [SFR-801] 연결된 TCP 클라이언트의 접속 수, 식별 번호, 연결 유지 시간을 메모리 상에서 실시간 추적
 */
public class SessionRegistry {

    private static final SessionRegistry INSTANCE = new SessionRegistry();
    private final ConcurrentHashMap<Integer, ClientHandler> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    private SessionRegistry() {}

    // ====================
    // [SFR-801] 싱글톤 인스턴스 반환 - 전역에서 동일 레지스트리 공유
    // ====================
    public static SessionRegistry getInstance() {
        return INSTANCE;
    }

    // ====================
    // [SFR-801] 세션 등록 - 클라이언트 접속 시 고유 세션 ID 부여 후 등록
    // [SFR-802] 세션 관리 - 접속 수 실시간 추적
    // ====================
    public int register(ClientHandler handler) {
        int sessionId = idSequence.incrementAndGet();
        sessions.put(sessionId, handler);
        printStatus("연결", sessionId);
        return sessionId;
    }

    // ====================
    // [SFR-801] 세션 해제 - 클라이언트 종료 시 세션 제거 및 접속 수 감소
    // [SFR-803] 세션 관리 - 클라이언트 연결 종료 처리
    // ====================
    public void unregister(int sessionId) {
        sessions.remove(sessionId);
        printStatus("해제", sessionId);
    }

    // ====================
    // [SFR-804] 세션 관리 - 현재 등록된 모든 ClientHandler 목록 반환
    // ====================
    public Collection<ClientHandler> getSessions() {
        return sessions.values();
    }

    // ====================
    // [SFR-805] 세션 관리 - 현재 접속 중인 클라이언트 수 반환
    // ====================
    public int getCount() {
        return sessions.size();
    }

    private void printStatus(String event, int sessionId) {
        System.out.printf("[SessionRegistry] 세션 %s: ID=%d | 현재 접속 수=%d%n",
                event, sessionId, sessions.size());
    }
}
