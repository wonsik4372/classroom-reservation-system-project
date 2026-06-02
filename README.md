# Classroom Reservation System (CRSystem)

Java Swing과 TCP 소켓 통신을 이용한 강의실 예약 및 관리 시스템

## 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 개발 기간 | 2026.05 ~ 진행 중 |
| 언어 | Java 17 |
| UI 프레임워크 | Java Swing |
| 빌드 도구 | Maven (멀티 모듈) |
| 통신 프로토콜 | TCP/IP Socket |
| 데이터 저장 | JSON 파일 (Gson) |
| 테스트 프레임워크 | JUnit 5 + JaCoCo |
| 개발 환경 | NetBeans IDE |

## 주요 기능

| 기능 | 대상 |
|---|---|
| 로그인 / 로그아웃 (권한별 화면 분기) | 관리자, 조교, 교수, 학생 |
| 사용자 추가 / 삭제 | 관리자 |
| 강의실 현황 조회 (일별·주별·월별) | 조교, 교수, 학생 |
| 강의실 정보 등록·수정·삭제 및 시간표 PDF 업로드 | 조교 |
| 강의실 예약 신청 (교수·학생 권한별 제약 적용) | 교수, 학생 |
| 예약 승인 / 거부 (사유 포함) | 조교 |
| 본인 예약 취소 | 교수, 학생 |
| 실시간 알림 (로그인 중 푸시 / 오프라인 시 다음 로그인에 전달) | 학생 |
| JSON 파일 영속성 (재시작 후 데이터 유지) | 서버 |

---

## 시스템 아키텍처

MVC 패턴 기반의 서버-클라이언트 분리 구조로, 세 개의 Maven 모듈로 구성됩니다.

```
CRSystem (멀티 모듈 루트)
├── CommonModule       공유 DTO / Enum
├── ServerSystem       TCP 서버 + 비즈니스 로직
└── SystemClient       Swing GUI 클라이언트
```

### 통신 흐름

```
[SystemClient]                      [ServerSystem]
  View (Swing GUI)
    │  이벤트 발생
    ▼
  Controller                 TCP 소켓 (직렬화 ObjectStream)
  (SessionManager,    ──────────────────────────────────►  ClientHandler (스레드)
   ReservationController,                                       │
   NotificationController,                                      ▼
   UserController)          ◄──────────────────────────  MainController (라우터)
    │  ResponseDTO 수신                                          │
    ▼                                                     Service 레이어
  View 갱신                                              (LoginService, UserService,
                                                          ReservationService,
                                                          NotificationService)
                                                                │
                                                          Model / DAO 레이어
                                                         (UserCatalog, ReservationCatalog,
                                                          NotificationStore,
                                                          UserFileManager, ReservationFileManager)
                                                                │
                                                          JSON 파일 (masterfile/)
```

---

## 디렉토리 구조

```
classroom-reservation-system-project/
│
├── pom.xml                          멀티 모듈 루트 POM
│
├── CommonModule/                    서버·클라이언트 공유 모듈
│   └── src/main/java/com/crsystem/common/
│       ├── dto/
│       │   ├── UserDTO.java         로그인 요청/응답 + 미읽음 알림 포함
│       │   ├── ReservationDTO.java  예약 요청/응답 (Status: PENDING·APPROVED·REJECTED)
│       │   ├── NotificationDTO.java 승인·거부 알림 (Type: APPROVED·REJECTED)
│       │   ├── RequestDTO.java      클라이언트→서버 공통 요청 봉투
│       │   ├── ResponseDTO.java     서버→클라이언트 공통 응답 봉투
│       │   ├── ClassroomInfo.java   강의실 정보
│       │   ├── ScheduleData.java    시간표 데이터
│       │   ├── Classroom.java
│       │   ├── DayReservation.java
│       │   └── Reservation.java
│       └── enums/
│           └── Role.java            ADMIN · ASSISTANT · PROFESSOR · STUDENT
│
├── ServerSystem/                    TCP 서버 모듈
│   └── src/
│       ├── main/
│       │   ├── java/com/crsystem/systemserver/
│       │   │   ├── main/
│       │   │   │   ├── CRSystemServer.java      서버 진입점, ServerSocket 생성 및 스레드 관리
│       │   │   │   └── ClientHandler.java       클라이언트 연결당 스레드, ObjectStream 입출력
│       │   │   ├── controller/
│       │   │   │   ├── MainController.java      Command 패턴 라우터 (commandMap)
│       │   │   │   └── FileManager.java         파일 DAO 공통 인터페이스
│       │   │   ├── service/
│       │   │   │   ├── LoginService.java        인증, 조교→교수 화면 허용, 미읽음 알림 전달
│       │   │   │   ├── UserService.java         사용자 추가·삭제·조회, ADMIN 삭제 보호
│       │   │   │   ├── ReservationService.java  예약 등록·승인·거부·취소, 교시 제한, 교수 덮어쓰기
│       │   │   │   ├── NotificationService.java 알림 생성(승인·거부) 및 미읽음 조회·읽음처리
│       │   │   │   └── ClassroomService.java    (구현 예정)
│       │   │   ├── model/
│       │   │   │   ├── User.java                사용자 도메인 객체 (비밀번호 검증 포함)
│       │   │   │   ├── UserCatalog.java         사용자 목록 (synchronized)
│       │   │   │   ├── ReservationCatalog.java  예약 목록 (CopyOnWriteArrayList)
│       │   │   │   └── NotificationStore.java   사용자별 미읽음 알림 인메모리 저장소 (ConcurrentHashMap)
│       │   │   └── dao/
│       │   │       ├── UserFileManager.java      User.json 읽기·쓰기·추가·삭제
│       │   │       ├── ReservationFileManager.java Reservation.json 읽기·쓰기 (LocalDate 직렬화)
│       │   │       └── ServerPaths.java          JSON 파일 경로 상수
│       │   └── resources/
│       │       ├── application.properties        서버 포트 설정 (기본: 9998)
│       │       └── masterfile/
│       │           ├── User.json                사용자 데이터
│       │           └── Reservation.json         예약 데이터
│       └── test/
│           └── java/com/crsystem/systemserver/
│               ├── dao/
│               │   └── UserFileManagerTest.java      파일 CRUD 및 영속성 검증 (6개)
│               ├── model/
│               │   └── NotificationStoreTest.java    알림 저장·조회·읽음처리 검증 (10개)
│               └── service/
│                   ├── BaseUserFileTest.java          User.json 백업/복원 공통 픽스처
│                   ├── LoginServiceTest.java          로그인 흐름·권한 검증·미읽음 알림 (7개)
│                   ├── UserServiceTest.java           사용자 추가·삭제·조회 검증 (9개)
│                   └── NotificationServiceTest.java  승인·거부 알림 생성·조회 검증 (10개)
│
└── SystemClient/                    Swing GUI 클라이언트 모듈
    └── src/main/java/com/crsystem/systemclient/
        ├── main/
        │   └── CRSystemClient.java  클라이언트 진입점, 서버 소켓 연결 관리
        ├── controller/
        │   ├── SessionManager.java       로그인 세션 상태 관리 (싱글톤)
        │   ├── UserController.java       사용자 추가·삭제 서버 요청
        │   ├── ReservationController.java 예약 등록·조회·승인·거부·취소 서버 요청
        │   └── NotificationController.java 알림 폴링 및 수신 처리
        └── view/
            ├── LoginGUI.java             로그인 화면
            ├── Admin/
            │   ├── AdminGUI.java         관리자 메인 화면
            │   └── AddUserGUI.java       사용자 추가 화면
            ├── Assistant/
            │   ├── AssMainGUI.java       조교 메인 화면
            │   ├── AssistantGUI.java     조교 예약 관리 화면
            │   └── AssistantGUIveta.java (개발 중)
            └── Reservation/
                ├── RoomListGUI.java          강의실 목록 화면
                ├── TimeTableGUI.java         시간표 조회 화면
                ├── ReservationRegisterGUI.java 예약 신청 화면
                └── ReservationListUI.java     예약 목록 화면
```

---

## 비즈니스 규칙 (핵심 제약)

| 규칙 | 내용 |
|---|---|
| 교수 예약 시간 제한 | 하루 최대 3교시 |
| 학생 예약 시간 제한 | 하루 최대 2교시 |
| 학생 예약 인원 제한 | 강의실 수용인원의 1/2 이하 |
| 학생 예약 기간 제한 | 최소 하루 전 ~ 최대 14일 이내 |
| 교수 보강 우선권 | 교수 예약 등록 시 동일 슬롯의 학생 예약 자동 REJECTED |
| 조교 권한 | 조교 계정으로 교수 화면 접근 가능 |
| 관리자 계정 보호 | ADMIN 계정은 삭제 불가 |
| 예약 상태 전이 | PENDING → APPROVED 또는 REJECTED (PENDING 상태만 처리 가능) |
| 예약 취소 | 본인의 예약만 취소 가능, REJECTED 상태는 취소 불가 |
| 알림 전달 | 로그인 중: 폴링으로 즉시 전달 / 오프라인: 다음 로그인 시 일괄 전달 |

---

## 기본 계정 (User.json)

| 권한 | ID | 초기 비밀번호 |
|---|---|---|
| 관리자 (ADMIN) | 12345 | 12345 |
| 조교 (ASSISTANT) | 23456 | 23456 |
| 교수 (PROFESSOR) | 34567 | 34567 |
| 학생 (STUDENT) | 12341234 | 12341234 |

> 초기 비밀번호는 ID와 동일합니다. 신규 추가 사용자도 동일 규칙이 적용됩니다.

---

## 실행 방법

### 사전 조건
- JRE 17 이상
- 서버와 클라이언트가 동일 네트워크에 있어야 합니다.

### 서버 실행

```bash
# ServerSystem 디렉토리에서
cd ServerSystem
mvn package -DskipTests
java -jar target/SystemServer-1.0-SNAPSHOT.jar
```

서버 포트는 `src/main/resources/application.properties`에서 변경할 수 있습니다.

```properties
server.port=9998
```

### 클라이언트 실행

```bash
cd SystemClient
mvn package -DskipTests
java -jar target/SystemClient-1.0-SNAPSHOT.jar
```

---

## 테스트 실행

```bash
# ServerSystem 디렉토리에서
cd ServerSystem
mvn test
```

### 현재 테스트 현황 (총 42개)

| 테스트 파일 | 테스트 수 | 커버 영역 |
|---|---|---|
| `UserFileManagerTest` | 6 | User.json CRUD, 파일 없을 때 빈 리스트 반환 |
| `NotificationStoreTest` | 10 | 알림 저장·조회·읽음처리, 사용자별 격리 |
| `LoginServiceTest` | 7 | 로그인 성공·실패, 권한 검증, 미읽음 알림 전달 |
| `UserServiceTest` | 9 | 사용자 추가·삭제·조회, ADMIN 삭제 방지, 중복 ID 방지 |
| `NotificationServiceTest` | 10 | 승인·거부 알림 생성, 미읽음 조회 후 읽음처리 |

JaCoCo 커버리지 리포트는 `target/site/jacoco/index.html`에서 확인할 수 있습니다.
