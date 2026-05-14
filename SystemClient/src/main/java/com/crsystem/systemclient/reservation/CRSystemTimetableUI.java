// 시간표형태 UI 제공
package com.crsystem.systemclient.reservation;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CRSystemTimetableUI extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private String[] days = {"월", "화", "수", "목", "금"};
    private List<Point> selectedCells = new ArrayList<>();

    public CRSystemTimetableUI() {
        setTitle("강의실 예약 시스템 - 시간표");
        setSize(900, 500);
        setLayout(new BorderLayout());

        String[] columnNames = {"교시/시간", "월", "화", "수", "목", "금"};
        model = new DefaultTableModel(columnNames, 9) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String[] times = {"1교시(09:00)", "2교시(10:00)", "3교시(11:00)", "4교시(12:00)",
            "5교시(13:00)", "6교시(14:00)", "7교시(15:00)", "8교시(16:00)", "9교시(17:00)"};
        for (int i = 0; i < 9; i++) {
            model.setValueAt(times[i], i, 0);
        }

        table = new JTable(model);
        table.setRowHeight(45);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();
                if (col == 0) {
                    return;
                }

                Point p = new Point(row, col);
                if (selectedCells.contains(p)) {
                    selectedCells.remove(p);
                } else {
                    selectedCells.add(p);
                }
                table.repaint();
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Color.WHITE);
                for (Point p : selectedCells) {
                    if (p.x == row && p.y == column) {
                        c.setBackground(new Color(180, 200, 255));
                    }
                }
                return c;
            }
        });

        JButton btnReserve = new JButton("선택한 교시 예약 신청하기");
        btnReserve.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btnReserve.addActionListener(e -> {
            if (selectedCells.isEmpty()) {
                JOptionPane.showMessageDialog(this, "예약할 교시를 선택해주세요.");
                return;
            }

            List<Integer> rows = new ArrayList<>();
            int col = selectedCells.get(0).y;
            for (Point p : selectedCells) {
                if (p.y != col) {
                    JOptionPane.showMessageDialog(this, "같은 요일 내에서만 선택 가능합니다.");
                    return;
                }
                rows.add(p.x);
            }
            Collections.sort(rows);

            for (int i = 0; i < rows.size() - 1; i++) {
                if (rows.get(i + 1) != rows.get(i) + 1) {
                    JOptionPane.showMessageDialog(this, "연속된 교시만 예약 가능합니다.");
                    return;
                }
            }

            // [수정 포인트] "N교시, M교시" 형태의 문자열 생성
            String periodString = rows.stream()
                    .map(r -> (r + 1) + "교시")
                    .collect(Collectors.joining(", "));

            new CRSystemReservation(days[col - 1], periodString, rows.size()).setVisible(true);
            selectedCells.clear();
            table.repaint();
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnReserve, BorderLayout.SOUTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
