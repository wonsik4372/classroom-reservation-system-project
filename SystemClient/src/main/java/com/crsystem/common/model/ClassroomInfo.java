/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.common.model;

/**
 * 강의실 정보 관련 데이터
 *
 * @author sunho
 */
public class ClassroomInfo {
    private int deptNo = 0;             // 건물 번호 ex) 23. 정보공학관 에서 23만 추출 
    private int floor = 0;              // 층 
    private int roomNo = 0;             // 강의실 번호 
    private int capacity = 0;           // 최대 수용 인원
    private String features = "";       // 특이사항 (예: 인터넷 불가, 빔 불가)
    private int computerCount = 0;      // 사용 가능 컴퓨터 개수
    private boolean isUsable = true;      // 사용 가능 여부 => 사용 가능 true

    // 기본 생성자
    
    public ClassroomInfo() {
    }
    
    // 강의실 추가 
    public ClassroomInfo(int deptNO, int floor, int roomNO, int capacity, String features, int computerCount, boolean isUsable) {
        this.deptNo = deptNo;
        this.floor = floor;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.features = features;
        this.computerCount = computerCount;
        this.isUsable = isUsable;
    }
    
    // 강의실 정보 파일 불러오기 - 건물, 강의실 추가 
    public ClassroomInfo(int deptNO, int floor, int roomNO, int capacity) {
        this.deptNo = deptNo;
        this.floor = floor;
        this.roomNo = roomNo;
        this.capacity = capacity;
    }
    
    // Getter

    public int getDeptNo() {
        return deptNo;
    }

    public int getFloor() {
        return floor;
    }

    public int getRoomNo() {
        return roomNo;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getFeatures() {
        return features;
    }

    public int getComputerCount() {
        return computerCount;
    }

    public boolean isUsable() {
        return isUsable;
    }

    // Setter 
    public void setDeptNo(int deptNo) {
        this.deptNo = deptNo;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public void setRoomNo(int roomNo) {
        this.roomNo = roomNo;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public void setComputerCount(int computerCount) {
        this.computerCount = computerCount;
    }

    public void setIsUsable(boolean isUsable) {
        this.isUsable = isUsable;
    }
    
    
    
}
