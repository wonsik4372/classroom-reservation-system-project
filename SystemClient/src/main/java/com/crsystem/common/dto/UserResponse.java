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
public class UserResponse {
    private Role role;
    
    public UserResponse(Role role) {
        this.role = role;
    }
}
