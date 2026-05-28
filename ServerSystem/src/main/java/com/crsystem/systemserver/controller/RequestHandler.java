/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.crsystem.systemserver.controller;

/**
 *
 * @author wonsik
 */
import java.io.ObjectOutputStream;

public interface RequestHandler {
    void process(Object request, ObjectOutputStream out);
}
