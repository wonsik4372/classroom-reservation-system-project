/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.crsystem.systemserver.controller;

import java.util.List;
/**
 *
 * @author wonsik
 */
public interface FileManager<T> {
    // 모든 데이터 불러오기
    List<T> loadAll();
    
    // 전체 데이터 덮어쓰기 (저장)
    void saveAll(List<T> list);
    
    // 단일 객체 추가 (또는 수정)
    void add(T item);
    
    // 고유 ID로 삭제
    void delete(String id);
}
