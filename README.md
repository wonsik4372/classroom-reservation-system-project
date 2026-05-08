# Classroom Reservation System (CRSystem)
Java Swing과 TCP 소켓 통신을 이용한 강의실 예약 및 관리 시스템

## 프로젝트 개요
* 개발 기간: 2026.05 ~ 진행 중
* 기술 스택:
    * Language: Java
    * UI Framwork: Java Swing
    * Build Tool: Maven
    * Protocol: TCP/IP Socket
* IDE: NetBeans
* 주요 기능:
    * 서버-클라이언트 소켓 통신을 통한 실시간 데이터 송수신
    * 강의실 목록 조회 및 예약 신청/취소
    * 관리자 승인
    * 사용자 로그인
    * 
    *
    
## 시스템 아키텍처
* MVC(Model-View-Controller)패턴 기반
* 서버와 클라이언트 모듈 분리

### 구조
```
    com.crsystem
├── server (SystemServer)
│   ├── main 
│   ├── model 
│   ├── service
│   └── controller
└── client (SystemClient)
    ├── main    
    ├── model
    ├── service
    ├── controller
    └── view
```

#### server
```
main: start server, create server socket, create and management thread

controller: object parsing, routing for service

service: Business Role

model: DTO, DAO
```

#### client
```
main: start GUI, socket connect to server IP/Port

view: Swing UI

controller: handling event in view, routing for service

service: Business Role, command to server, response processing

model: Temp DTO
```

