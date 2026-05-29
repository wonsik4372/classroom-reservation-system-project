/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import com.crsystem.common.enums.Role;

import java.io.Serializable;

/**
 * 클라이언트 요청에 대한 응답 또는 성공 여부 | 서버 -> 클라이언트 
 * @author wonsik
 */
public class ResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String message;    // 텍스트 메시지 ("가입 성공", "비밀번호 틀림" 등)
    private String result;
    private Object payload;  // 클라이언트에게 줄 데이터 (예: UserDto.Response 객체)
    private boolean isSuccess;
    
    public ResponseDTO(){}
    
    // 단순 메시지 전송
    public ResponseDTO(String message) {
        this.message = message;
    }

    // 메시지 + 실제 데이터
    public ResponseDTO(boolean isSuccess, String message, Object payload) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.payload = payload;
    }
    
    // Getter
    public String getMessage() {
        return message;
    }
    
    public String getResult(){
        return result;
    }
        
    public Object getPayload() {
        return payload;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    // Setter 
    public void setMessage(String message) {
        this.message = message;
    }

    public void setResult(String result) {
        this.result = result;
    }
    
    public void setPayload(Object Payload) {
        this.payload = payload;
    }
    
    public void setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    
}
