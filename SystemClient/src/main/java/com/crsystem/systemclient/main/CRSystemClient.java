
package com.crsystem.systemclient.main;

import com.crsystem.systemclient.view.LoginGUI;
import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.io.ObjectOutputStream; 
import java.io.ObjectInputStream; 

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

/**
 *
 * @author wonsik
 */
public class CRSystemClient {
    
    /**
     * @param args the command line arguments
     */
    
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private ObjectOutputStream writer;
    private ObjectInputStream reader;
    
    public CRSystemClient() {
        // 기본값 설정 
        this.serverIp = "127.0.0.1";
        this.serverPort = 9998;
    }
    
    public static void main(String[] args) {
        CRSystemClient client = new CRSystemClient();
        
        try {
            // 서버 연결 시도
            client.connectToServer();            
            // 연결 성공 시 LoginGUI 실행
            java.awt.EventQueue.invokeLater(() -> {
                // LoginGUI 생성자에 writer와 reader를 넘김 
                new LoginGUI().setVisible(true);
            });            
        } 
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "서버에 연결할 수 없습니다.\n" + e.getMessage(), 
                "연결 실패", JOptionPane.ERROR_MESSAGE);
            // 연결 실패 시 프로그램 종료
            System.exit(1); 
        }
    }

    // 설정 파일 로드
    public void loadConfigFile(){
        try(BufferedReader reader = new BufferedReader(new FileReader("config.txt"))){
            String line;
            while((line=reader.readLine()) != null){
                if(line.startsWith("server_ip=")){
                    this.serverIp = line.split("=")[1];                                      
                }
                else if(line.startsWith("server_port=")){
                    this.serverPort = Integer.parseInt(line.split("=")[1]);
                }
            }
        }
        catch(Exception e){
            System.err.println("config.txt 로드 실패, 기본값 사용: " + e.getMessage());
        }
    }

    // 서버 연결 및 스트림 초기화
    public void connectToServer() throws IOException {
        // 설정 로드
        loadConfigFile(); 
        
        // 소켓 연결
        System.out.println("서버 연결 시도: " + serverIp + ":" + serverPort);
        this.socket = new Socket(serverIp, serverPort);
        
        //스트림 생성
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.writer.flush();
        this.reader = new ObjectInputStream(socket.getInputStream());
        
        System.out.println("네트워크 연결 및 스트림 설정 완료.");
    }
    
    public void setupNetworking() {
        loadConfigFile(); // IP와 Port를 먼저 로드
        
        try {
            this.socket = new Socket(serverIp, serverPort);
            
            this.writer = new ObjectOutputStream(socket.getOutputStream());
            this.writer.flush();
            this.reader = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("네트워크 연결 및 스트림 설정 완료.");

        } catch (IOException e) {
            System.err.println("네트워크 설정 오류: 서버 연결 실패 또는 스트림 초기화 오류.");
            e.printStackTrace();
            // 오류 발생 시 writer/reader는 null 상태.
        }
    }

    // Getter
    public ObjectOutputStream getWriter() { return this.writer; }
    public ObjectInputStream getReader() { return this.reader; }
    public String getServerIp() { return this.serverIp; }
    public int getServerPort() { return this.serverPort; }
    
    // 자원 해제 (필요시 호출)
    public void close() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Object send(Object request) {

        // 네트워크가 아직 안 열려 있으면 먼저 연결
        if (socket == null || socket.isClosed() || writer == null || reader == null) {
            setupNetworking();
        }

        try {
            // 1) 서버로 객체 전송
            writer.writeObject(request);
            writer.flush();

            // 2) 서버 응답 수신
            Object response = reader.readObject();
            return response;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("서버 통신 중 오류: " + e.getMessage());
            e.printStackTrace();
            return null;   // GUI 쪽에서 null 체크해서 에러 메시지 보여주면 됨
        }
    }
}
    
