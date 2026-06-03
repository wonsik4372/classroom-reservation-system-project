/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.crsystem.systemclient.view.Reservation;

import com.crsystem.common.dto.ReservationDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.systemclient.controller.ReservationController;
import com.crsystem.systemclient.controller.SessionManager;
import com.crsystem.systemclient.controller.TimetableController;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wonsik
 */
public class UserGUI extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(UserGUI.class.getName());

    private java.util.List<ReservationDTO.Response> myReservationList = new java.util.ArrayList<>();
    private Map<String, Object> timetableData = null;

    /**
     * Creates new form UserGUI
     */
    public UserGUI() {
        initComponents();
        initCustomSettings();
        loadReservationsFromServer();
        loadTimetableFromServer();
    }

    private void initCustomSettings() {
        jLabelTime.setText(getCurrentTime());
        new javax.swing.Timer(1000, e -> jLabelTime.setText(getCurrentTime())).start();

        jButtonLogout.addActionListener(e -> handleLogout());

        jTableTimeTable.setAutoCreateRowSorter(true);
        jTable2.setAutoCreateRowSorter(true);

        String[] myResColumns = {"선택", "강의실", "날짜", "교시", "목적", "상태"};
        javax.swing.table.DefaultTableModel myResModel = new javax.swing.table.DefaultTableModel(myResColumns, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };
        jTableMyReservation.setModel(myResModel);
        jTableMyReservation.setAutoCreateRowSorter(true);
        jTableMyReservation.getColumnModel().getColumn(0).setMaxWidth(50);

        jButtonCancel.addActionListener(e -> handleCancelReservation());

        jTabbedPaneTop.addChangeListener(e -> {
            if (jTabbedPaneTop.getSelectedIndex() == 2) {
                updateMyReservationTable();
            }
        });

        jComboBoxYear.addActionListener(e -> loadTimetableFromServer());
        jComboBoxSemester.addActionListener(e -> updateBuildingComboBox());
        jComboBoxBuildingNo.addActionListener(e -> updateFloorComboBox());
        jComboBoxFloor.addActionListener(e -> updateClassroomComboBox());
        jComboBox4.addActionListener(e -> updateTimetableTable());
    }

    private String getCurrentTime() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss"));
    }

    @SuppressWarnings("unchecked")
    private void loadTimetableFromServer() {
        String year = (String) jComboBoxYear.getSelectedItem();
        TimetableController.getInstance().getTimetable(year,
            response -> {
                if (response.isSuccess() && response.getPayload() instanceof Map) {
                    timetableData = (Map<String, Object>) response.getPayload();
                    updateBuildingComboBox();
                } else {
                    logger.warning("시간표 로드 실패: " + response.getMessage());
                }
            },
            error -> logger.warning("시간표 통신 오류: " + error)
        );
    }

    @SuppressWarnings("unchecked")
    private void updateBuildingComboBox() {
        if (timetableData == null) return;

        String semester = (String) jComboBoxSemester.getSelectedItem();
        Map<String, Object> semesterMap = (Map<String, Object>) timetableData.get(semester);

        javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
        model.addElement("건물선택");
        if (semesterMap != null) {
            for (String building : semesterMap.keySet()) {
                model.addElement(building);
            }
        }
        jComboBoxBuildingNo.setModel(model);
        updateFloorComboBox();
    }

    @SuppressWarnings("unchecked")
    private void updateFloorComboBox() {
        if (timetableData == null) return;

        String semester = (String) jComboBoxSemester.getSelectedItem();
        String building = (String) jComboBoxBuildingNo.getSelectedItem();

        javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
        model.addElement("층 선택");

        if (!"건물선택".equals(building)) {
            try {
                Map<String, Object> semesterMap = (Map<String, Object>) timetableData.get(semester);
                if (semesterMap != null) {
                    Map<String, Object> buildingMap = (Map<String, Object>) semesterMap.get(building);
                    if (buildingMap != null) {
                        for (String floor : buildingMap.keySet()) {
                            // JSON 키는 "9층" 형태이지만 콤보박스에는 숫자만 저장
                            // (btnGoToReserveActionPerformed 등이 floor + "층" 으로 조합하므로)
                            model.addElement(floor.replace("층", "").trim());
                        }
                    }
                }
            } catch (ClassCastException ignored) {}
        }
        jComboBoxFloor.setModel(model);
        updateClassroomComboBox();
    }

    @SuppressWarnings("unchecked")
    private void updateClassroomComboBox() {
        if (timetableData == null) return;

        String semester = (String) jComboBoxSemester.getSelectedItem();
        String building = (String) jComboBoxBuildingNo.getSelectedItem();
        String floor    = (String) jComboBoxFloor.getSelectedItem();

        javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
        model.addElement("강의실 선택");

        if (!"건물선택".equals(building) && !"층 선택".equals(floor)) {
            try {
                Map<String, Object> semesterMap = (Map<String, Object>) timetableData.get(semester);
                if (semesterMap != null) {
                    Map<String, Object> buildingMap = (Map<String, Object>) semesterMap.get(building);
                    if (buildingMap != null) {
                        // 콤보박스에는 숫자만 저장되므로 JSON 키 조회 시 "층" 재부착
                        Map<String, Object> floorMap = (Map<String, Object>) buildingMap.get(floor + "층");
                        if (floorMap != null) {
                            for (String room : floorMap.keySet()) {
                                // 호수만 추출 (예: "911호" -> "911")
                                model.addElement(room.replace("호", ""));
                            }
                        }
                    }
                }
            } catch (ClassCastException ignored) {}
        }
        jComboBox4.setModel(model);
        updateTimetableTable();
    }

    @SuppressWarnings("unchecked")
    private void updateTimetableTable() {
        if (timetableData == null) return;

        String semester = (String) jComboBoxSemester.getSelectedItem();
        String building = (String) jComboBoxBuildingNo.getSelectedItem();
        String floor    = (String) jComboBoxFloor.getSelectedItem();
        String room     = (String) jComboBox4.getSelectedItem();

        String[] columns = {"교시 / 시간", "월", "화", "수", "목", "금"};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(columns, 0) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };

        if ("건물선택".equals(building) || "층 선택".equals(floor) || "강의실 선택".equals(room)) {
            jTableTimeTable.setModel(model);
            return;
        }

        String roomKey = room + "호";
        try {
            Map<String, Object> semesterMap = (Map<String, Object>) timetableData.get(semester);
            if (semesterMap == null) { jTableTimeTable.setModel(model); return; }
            Map<String, Object> buildingMap = (Map<String, Object>) semesterMap.get(building);
            if (buildingMap == null) { jTableTimeTable.setModel(model); return; }
            Map<String, Object> floorMap = (Map<String, Object>) buildingMap.get(floor + "층");
            if (floorMap == null) { jTableTimeTable.setModel(model); return; }
            Map<String, Object> roomData = (Map<String, Object>) floorMap.get(roomKey);
            if (roomData == null) { jTableTimeTable.setModel(model); return; }

            Map<String, java.util.List<Map<String, Object>>> schedule =
                    (Map<String, java.util.List<Map<String, Object>>>) roomData.get("schedule");
            if (schedule == null) { jTableTimeTable.setModel(model); return; }

            String[] days = {"월", "화", "수", "목", "금"};
            String[] periodLabels = {
                "1교시(09:00-09:50)", "2교시(10:00-10:50)", "3교시(11:00-11:50)",
                "4교시(12:00-12:50)", "5교시(13:00-13:50)", "6교시(14:00-14:50)",
                "7교시(15:00-15:50)", "8교시(16:00-16:50)", "9교시(17:00-17:50)"
            };

            for (int i = 0; i < 9; i++) {
                Object[] row = new Object[6];
                row[0] = periodLabels[i];
                for (int d = 0; d < days.length; d++) {
                    java.util.List<Map<String, Object>> daySlots = schedule.get(days[d]);
                    if (daySlots != null && i < daySlots.size()) {
                        Map<String, Object> slot = daySlots.get(i);
                        String subject = (String) slot.get("subject");
                        String professor = (String) slot.get("professor");
                        int status = slot.get("status") instanceof Integer ? (int) slot.get("status") : 0;
                        if (status == 0) {
                            row[d + 1] = "";
                        } else {
                            row[d + 1] = subject + (professor != null && !"없음".equals(professor) ? "\n(" + professor + ")" : "");
                        }
                    } else {
                        row[d + 1] = "";
                    }
                }
                model.addRow(row);
            }
        } catch (ClassCastException e) {
            logger.warning("시간표 데이터 파싱 오류: " + e.getMessage());
        }

        jTableTimeTable.setModel(model);
        jTableTimeTable.setRowHeight(40);
    }

    private void handleLogout() {
        SessionManager.getInstance().logout();
        this.dispose();
        javax.swing.SwingUtilities.invokeLater(() ->
                new com.crsystem.systemclient.view.LoginGUI().setVisible(true));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanelTitle = new javax.swing.JPanel();
        jLabelClassRoomListTitle = new javax.swing.JLabel();
        jPanelCondition = new javax.swing.JPanel();
        jLabelYear = new javax.swing.JLabel();
        jLabelBuildingNo = new javax.swing.JLabel();
        jLabelSemester = new javax.swing.JLabel();
        jLabelClassRoom = new javax.swing.JLabel();
        jComboBoxYear = new javax.swing.JComboBox<>();
        jComboBoxSemester = new javax.swing.JComboBox<>();
        jComboBoxBuildingNo = new javax.swing.JComboBox<>();
        jComboBox4 = new javax.swing.JComboBox<>();
        jLabelFloor = new javax.swing.JLabel();
        jComboBoxFloor = new javax.swing.JComboBox<>();
        jLabelTime = new javax.swing.JLabel();
        jButtonLogout = new javax.swing.JButton();
        jTabbedPaneTop = new javax.swing.JTabbedPane();
        jScrollPaneScheduleList = new javax.swing.JScrollPane();
        jTableTimeTable = new javax.swing.JTable();
        jScrollPaneReservationList = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanelMyReservation = new javax.swing.JPanel();
        jScrollPaneMyReservation = new javax.swing.JScrollPane();
        jTableMyReservation = new javax.swing.JTable();
        jButtonCancel = new javax.swing.JButton();
        btnGoToReserve = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setPreferredSize(new java.awt.Dimension(850, 500));

        jLabelClassRoomListTitle.setFont(new java.awt.Font("Helvetica Neue", 0, 24)); // NOI18N
        jLabelClassRoomListTitle.setText("Class-Room List");

        javax.swing.GroupLayout jPanelTitleLayout = new javax.swing.GroupLayout(jPanelTitle);
        jPanelTitle.setLayout(jPanelTitleLayout);
        jPanelTitleLayout.setHorizontalGroup(
            jPanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTitleLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelClassRoomListTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelTitleLayout.setVerticalGroup(
            jPanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTitleLayout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addComponent(jLabelClassRoomListTitle)
                .addContainerGap(34, Short.MAX_VALUE))
        );

        jLabelYear.setText("년도");

        jLabelBuildingNo.setText("건물");

        jLabelSemester.setText("학기");

        jLabelClassRoom.setText("강의실");

        jComboBoxYear.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2026", "2025", "2024", "2023" }));

        jComboBoxSemester.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1학기", "2학기" }));

        jComboBoxBuildingNo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "건물선택", "23 정보공학관", "20 산학협력관" }));

        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "강의실 선택", "911", "912", "913", "914", "915", "916", "918" }));
        jComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox4ActionPerformed(evt);
            }
        });

        jLabelFloor.setText("층");

        jComboBoxFloor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "층 선택", "1", "2", "3", "4", "5", "6", "7", "8", "9" }));

        javax.swing.GroupLayout jPanelConditionLayout = new javax.swing.GroupLayout(jPanelCondition);
        jPanelCondition.setLayout(jPanelConditionLayout);
        jPanelConditionLayout.setHorizontalGroup(
            jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConditionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelConditionLayout.createSequentialGroup()
                        .addComponent(jLabelYear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBoxYear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(286, 286, 286)
                        .addComponent(jLabelSemester))
                    .addGroup(jPanelConditionLayout.createSequentialGroup()
                        .addComponent(jLabelBuildingNo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBoxBuildingNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(91, 91, 91)
                        .addComponent(jLabelFloor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBoxFloor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabelClassRoom)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jComboBoxSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelConditionLayout.setVerticalGroup(
            jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConditionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelYear)
                    .addComponent(jLabelSemester)
                    .addComponent(jComboBoxYear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelConditionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelClassRoom)
                        .addComponent(jComboBoxBuildingNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelFloor)
                        .addComponent(jComboBoxFloor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabelBuildingNo))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabelTime.setText("Current Time");

        jButtonLogout.setText("LogOut");

        jTableTimeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPaneScheduleList.setViewportView(jTableTimeTable);

        jTabbedPaneTop.addTab("강의실 시간표", jScrollPaneScheduleList);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPaneReservationList.setViewportView(jTable2);

        jTabbedPaneTop.addTab("예약 현황", jScrollPaneReservationList);

        jTableMyReservation.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPaneMyReservation.setViewportView(jTableMyReservation);

        jButtonCancel.setText("예약 취소");

        javax.swing.GroupLayout jPanelMyReservationLayout = new javax.swing.GroupLayout(jPanelMyReservation);
        jPanelMyReservation.setLayout(jPanelMyReservationLayout);
        jPanelMyReservationLayout.setHorizontalGroup(
            jPanelMyReservationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMyReservationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMyReservationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneMyReservation, javax.swing.GroupLayout.DEFAULT_SIZE, 826, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelMyReservationLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonCancel)))
                .addContainerGap())
        );
        jPanelMyReservationLayout.setVerticalGroup(
            jPanelMyReservationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMyReservationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneMyReservation, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCancel)
                .addContainerGap())
        );

        jTabbedPaneTop.addTab("내 예약", jPanelMyReservation);

        btnGoToReserve.setText("선택한 강의실 예약하기");
        btnGoToReserve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGoToReserveActionPerformed(evt);
            }
        });

        btnRefresh.setText("새로고침");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelTitle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneTop)
                    .addComponent(jPanelCondition, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnGoToReserve, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(178, 178, 178)
                        .addComponent(jLabelTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRefresh)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonLogout)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanelCondition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPaneTop, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelTime)
                    .addComponent(jButtonLogout)
                    .addComponent(btnGoToReserve)
                    .addComponent(btnRefresh))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 850, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox4ActionPerformed
        updateReservationTable();
    }//GEN-LAST:event_jComboBox4ActionPerformed

    private void btnGoToReserveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoToReserveActionPerformed
        String building = (String) jComboBoxBuildingNo.getSelectedItem();
        String floor = (String) jComboBoxFloor.getSelectedItem();
        String room = (String) jComboBox4.getSelectedItem();

        if ("건물선택".equals(building) || "층 선택".equals(floor) || "강의실 선택".equals(room)) {
            javax.swing.JOptionPane.showMessageDialog(this, "건물, 층, 강의실을 모두 선택해주세요.");
            return;
        }

        String roomName = building + " " + floor + "층 " + room + "호";

        javax.swing.JFrame timetableFrame = new javax.swing.JFrame(roomName + " - 시간표 예약");
        timetableFrame.setContentPane(new TimeTableGUI(roomName));
        timetableFrame.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        timetableFrame.pack();
        timetableFrame.setLocationRelativeTo(this);
        timetableFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                updateReservationTable();
            }
        });
        timetableFrame.setVisible(true);
    }//GEN-LAST:event_btnGoToReserveActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        loadReservationsFromServer();
    }//GEN-LAST:event_btnRefreshActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        java.awt.EventQueue.invokeLater(() -> new UserGUI().setVisible(true));
    }

    private void loadReservationsFromServer() {
        ReservationController.getInstance().getReservationList(
            "ALL",
            (ResponseDTO response) -> {
                if (response.isSuccess()) {
                    ReservationController.getInstance().updateCache(response.getPayload());
                    updateReservationTable();
                    updateMyReservationTable();
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "예약 현황 조회 실패: " + response.getMessage());
                }
            },
            error -> javax.swing.JOptionPane.showMessageDialog(this, "통신 오류: " + error)
        );
    }

    private void updateMyReservationTable() {
        String myId = SessionManager.getInstance().getCurrentUser().getId();
        List<ReservationDTO.Response> cache = ReservationController.getInstance().getReservationCache();

        javax.swing.table.DefaultTableModel model =
                (javax.swing.table.DefaultTableModel) jTableMyReservation.getModel();
        model.setRowCount(0);
        myReservationList.clear();

        if (cache == null) return;

        for (ReservationDTO.Response res : cache) {
            if (myId.equals(res.getUserId())) {
                model.addRow(new Object[]{
                    false,
                    res.getRoomName(),
                    res.getDate() != null ? res.getDate().toString() : "",
                    res.getPeriodInfo(),
                    res.getPurpose(),
                    res.getStatus() != null ? res.getStatus().name() : ""
                });
                myReservationList.add(res);
            }
        }
        jTableMyReservation.setRowHeight(25);
    }

    private void handleCancelReservation() {
        javax.swing.table.DefaultTableModel model =
                (javax.swing.table.DefaultTableModel) jTableMyReservation.getModel();

        java.util.List<String> idsToCancel = new java.util.ArrayList<>();
        for (int viewRow = 0; viewRow < jTableMyReservation.getRowCount(); viewRow++) {
            int modelRow = jTableMyReservation.convertRowIndexToModel(viewRow);
            Boolean checked = (Boolean) model.getValueAt(modelRow, 0);
            if (checked != null && checked) {
                idsToCancel.add(myReservationList.get(modelRow).getReservationId());
            }
        }

        if (idsToCancel.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "취소할 예약을 선택해주세요.");
            return;
        }

        int reply = javax.swing.JOptionPane.showConfirmDialog(this,
                idsToCancel.size() + "건의 예약을 취소하시겠습니까?", "예약 취소 확인",
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (reply != javax.swing.JOptionPane.YES_OPTION) return;

        cancelNext(idsToCancel, 0);
    }

    private void cancelNext(java.util.List<String> ids, int index) {
        if (index >= ids.size()) {
            javax.swing.JOptionPane.showMessageDialog(this, "취소 처리가 완료되었습니다.");
            loadReservationsFromServer();
            return;
        }

        ReservationController.getInstance().cancelReservation(
            ids.get(index),
            response -> {
                if (!response.isSuccess()) {
                    javax.swing.JOptionPane.showMessageDialog(this, "취소 실패: " + response.getMessage());
                }
                cancelNext(ids, index + 1);
            },
            error -> {
                javax.swing.JOptionPane.showMessageDialog(this, "통신 오류: " + error);
                cancelNext(ids, index + 1);
            }
        );
    }

    public void updateReservationTable() {
        String building = (String) jComboBoxBuildingNo.getSelectedItem();
        String floor    = (String) jComboBoxFloor.getSelectedItem();
        String room     = (String) jComboBox4.getSelectedItem();

        String[] columnNames = {"예약 날짜", "요일", "선택 교시", "예약자", "구분", "사용 목적", "동반 인원", "승인 상태"};
        javax.swing.table.DefaultTableModel model =
                new javax.swing.table.DefaultTableModel(columnNames, 0);

        if ("건물선택".equals(building) || "층 선택".equals(floor) || "강의실 선택".equals(room)) {
            jTable2.setModel(model);
            return;
        }

        String targetRoomName = building + " " + floor + "층 " + room + "호";

        List<ReservationDTO.Response> cache = ReservationController.getInstance().getReservationCache();
        if (cache != null) {
            for (ReservationDTO.Response info : cache) {
                if (targetRoomName.equals(info.getRoomName())) {
                    model.addRow(new Object[]{
                        info.getDate() != null ? info.getDate().toString() : "",
                        info.getDay() != null ? info.getDay() + "요일" : "",
                        info.getPeriodInfo(),
                        info.getUserName(),
                        info.getRoleType(),
                        info.getPurpose(),
                        info.getPartnerCount() + "명",
                        info.getStatus()
                    });
                }
            }
        }

        jTable2.setModel(model);
        jTable2.setRowHeight(25);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGoToReserve;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonLogout;
    private javax.swing.JComboBox<String> jComboBox4;
    private javax.swing.JComboBox<String> jComboBoxBuildingNo;
    private javax.swing.JComboBox<String> jComboBoxFloor;
    private javax.swing.JComboBox<String> jComboBoxSemester;
    private javax.swing.JComboBox<String> jComboBoxYear;
    private javax.swing.JLabel jLabelBuildingNo;
    private javax.swing.JLabel jLabelClassRoom;
    private javax.swing.JLabel jLabelClassRoomListTitle;
    private javax.swing.JLabel jLabelFloor;
    private javax.swing.JLabel jLabelSemester;
    private javax.swing.JLabel jLabelTime;
    private javax.swing.JLabel jLabelYear;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelCondition;
    private javax.swing.JPanel jPanelMyReservation;
    private javax.swing.JPanel jPanelTitle;
    private javax.swing.JScrollPane jScrollPaneMyReservation;
    private javax.swing.JScrollPane jScrollPaneReservationList;
    private javax.swing.JScrollPane jScrollPaneScheduleList;
    private javax.swing.JTabbedPane jTabbedPaneTop;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTableMyReservation;
    private javax.swing.JTable jTableTimeTable;
    // End of variables declaration//GEN-END:variables
}
