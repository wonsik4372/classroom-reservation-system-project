/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemclient.session;

import com.crsystem.common.dto.UserDTO;

public class SessionManager {

    private static UserDTO.Response currentUser;

    public static void setCurrentUser(UserDTO.Response user) {
        currentUser = user;
    }

    public static UserDTO.Response getCurrentUser() {
        return currentUser;
    }
}
