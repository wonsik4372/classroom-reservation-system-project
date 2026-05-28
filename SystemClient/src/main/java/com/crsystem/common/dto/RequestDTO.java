/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

/**
 *
 * @author wonsik
 */
public class RequestDTO {
    private String command;  // 목적지 (예: "LOGIN")
    private Object payload;  // 진짜 데이터 (예: UserDto.Request 객체)

    public RequestDTO(String command, Object payload) {
        this.command = command;
        this.payload = payload;
    }
    
    public String getCommand() { return command; }
    public Object getPayload() { return payload; }
    
}
