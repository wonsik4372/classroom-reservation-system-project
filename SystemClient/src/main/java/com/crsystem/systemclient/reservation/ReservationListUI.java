// 예약 목록 표시 UI
package com.crsystem.systemclient.reservation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ReservationListUI extends JFrame {

    private JTable table;
    private DefaultTableModel model;

    public ReservationListUI() {
        setTitle("예약 내역 관리");
        setSize(950, 400);
        setLayout(new BorderLayout());

        String[] columns = {"날짜", "요일", "예약 교시", "구분", "상태", "거절사유"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);

        refreshTable();

        JPanel pnlButtons = new JPanel();
        JButton btnApprove = new JButton("승인");
        JButton btnReject = new JButton("거절");
        pnlButtons.add(btnApprove);
        pnlButtons.add(btnReject);

        btnApprove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                CRSystemReservation.reservationList.get(row).status = "예약 확정";
                refreshTable();
            }
        });

        btnReject.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String reason = JOptionPane.showInputDialog(this, "거절 사유를 입력하세요:");
                if (reason != null) {
                    ReservationInfo res = CRSystemReservation.reservationList.get(row);
                    res.status = "예약 거절";
                    res.rejectReason = reason;
                    refreshTable();
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(pnlButtons, BorderLayout.SOUTH);
        setLocationRelativeTo(null);
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (ReservationInfo res : CRSystemReservation.reservationList) {
            model.addRow(new Object[]{
                res.date, res.day, res.periodInfo, res.userType, res.status, res.rejectReason
            });
        }
    }
}
