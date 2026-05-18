package com.crsystem.common.model;

import java.io.Serializable;

/**
 * 강의실 목록 및 시간표 조회를 위한 요청 패킷
 */
public class RoomListRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int deptNo;    // 건물 번호 (ex: 23)
    private int floor;     // 층 (ex: 9)
    private String date;   // 조회 날짜 (ex: "2026-05-19")

    public RoomListRequest(int deptNo, int floor, String date) {
        this.deptNo = deptNo;
        this.floor = floor;
        this.date = date;
    }

    public int getDeptNo() { return deptNo; }
    public int getFloor() { return floor; }
    public String getDate() { return date; }
}