package cse.se.CRS.common;

public class ClassroomInfo {
    private String status = "사용 가능"; // 기본값
    private int capacity = 0;           // 최대 수용 인원
    private String features = "";       // 특이사항 (예: 인터넷 불가, 빔 불가)
    private int computerCount = 0;      // 사용 가능 컴퓨터 개수

    // 기본 생성자
    public ClassroomInfo() {}

    // Getter & Setter
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public int getComputerCount() { return computerCount; }
    public void setComputerCount(int computerCount) { this.computerCount = computerCount; }
}