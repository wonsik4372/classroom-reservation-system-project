package com.crsystem.systemserver.main;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SessionRegistryTest {

    private SessionRegistry registry;
    private final List<ServerSocket> serverSockets = new ArrayList<>();
    private final List<Socket> clientSockets = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        registry = SessionRegistry.getInstance();

        Field sessionsField = SessionRegistry.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        ((java.util.concurrent.ConcurrentHashMap<?, ?>) sessionsField.get(registry)).clear();

        Field idField = SessionRegistry.class.getDeclaredField("idSequence");
        idField.setAccessible(true);
        ((AtomicInteger) idField.get(registry)).set(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (Socket s : clientSockets) { try { s.close(); } catch (Exception ignored) {} }
        for (ServerSocket ss : serverSockets) { try { ss.close(); } catch (Exception ignored) {} }
        clientSockets.clear();
        serverSockets.clear();
    }

    /** 테스트용 더미 ClientHandler 생성 (로컬 소켓 쌍 사용, 블로킹 방지) */
    private ClientHandler createDummyHandler() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        serverSockets.add(ss);

        // 클라이언트 쪽 스트림을 먼저 열어야 서버 ObjectInputStream 블로킹 해제됨
        Socket client = new Socket("localhost", ss.getLocalPort());
        clientSockets.add(client);
        java.io.ObjectOutputStream clientOut = new java.io.ObjectOutputStream(client.getOutputStream());
        clientOut.flush();

        Socket server = ss.accept();
        clientSockets.add(server);
        return new ClientHandler(server);
    }

    // ====================
    // [TC-52] SFR-801 - 세션 등록 시 고유 ID 부여
    // ====================
    @Test
    public void register_assignsUniqueSessionId() throws Exception {
        ClientHandler h1 = createDummyHandler();
        ClientHandler h2 = createDummyHandler();

        int id1 = registry.register(h1);
        int id2 = registry.register(h2);

        assertNotEquals(id1, id2);
        assertEquals(2, registry.getCount());
    }

    // ====================
    // [TC-53] SFR-801 - 세션 해제 시 접속 수 감소
    // ====================
    @Test
    public void unregister_decreasesSessionCount() throws Exception {
        ClientHandler h1 = createDummyHandler();
        ClientHandler h2 = createDummyHandler();

        int id1 = registry.register(h1);
        registry.register(h2);
        assertEquals(2, registry.getCount());

        registry.unregister(id1);
        assertEquals(1, registry.getCount());
    }

    // ====================
    // [TC-54] SFR-801 - 초기 상태에서 접속 수 0
    // ====================
    @Test
    public void getCount_returnsZeroWhenNoSessions() {
        assertEquals(0, registry.getCount());
    }

    // ====================
    // [TC-55] SFR-801 - 세션 목록 조회
    // ====================
    @Test
    public void getSessions_returnsAllRegisteredHandlers() throws Exception {
        ClientHandler h1 = createDummyHandler();
        ClientHandler h2 = createDummyHandler();

        registry.register(h1);
        registry.register(h2);

        Collection<ClientHandler> sessions = registry.getSessions();
        assertEquals(2, sessions.size());
        assertTrue(sessions.contains(h1));
        assertTrue(sessions.contains(h2));
    }

    // ====================
    // [TC-56] SFR-801 - 인스턴스 동일성 보장
    // ====================
    @Test
    public void getInstance_returnsSameInstance() {
        SessionRegistry a = SessionRegistry.getInstance();
        SessionRegistry b = SessionRegistry.getInstance();
        assertSame(a, b);
    }
}
