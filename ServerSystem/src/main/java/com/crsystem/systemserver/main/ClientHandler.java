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
import java.util.HashMap;


/**
 * 클라이언트 통신
 * @author wonsik
 */
// import Common.model.*;


public class ClientHandler implements Runnable {   
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // socket만 받아서 처리 
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        Object requestObject;
        try {
            while ((requestObject = in.readObject()) != null) {
                
                // 받은 데이터 형 검증 
                if (requestObject instanceof RequestDTO) {
                    RequestDTO req = (RequestDTO) requestObject;
                    
                    // 책임 위임 
                    ResponseDTO res = MainController.handleRequest(req);
                    
                    // 결과 전송 
                    out.writeObject(res);
                    out.flush(); 
                    out.reset(); // 캐시 지우기
                } 
                else {
                    // 비정상적인 객체가 날아온 경우의 방어 로직
                    ResponseDTO errorRes = new ResponseDTO();
                    errorRes.setResult("FAIL");
                    errorRes.setMessage("오류: 처리할 수 없는 요청 규격입니다.");
                    out.writeObject(errorRes);
                    out.flush();
                    out.reset();
                }
            }
        } catch (IOException e) {
            System.out.println("클라이언트 연결 종료: " + clientSocket.getInetAddress());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("핸들러 스레드 반환됨.");
        }
    }
}