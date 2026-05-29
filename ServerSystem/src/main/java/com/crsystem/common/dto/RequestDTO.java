/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import java.io.Serializable;

/**
 *
 * @author wonsik
 */
public class RequestDTO implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String command;  // 작업이름 (예: "LOGIN")
    private Object payload;  // 데이터 (ex. UserDto.Request 객체)

    public RequestDTO(String command, Object payload) {
        this.command = command;
        this.payload = payload;
    }
    
    public String getCommand() { return command; }
    public Object getPayload() { return payload; }
    
}
