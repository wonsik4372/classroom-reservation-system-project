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

    public static SessionRegistry getInstance() {
        return INSTANCE;
    }

    public int register(ClientHandler handler) {
        int sessionId = idSequence.incrementAndGet();
        sessions.put(sessionId, handler);
        printStatus("연결", sessionId);
        return sessionId;
    }

    public void unregister(int sessionId) {
        sessions.remove(sessionId);
        printStatus("해제", sessionId);
    }

    public Collection<ClientHandler> getSessions() {
        return sessions.values();
    }

    public int getCount() {
        return sessions.size();
    }

    private void printStatus(String event, int sessionId) {
        System.out.printf("[SessionRegistry] 세션 %s: ID=%d | 현재 접속 수=%d%n",
                event, sessionId, sessions.size());
    }
}
