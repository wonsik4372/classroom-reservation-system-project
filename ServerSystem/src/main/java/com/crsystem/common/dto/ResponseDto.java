/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.dto;

import com.crsystem.common.enums.Role;
/**
 *
 * @author wonsik
 */
public class ResponseDto {
    private Role role;
    private String msg;
    private boolean isSuccess;
    
    public ResponseDto(Role role) {
        this.role = role;
    }
    public ResponseDto(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    public ResponseDto(String msg) {
        this.msg = msg;
    }
    
}
