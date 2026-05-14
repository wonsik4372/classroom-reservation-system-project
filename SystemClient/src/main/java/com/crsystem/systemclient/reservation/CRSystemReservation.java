//예약 정보 입력 및 예약 UI
package com.crsystem.systemclient.reservation;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CRSystemReservation extends JFrame {

    public static List<ReservationInfo> reservationList = new ArrayList<>();

    private String day;
    private String periodInfo; //예약 시간(교시) 확인용
    private int duration;      

    public CRSystemReservation(String day, String periodInfo, int duration) {
        this.day = day;
        this.periodInfo = periodInfo;
        this.duration = duration;

        setTitle("예약 상세 정보 입력");
        setLayout(new GridLayout(9, 2, 10, 10));

        LocalDate today = LocalDate.now();
        List<String> dateLabels = new ArrayList<>();
        List<LocalDate> dateValues = new ArrayList<>();
        String[] dayNames = {"", "월", "화", "수", "목", "금", "토", "일"};

        for (int i = -14; i <= 14; i++) {
            LocalDate d = today.plusDays(i);
            dateLabels.add(d.toString() + " (" + dayNames[d.getDayOfWeek().getValue()] + ")");
            dateValues.add(d);
        }

        JComboBox<String> dateCombo = new JComboBox<>(dateLabels.toArray(new String[0]));
        dateCombo.setSelectedIndex(14);

        String[] types = {"학생", "교수"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        JTextField purposeField = new JTextField();
        JSpinner partnerSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));

        add(new JLabel(" 선택된 정보:"));
        add(new JLabel(day + "요일 / " + periodInfo));
        add(new JLabel(" 예약 날짜 선택:"));
        add(dateCombo);
        add(new JLabel(" 사용자 구분:"));
        add(typeCombo);
        add(new JLabel(" 사용 목적:"));
        add(purposeField);
        add(new JLabel(" 동반 인원 수:"));
        add(partnerSpinner);

        JButton btnSubmit = new JButton("최종 예약 신청");
        JButton btnList = new JButton("예약 목록 보기");

        btnSubmit.addActionListener(e -> {
            LocalDate selectedDate = dateValues.get(dateCombo.getSelectedIndex());
            String actualDay = dayNames[selectedDate.getDayOfWeek().getValue()];

            if (!actualDay.equals(this.day)) {
                JOptionPane.showMessageDialog(this, "선택한 날짜(" + actualDay + ")와 시간표 요일(" + this.day + ")이 일치하지 않습니다.");
                return;
            }

            String type = (String) typeCombo.getSelectedItem();
            if (type.equals("학생") && duration > 2) {
                JOptionPane.showMessageDialog(this, "학생은 최대 2교시까지만 가능합니다.");
                return;
            }
            if (type.equals("교수") && duration > 3) {
                JOptionPane.showMessageDialog(this, "교수는 최대 3교시까지만 가능합니다.");
                return;
            }

            ReservationInfo info = new ReservationInfo();
            info.date = selectedDate;
            info.day = this.day;
            info.periodInfo = this.periodInfo; //예약한 시간 표시
            info.userType = type;
            info.purpose = purposeField.getText();
            info.partnerCount = (int) partnerSpinner.getValue();
            info.status = type.equals("교수") ? "예약 확정" : "대기 중";

            reservationList.add(info);
            JOptionPane.showMessageDialog(this, "예약 성공! (" + info.status + ")");
            this.dispose();
        });

        btnList.addActionListener(e -> new ReservationListUI().setVisible(true));

        add(btnSubmit);
        add(btnList);
        setSize(450, 400);
        setLocationRelativeTo(null);
    }
}
