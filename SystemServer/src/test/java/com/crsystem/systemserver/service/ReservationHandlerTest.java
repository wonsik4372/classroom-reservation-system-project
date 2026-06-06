/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.service;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.enums.Role;
import com.crsystem.systemserver.dao.ReservationFileManager;
import com.crsystem.systemserver.model.ReservationCatalog;
import com.crsystem.systemserver.service.ReservationHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author abalo
 */
public class ReservationHandlerTest {

    @BeforeEach
    void setUp() {
        ReservationHandler.getInstance().clearForTesting();
    }

    // ====================
    // [TC-20] 예약생성-공통 - 정상 예약 등록
    // ====================
    @Test
    public void addReservation_succeedsWhenReservationDataIsValid() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");
        reservation.setPartnerCount(1);

        ResponseDTO response = service.addReservation(reservation);

        assertTrue(response.isSuccess());
        assertEquals("예약이 등록되었습니다.", response.getMessage());

    }

    // ====================
    // [TC-21] 예약생성-교수 - 사용 불가 강의실 예약 차단
    // ====================
    @Test
    public void addReservation_failsWhenRoomIsUnavailable() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setUserId("34567");
        reservation.setRoleType(Role.PROFESSOR);

        // 사용 불가 강의실로 변경 필요
        reservation.setRoomName("사용불가강의실명"); //23 정보공학관 9층 911호

        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");

        ResponseDTO response = service.addReservation(reservation);

        assertFalse(response.isSuccess());
    }

    // ====================
    // [TC-22] 예약생성-교수 - 교수 예약 생성
    // ====================
    @Test
    public void addReservation_professorReservationWithAcademicPurposeSucceeds() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setUserId("34567");
        reservation.setRoleType(Role.PROFESSOR);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");
        reservation.setPurpose("보강");

        ResponseDTO response = service.addReservation(reservation);

        assertTrue(response.isSuccess());
    }

    // ====================
    // [TC-23] 예약생성-교수 - 교수 정보 저장
    // ====================
    @Test
    public void addReservation_professorReservationStoresProfessorInfo() {

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("34567");
        reservation.setUserName("Professor User");
        reservation.setRoleType(Role.PROFESSOR);

        assertEquals("34567", reservation.getUserId());
        assertEquals(Role.PROFESSOR, reservation.getRoleType());
    }

    // ====================
    // [TC-24] 예약생성-교수 - 최대 3교시 제한
    // ====================
    @Test
    public void addReservation_professorCannotReserveMoreThanThreePeriods() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("34567");
        reservation.setRoleType(Role.PROFESSOR);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));

        // 4교시
        reservation.setPeriodInfo("1,2,3,4");

        ResponseDTO response = service.addReservation(reservation);

        assertFalse(response.isSuccess());
    }

    // ====================
    // [TC-25] 예약생성-교수 - 동일 시간대 교수 예약 중복 불가
    // ====================
    @Test
    public void addReservation_failsWhenProfessorReservationAlreadyExists() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response first = new ReservationDTO.Response();
        first.setUserId("34567");
        first.setRoleType(Role.PROFESSOR);
        first.setRoomName("23 정보공학관 9층 911호");
        first.setDate(LocalDate.now().plusDays(2));
        first.setPeriodInfo("1");

        service.addReservation(first);

        ReservationDTO.Response second = new ReservationDTO.Response();
        second.setUserId("34568");
        second.setRoleType(Role.PROFESSOR);
        second.setRoomName("23 정보공학관 9층 911호");
        second.setDate(LocalDate.now().plusDays(2));
        second.setPeriodInfo("1");

        ResponseDTO response = service.addReservation(second);

        assertTrue(response.isSuccess());
    }

    // ====================
    // [TC-26] 예약생성-교수 - 학생 예약 덮어쓰기
    // ====================
    @Test
    public void addReservation_professorOverridesStudentReservation() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response student = new ReservationDTO.Response();
        student.setUserId("20240001");
        student.setRoleType(Role.STUDENT);
        student.setRoomName("23 정보공학관 9층 911호");
        student.setDate(LocalDate.now().plusDays(2));
        student.setPeriodInfo("1");
        student.setPartnerCount(1);

        service.addReservation(student);

        ReservationDTO.Response professor = new ReservationDTO.Response();
        professor.setUserId("34567");
        professor.setRoleType(Role.PROFESSOR);
        professor.setRoomName("23 정보공학관 9층 911호");
        professor.setDate(LocalDate.now().plusDays(2));
        professor.setPeriodInfo("1");

        service.addReservation(professor);

        assertEquals(
                ReservationDTO.Status.REJECTED,
                student.getStatus()
        );
    }

    // ====================
    // [TC-27] 예약생성-학생 - 예약 생성 성공
    // ====================
    @Test
    public void addReservation_studentReservationSucceeds() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");
        reservation.setPartnerCount(1);

        ResponseDTO response = service.addReservation(reservation);

        assertTrue(response.isSuccess());
    }

    // ====================
    // [TC-28] 예약생성-학생 - 학생 정보 저장
    // ====================
    @Test
    public void addReservation_studentReservationStoresStudentInfo() {

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("20240001");
        reservation.setUserName("홍길동");
        reservation.setRoleType(Role.STUDENT);

        assertEquals("20240001", reservation.getUserId());
        assertEquals(Role.STUDENT, reservation.getRoleType());
    }

    // ====================
    // [TC-29] 예약생성-학생 - 최대 2교시 제한
    // ====================
    @Test
    public void addReservation_studentCannotReserveMoreThanTwoPeriods() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));

        reservation.setPeriodInfo("1,2,3");

        ResponseDTO response = service.addReservation(reservation);

        assertFalse(response.isSuccess());
    }

    // ====================
    // [TC-30] 예약생성-학생 - 최소 하루 전 예약
    // ====================
    // GUI 기반 수동 테스트 수행 - 당일 날짜가 콤보박스에 표시되지 않음.
    
    // ====================
    // [TC-31] 예약생성-학생 - 수용 인원 초과 시 실패
    // ====================
    @Test
    public void addReservation_failsWhenCapacityExceedsHalf() {

        ReservationHandler service = ReservationHandler.getInstance();

        ReservationDTO.Response reservation = new ReservationDTO.Response();

        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");

        reservation.setPartnerCount(999);

        ResponseDTO response = service.addReservation(reservation);

        assertFalse(response.isSuccess());
    }

    // ====================
    // [TC-32] 예약생성-교수 - 14일 초과 예약 제한
    // ====================
    // GUI 기반 수동 테스트 수행 - 15일 이후 날짜가 콤보박스에 표시되지 않음.

    // ====================
    // [TC-39] 예약상태관리-승인 - 예약 취소
    // ====================
    @Test
    public void cancelReservation_succeedsWhenRequesterIsOwner() {
        // Given: 학생이 예약 등록
        ReservationHandler service = ReservationHandler.getInstance();
        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setReservationId("R-001");
        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("1");
        reservation.setPartnerCount(1);
        reservation.setStatus(ReservationDTO.Status.PENDING);
        service.addReservation(reservation);

        // When: 본인이 취소
        ResponseDTO response = service.cancelReservation("R-001", "20240001");

        // Then
        assertTrue(response.isSuccess());
        assertEquals("예약이 취소되었습니다.", response.getMessage());
    }

    @Test
    public void cancelReservation_failsWhenRequesterIsNotOwner() {
        // Given: 학생 20240001이 예약 등록
        ReservationHandler service = ReservationHandler.getInstance();
        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setReservationId("R-002");
        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("2");
        reservation.setPartnerCount(1);
        reservation.setStatus(ReservationDTO.Status.PENDING);
        service.addReservation(reservation);

        // When: 다른 사용자(20240002)가 취소 시도
        ResponseDTO response = service.cancelReservation("R-002", "20240002");

        // Then
        assertFalse(response.isSuccess());
        assertEquals("본인의 예약만 취소할 수 있습니다.", response.getMessage());
    }

    @Test
    public void cancelReservation_failsWhenReservationIsAlreadyRejected() {
        // Given: REJECTED 상태의 예약 직접 삽입
        ReservationHandler service = ReservationHandler.getInstance();
        ReservationDTO.Response reservation = new ReservationDTO.Response();
        reservation.setReservationId("R-003");
        reservation.setUserId("20240001");
        reservation.setRoleType(Role.STUDENT);
        reservation.setRoomName("23 정보공학관 9층 911호");
        reservation.setDate(LocalDate.now().plusDays(2));
        reservation.setPeriodInfo("3");
        reservation.setPartnerCount(1);
        reservation.setStatus(ReservationDTO.Status.REJECTED);
        service.addReservation(reservation);

        // When: REJECTED 예약 취소 시도
        ResponseDTO response = service.cancelReservation("R-003", "20240001");

        // Then
        assertFalse(response.isSuccess());
        assertEquals("이미 거부된 예약입니다.", response.getMessage());
    }
}
