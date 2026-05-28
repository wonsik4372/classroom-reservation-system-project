/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.crsystem.systemserver.main;

import com.crsystem.systemserver.main.ClientHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author wonsik
 */
public class CRSystemServer {
    private int serverPort;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        CRSystemServer server = new CRSystemServer();
        server.loadConfigFile();    // 포트번호 설정 또는 불러오기 
        server.startServer();       // 서버 시작 
       
        
    }
    
    // config.txt 읽기 
    public void loadConfigFile(){
        try(BufferedReader reader = new BufferedReader(new FileReader(("config.txt")))){
            String line;
            while((line = reader.readLine())!=null){    // 파일 한줄 씩 읽음 
                if(line.startsWith("server_port")){                        // server_port로 시작하면 
                    this.serverPort = Integer.parseInt(line.split("=")[1]);     // =을 기준으로 쪼개서 0000과 같은 부분을 serverPort에 부여 
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            this.serverPort = 9998;
        }
    }
    
    // 서버 시작 
    public void startServer(){
        
                
        try(ServerSocket serverSocket = new ServerSocket(this.serverPort)){
            System.out.println("강의실 예약 서버가 시작되었습니다. (포트: " + this.serverPort + ")");
            
            while(true) {
                // 클라이언트 연결 대기 
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트가 연결되었습니다. : " + clientSocket.getInetAddress());
                
                // 클라이언트마다 별도 스레드 생성 후 처리 
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        }
        // 예외 처리
        catch (IOException e){
            e.printStackTrace();
        }
    }
    
}
