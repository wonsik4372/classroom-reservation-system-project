package com.crsystem.systemserver.main;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClientHandler 단위 테스트
 * TC-47 (SFR-801) ~ TC-51 (SFR-805)
 */
public class ClientHandlerTest {

    private final List<ServerSocket> serverSockets = new ArrayList<>();
    private final List<Socket> allSockets = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        SessionRegistry registry = SessionRegistry.getInstance();
        Field sessionsField = SessionRegistry.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) sessionsField.get(registry)).clear();
        Field idField = SessionRegistry.class.getDeclaredField("idSequence");
        idField.setAccessible(true);
        ((AtomicInteger) idField.get(registry)).set(0);
    }

    @AfterEach
    void tearDown() {
        for (Socket s : allSockets)       { try { s.close(); }  catch (Exception ignored) {} }
        for (ServerSocket ss : serverSockets) { try { ss.close(); } catch (Exception ignored) {} }
        allSockets.clear();
        serverSockets.clear();
    }

    /**
     * 로컬 소켓 쌍 생성 유틸리티
     * returns Object[] { clientSide(Socket), serverSide(Socket), clientOut(ObjectOutputStream) }
     */
    private Object[] createSocketPair() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        serverSockets.add(ss);

        Socket client = new Socket("localhost", ss.getLocalPort());
        allSockets.add(client);

        // ClientHandler의 ObjectInputStream 초기화가 블로킹되지 않도록 먼저 헤더 전송
        ObjectOutputStream clientOut = new ObjectOutputStream(client.getOutputStream());
        clientOut.flush();

        Socket server = ss.accept();
        allSockets.add(server);
        return new Object[]{client, server, clientOut};
    }

    // ====================
    // [TC-47] SFR-801 - 연결 시각 추적: connectTime이 생성 시점에 설정됨
    // ====================
    @Test
    void connectTime_isSetOnCreation() throws Exception {
        LocalDateTime before = LocalDateTime.now();
        Object[] pair = createSocketPair();
        ClientHandler handler = new ClientHandler((Socket) pair[1]);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(handler.getConnectTime());
        assertFalse(handler.getConnectTime().isBefore(before));
        assertFalse(handler.getConnectTime().isAfter(after));
    }

    // ====================
    // [TC-47] SFR-801 - 세션 등록: run() 호출 시 SessionRegistry에 세션이 등록됨
    // ====================
    @Test
    void run_registersHandlerInSessionRegistry() throws Exception {
        Object[] pair = createSocketPair();
        Socket clientSide = (Socket) pair[0];
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Thread t = new Thread(handler);
        t.start();
        Thread.sleep(200);

        assertEquals(1, SessionRegistry.getInstance().getCount());

        clientSide.close(); // EOFException 유발 → cleanup() → unregister()
        t.join(2000);
    }

    // ====================
    // [TC-48] SFR-802 - onHeartbeatReceived() 호출 시 missedHeartbeats 카운터를 0으로 리셋
    // ====================
    @Test
    void onHeartbeatReceived_resetsMissedHeartbeatCount() throws Exception {
        Object[] pair = createSocketPair();
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Field field = ClientHandler.class.getDeclaredField("missedHeartbeats");
        field.setAccessible(true);
        AtomicInteger counter = (AtomicInteger) field.get(handler);

        counter.set(2);
        handler.onHeartbeatReceived();

        assertEquals(0, counter.get());
    }

    // ====================
    // [TC-48] SFR-802 - HEARTBEAT 명령 수신 시 PONG 응답 반환
    // ====================
    @Test
    void run_respondsToHeartbeatWithPong() throws Exception {
        Object[] pair = createSocketPair();
        Socket clientSide = (Socket) pair[0];
        ObjectOutputStream clientOut = (ObjectOutputStream) pair[2];
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Thread t = new Thread(handler);
        t.start();
        Thread.sleep(200);

        ObjectInputStream clientIn = new ObjectInputStream(clientSide.getInputStream());

        clientOut.writeObject(new RequestDTO("HEARTBEAT", null));
        clientOut.flush();

        ResponseDTO response = (ResponseDTO) clientIn.readObject();
        assertEquals("PONG", response.getMessage());

        clientSide.close();
        t.join(2000);
    }

    // ====================
    // [TC-49] SFR-803 - 연결 종료 시 SessionRegistry에서 세션이 제거됨 (자원 회수)
    // ====================
    @Test
    void run_unregistersSessionOnDisconnect() throws Exception {
        Object[] pair = createSocketPair();
        Socket clientSide = (Socket) pair[0];
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Thread t = new Thread(handler);
        t.start();
        Thread.sleep(200);
        assertEquals(1, SessionRegistry.getInstance().getCount());

        clientSide.close(); // 비정상 종료 시뮬레이션
        t.join(2000);

        assertEquals(0, SessionRegistry.getInstance().getCount());
    }

    // ====================
    // [TC-50] SFR-804 - 알 수 없는 command 수신 시 FAIL 응답 반환
    // ====================
    @Test
    void run_returnsFailResponseForUnknownCommand() throws Exception {
        Object[] pair = createSocketPair();
        Socket clientSide = (Socket) pair[0];
        ObjectOutputStream clientOut = (ObjectOutputStream) pair[2];
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Thread t = new Thread(handler);
        t.start();
        Thread.sleep(200);

        ObjectInputStream clientIn = new ObjectInputStream(clientSide.getInputStream());

        clientOut.writeObject(new RequestDTO("UNKNOWN_COMMAND_XYZ", null));
        clientOut.flush();

        ResponseDTO response = (ResponseDTO) clientIn.readObject();
        assertEquals("FAIL", response.getResult());

        clientSide.close();
        t.join(2000);
    }

    // ====================
    // [TC-51] SFR-805 - 비-RequestDTO 객체 수신 시 에러 응답 반환 (서버 다운 없음)
    // ====================
    @Test
    void run_sendsErrorResponseForNonRequestDTOObject() throws Exception {
        Object[] pair = createSocketPair();
        Socket clientSide = (Socket) pair[0];
        ObjectOutputStream clientOut = (ObjectOutputStream) pair[2];
        ClientHandler handler = new ClientHandler((Socket) pair[1]);

        Thread t = new Thread(handler);
        t.start();
        Thread.sleep(200);

        ObjectInputStream clientIn = new ObjectInputStream(clientSide.getInputStream());

        // String은 Serializable이지만 RequestDTO가 아님 → 불량 데이터 시뮬레이션
        clientOut.writeObject("MALFORMED_DATA");
        clientOut.flush();

        ResponseDTO response = (ResponseDTO) clientIn.readObject();
        assertEquals("FAIL", response.getResult());

        clientSide.close();
        t.join(2000);
    }
}
