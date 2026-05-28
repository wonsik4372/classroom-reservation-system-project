/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.main;

import com.crsystem.systemserver.controller.RequestHandler;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;


/**
 * 클라이언트 통신
 * @author wonsik
 */
// import Common.model.*;


public class ClientHandler implements Runnable {    // Runnable: 멀티 스레드 
    private Socket clientSocket;
    
    ObjectOutputStream out;
    ObjectInputStream in;
  
    
    private HashMap<Class<?>, RequestHandler> commandMap;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try{
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }
        catch(IOException e){
            e.printStackTrace();
        }
        // 라우팅 테이블 
        this.commandMap = new HashMap(); 
        
    }
    
    // 모든 통신 처리
    @Override
    public void run() {
        Object requestObject;
        try{
            while ((requestObject = in.readObject())!=null){
                RequestHandler requestHandler = commandMap.get(requestObject.getClass());               
                if(requestHandler != null){
                    requestHandler.process(requestObject, out);
                    try {
                        out.reset(); // ObjectOutputStream 재사용의 핵심
                    } catch (IOException resetException) {
                        System.err.println("ObjectOutputStream reset 오류: " + resetException.getMessage());
                    }
                }
                else{
                    out.writeObject("오류: 처리할 수 없는 요청 객체입니다.");
                    out.flush(); 
                    out.reset();
                }
            }
        }
        catch (IOException e) {
            // 클라이언트가 정상 종료하면 이쪽으로 옵니다 (EOFException).
            System.out.println("클라이언트 연결이 종료되었습니다: " + e.getMessage());
        }
        catch (ClassNotFoundException e) {
            // 서버에 클래스(.class) 파일이 없을 때 발생합니다.
            System.err.println("클래스 불일치 오류: " + e.getMessage());
            e.printStackTrace();
        } 
        finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("핸들러 스레드 종료됨.");
        }
    }
}
