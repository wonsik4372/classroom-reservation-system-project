package com.crsystem.common.model;
/**
 * 강의실 정보 관련 데이터 모델
 */
public class ClassroomInfo {
    private int deptNo = 0;             // 건물 번호 (예: 23)
    private int floor = 0;              // 층
    private int roomNo = 0;             // 강의실 번호 (예: 911)
    private int capacity = 0;           // 최대 수용 인원
    private String features = "";       // 특이사항 (예: 인터넷 불가, 빔 불가)
    private int computerCount = 0;      // 사용 가능 컴퓨터 개수
    private String status = "사용 가능";  // 강의실 운영 상태 ("사용 가능" / "사용 불가")

    // 기본 생성자
    public ClassroomInfo() {}
    
    // 전체 필드 생성자
    public ClassroomInfo(int deptNo, int floor, int roomNo, int capacity, String features, int computerCount, String status) {
        this.deptNo = deptNo;
        this.floor = floor;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.features = features;
        this.computerCount = computerCount;
        this.status = status;
    }
    
    // 마스터 초기화용 간이 생성자
    public ClassroomInfo(int deptNo, int floor, int roomNo, int capacity) {
        this.deptNo = deptNo;
        this.floor = floor;
        this.roomNo = roomNo;
        this.capacity = capacity;
    }

    // Getter & Setter
    public int getDeptNo() { return deptNo; }
    public void setDeptNo(int deptNo) { this.deptNo = deptNo; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getRoomNo() { return roomNo; }
    public void setRoomNo(int roomNo) { this.roomNo = roomNo; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public int getComputerCount() { return computerCount; }
    public void setComputerCount(int computerCount) { this.computerCount = computerCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}