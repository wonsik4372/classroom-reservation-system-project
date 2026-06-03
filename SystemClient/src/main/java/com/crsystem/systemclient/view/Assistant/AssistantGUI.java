/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.crsystem.systemclient.view.Assistant;

import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemclient.controller.SessionManager;
import com.crsystem.systemclient.controller.TimetableController;
import java.util.Map;

/**
 *
 * @author wonsik
 */
public class AssistantGUI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AssistantGUI.class.getName());
    
    // 대기 목록과 전체 목록을 각각 저장해둘 리스트
    private java.util.List<com.crsystem.common.dto.ReservationDTO.Response> currentPendingList = new java.util.ArrayList<>();
    private java.util.List<com.crsystem.common.dto.ReservationDTO.Response> currentAllList = new java.util.ArrayList<>();

    private javax.swing.JComboBox<String> filterModeCombo;
    private javax.swing.JSpinner filterDateSpinner;

    private Map<String, Object> timetableData = null;

    public AssistantGUI() {
        initComponents();
        initCustomSettings();

        loadPendingReservations();
        loadAllReservations();

        UserDTO.Response currentUser = SessionManager.getInstance().getCurrentUser();
        this.setTitle("조교 메인 시스템 - [" + currentUser.getName() + "]님 로그인 중");
    }
    
    private void initCustomSettings() {
        this.setPreferredSize(new java.awt.Dimension(1150, 750));
        this.pack();
        this.setLocationRelativeTo(null);

        // 컬럼 클릭으로 정렬
        jTablePendingReservations.setAutoCreateRowSorter(true);
        jTableReservationList.setAutoCreateRowSorter(true);

        // 강의실 정보 탭 초기화
        jComboBoxBuilding.addActionListener(e -> updateRoomFloorCombo());
        jComboBoxFloor.addActionListener(e -> updateRoomTable());
        jButtonEdit.addActionListener(e -> openRoomModifyDialog());
        jButtonLogout.addActionListener(e -> jButtonLogoutActionPerformed(e));
        setupReservationFilter();
        loadTimetableData();

        // 더블 클릭 감지 -> 상세 정보 띄우기
        jTableReservationList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) { // 더블클릭 감지
                    showReservationDetailPopup();
                }
            }
        });
        
        jLabelTime.setText(getTime());
        new javax.swing.Timer(1000, e -> {
            jLabelTime.setText(getTime()); 
        }).start();
    }
    
    // ==========================================
    // 더블 클릭 시 상세 조회 팝업 
    // ==========================================
    private void showReservationDetailPopup() {
        int selectedViewRow = jTableReservationList.getSelectedRow();
        if (selectedViewRow == -1) return;

        int selectedRow = jTableReservationList.convertRowIndexToModel(selectedViewRow);
        com.crsystem.common.dto.ReservationDTO.Response selectedObj = currentAllList.get(selectedRow);

        String detailMessage = String.format(
            "============================\n" +
            "        예약 상세 정보\n" +
            "============================\n" +
            "▶ 예약 번호: %s\n" +
            "▶ 신청자 ID: %s (%s)\n" +
            "▶ 이름: %s\n" +
            "▶ 강의실: %s\n" +
            "▶ 목적: %s\n" +
            "▶ 동반 인원: %d명\n" +
            "▶ 예약일: %s\n" +
            "▶ 예약시간: %s\n" +
            "▶ 현재 상태: %s\n",
            selectedObj.getReservationId(),
            selectedObj.getUserId(), selectedObj.getRoleType().name(),
            selectedObj.getUserName(),
            selectedObj.getRoomName(),
            selectedObj.getPurpose(),
            selectedObj.getPartnerCount(),
            selectedObj.getDate(),
            selectedObj.getPeriodInfo(),
            selectedObj.getStatus().name()
        );

        // 만약 거절당한 예약이면 거절 사유도 추가로 보여줌
        if (selectedObj.getStatus() == com.crsystem.common.dto.ReservationDTO.Status.REJECTED) {
            detailMessage += "▶ 거절 사유: " + (selectedObj.getRejectReason() != null ? selectedObj.getRejectReason() : "없음") + "\n";
        }

        javax.swing.JOptionPane.showMessageDialog(
            this, 
            detailMessage, 
            "예약 상세 조회", 
            javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    // ==========================================
    // 대기 예약 현황 로드 
    // ==========================================
    private void loadPendingReservations() {
        // "PENDING" 상태의 예약만 달라고 컨트롤러에 요청
        com.crsystem.systemclient.controller.ReservationController.getInstance().getReservationList(
            "PENDING",
            (com.crsystem.common.dto.ResponseDTO response) -> {
                if (response.isSuccess()) {
                    // 서버가 보내준 예약 리스트 추출
                    currentPendingList= (java.util.List<com.crsystem.common.dto.ReservationDTO.Response>) response.getPayload();
                    
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.table.DefaultTableModel model = 
                            (javax.swing.table.DefaultTableModel) jTablePendingReservations.getModel();
                        
                        model.setRowCount(0); // 기존 데이터 싹 지우기 (초기화)
                        
                        if (currentPendingList != null) {
                            for (com.crsystem.common.dto.ReservationDTO.Response res : currentPendingList) {
                                // 넷빈즈 디자이너에서 설정한 컬럼 9개 순서에 맞춰 배열 생성
                                Object[] rowData = {
                                    false,               // 0: 선택 (체크박스)
                                    res.getRoleType() != null ? res.getRoleType().name() : "",   // 1: 구분 (교수/학생 등)
                                    res.getUserName(),   // 2: 이름
                                    res.getUserId(),     // 3: 학번/교번 (유저 ID)
                                    res.getRoomName(),     // 4: 강의실
                                    res.getPurpose(),    // 5: 목적
                                    res.getDate() != null ? res.getDate().toString() : "",       // 6: 예약일
                                    res.getPeriodInfo(),       // 7: 예약시간
                                    res.getPartnerCount()   // 8: 인원 수
                                };
                                model.addRow(rowData);
                            }
                        }
                    });
                } else {
                    System.err.println("대기 목록 조회 실패: " + response.getMessage());
                }
            },
            (String errorMessage) -> {
                System.err.println("네트워크 통신 에러: " + errorMessage);
            }
        );
    }
    // ==========================================
    // 전체 예약 현황 로드 
    // ==========================================
    private void setupReservationFilter() {
        filterModeCombo = new javax.swing.JComboBox<>(new String[]{"전체", "일별", "주별", "월별"});
        filterDateSpinner = new javax.swing.JSpinner(
            new javax.swing.SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        filterDateSpinner.setEditor(new javax.swing.JSpinner.DateEditor(filterDateSpinner, "yyyy-MM-dd"));
        filterDateSpinner.setPreferredSize(new java.awt.Dimension(120, 26));

        javax.swing.JPanel filterBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 6));
        filterBar.add(new javax.swing.JLabel("기간:"));
        filterBar.add(filterModeCombo);
        filterBar.add(new javax.swing.JLabel("기준일:"));
        filterBar.add(filterDateSpinner);

        jScrollPaneReservationSearch.setColumnHeaderView(filterBar);

        filterModeCombo.addActionListener(e -> applyReservationFilter());
        filterDateSpinner.addChangeListener(e -> applyReservationFilter());
    }

    private void applyReservationFilter() {
        if (currentAllList == null) return;
        String mode = (String) filterModeCombo.getSelectedItem();
        java.time.LocalDate base = ((java.util.Date) filterDateSpinner.getValue())
                .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        java.util.List<com.crsystem.common.dto.ReservationDTO.Response> filtered = currentAllList.stream()
            .filter(r -> {
                java.time.LocalDate d = r.getDate();
                if (d == null) return "전체".equals(mode);
                return switch (mode) {
                    case "일별" -> d.equals(base);
                    case "주별" -> {
                        java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
                        yield d.getYear() == base.getYear()
                            && d.get(wf.weekOfWeekBasedYear()) == base.get(wf.weekOfWeekBasedYear());
                    }
                    case "월별" -> d.getYear() == base.getYear() && d.getMonthValue() == base.getMonthValue();
                    default -> true;
                };
            })
            .collect(java.util.stream.Collectors.toList());

        javax.swing.table.DefaultTableModel model =
            (javax.swing.table.DefaultTableModel) jTableReservationList.getModel();
        model.setRowCount(0);
        for (com.crsystem.common.dto.ReservationDTO.Response res : filtered) {
            model.addRow(new Object[]{
                res.getRoleType(),
                res.getUserId(),
                res.getRoomName(),
                res.getPurpose(),
                res.getDate() != null ? res.getDate().toString() : "",
                res.getPeriodInfo(),
                res.getStatus()
            });
        }
    }

    private void loadAllReservations() {
        // "ALL" (또는 null)을 보내서 전체 예약을 달라고 컨트롤러에 요청
        com.crsystem.systemclient.controller.ReservationController.getInstance().getReservationList(
            "ALL", 
            (com.crsystem.common.dto.ResponseDTO response) -> {
                if (response.isSuccess()) {
                    currentAllList = (java.util.List<com.crsystem.common.dto.ReservationDTO.Response>) response.getPayload();
                    javax.swing.SwingUtilities.invokeLater(this::applyReservationFilter);
                } else {
                    System.err.println("전체 예약 목록 조회 실패: " + response.getMessage());
                }
            },
            (String errorMessage) -> {
                System.err.println("네트워크 통신 에러: " + errorMessage);
            }
        );
    }
    
    @SuppressWarnings("unchecked")
    private void loadTimetableData() {
        loadTimetableData(false);
    }

    @SuppressWarnings("unchecked")
    private void loadTimetableData(boolean refreshTableAfterLoad) {
        TimetableController.getInstance().getTimetable("2026",
            response -> {
                if (response.isSuccess() && response.getPayload() instanceof Map) {
                    timetableData = (Map<String, Object>) response.getPayload();
                    javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
                    model.addElement("건물 선택");
                    for (Object semObj : timetableData.values()) {
                        if (!(semObj instanceof Map)) continue;
                        for (String building : ((Map<String, Object>) semObj).keySet()) {
                            model.addElement(building);
                        }
                        break;
                    }
                    jComboBoxBuilding.setModel(model);
                    if (refreshTableAfterLoad) updateRoomTable();
                }
            },
            error -> logger.warning("시간표 로드 실패: " + error)
        );
    }

    @SuppressWarnings("unchecked")
    private void updateRoomFloorCombo() {
        if (timetableData == null) return;
        String building = (String) jComboBoxBuilding.getSelectedItem();
        javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();
        model.addElement("층 선택");
        if (!"건물 선택".equals(building)) {
            for (Object semObj : timetableData.values()) {
                if (!(semObj instanceof Map)) continue;
                Map<String, Object> buildingMap = (Map<String, Object>) ((Map<String, Object>) semObj).get(building);
                if (buildingMap != null) {
                    for (String floor : buildingMap.keySet()) {
                        model.addElement(floor.replace("층", "").trim());
                    }
                }
                break;
            }
        }
        jComboBoxFloor.setModel(model);
        updateRoomTable();
    }

    @SuppressWarnings("unchecked")
    private void updateRoomTable() {
        javax.swing.table.DefaultTableModel model =
                (javax.swing.table.DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        if (timetableData == null) return;

        String building = (String) jComboBoxBuilding.getSelectedItem();
        String floorNum = (String) jComboBoxFloor.getSelectedItem();
        if ("건물 선택".equals(building) || "층 선택".equals(floorNum)) return;

        String floorKey = floorNum + "층";
        for (Object semObj : timetableData.values()) {
            if (!(semObj instanceof Map)) continue;
            Map<String, Object> buildingMap = (Map<String, Object>) ((Map<String, Object>) semObj).get(building);
            if (buildingMap == null) break;
            Map<String, Object> floorMap = (Map<String, Object>) buildingMap.get(floorKey);
            if (floorMap == null) break;
            for (Map.Entry<String, Object> entry : floorMap.entrySet()) {
                String roomKey = entry.getKey();
                Map<String, Object> roomData = (Map<String, Object>) entry.getValue();
                Map<String, Object> info = (Map<String, Object>) roomData.get("info");
                if (info == null) continue;
                model.addRow(new Object[]{
                    building + " " + floorKey + " " + roomKey,
                    info.get("capacity"),
                    info.get("computerCount"),
                    info.get("status"),
                    info.get("features")
                });
            }
            break;
        }
        jTable1.setRowHeight(25);
    }

    @SuppressWarnings("unchecked")
    private void openRoomModifyDialog() {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "수정할 강의실을 선택해주세요.");
            return;
        }
        javax.swing.table.DefaultTableModel tableModel =
                (javax.swing.table.DefaultTableModel) jTable1.getModel();
        String roomName = (String) tableModel.getValueAt(selectedRow, 0);

        // 현재 info 구성
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("capacity",      tableModel.getValueAt(selectedRow, 1));
        info.put("computerCount", tableModel.getValueAt(selectedRow, 2));
        info.put("status",        tableModel.getValueAt(selectedRow, 3));
        info.put("features",      tableModel.getValueAt(selectedRow, 4));

        RoomModifyGUI panel = new RoomModifyGUI(roomName, info, () ->
            TimetableController.getInstance().getTimetable("2026",
                response -> {
                    if (response.isSuccess() && response.getPayload() instanceof Map) {
                        timetableData = (Map<String, Object>) response.getPayload();
                        updateRoomTable();
                    }
                },
                error -> logger.warning("시간표 로드 실패: " + error)
            )
        );
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, roomName + " 정보 수정", true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // 현재 시간을 가져와서 ISO 8601 형태로 설정
    private String getTime() {
        return java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'    'HH:mm:ss"));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPaneMain = new javax.swing.JTabbedPane();
        jPanelRoomInfo = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabelDeptNo = new javax.swing.JLabel();
        jComboBoxBuilding = new javax.swing.JComboBox<>();
        jLabelFloor = new javax.swing.JLabel();
        jComboBoxFloor = new javax.swing.JComboBox<>();
        jButtonEdit = new javax.swing.JButton();
        jPanelReservationMng = new javax.swing.JPanel();
        jScrollPaneReservationMng = new javax.swing.JScrollPane();
        jTablePendingReservations = new javax.swing.JTable();
        jButtonApprove = new javax.swing.JButton();
        jButtonReject = new javax.swing.JButton();
        jPanelReservationList = new javax.swing.JPanel();
        jScrollPaneReservationSearch = new javax.swing.JScrollPane();
        jTableReservationList = new javax.swing.JTable();
        jLabelTime = new javax.swing.JLabel();
        jButtonLogout = new javax.swing.JButton();
        jButtonRefresh = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] { "강의실", "수용 인원", "컴퓨터 수", "사용 여부", "특이사항" }
        ) {
            public boolean isCellEditable(int r, int c) { return false; }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jTable1);

        jLabelDeptNo.setText("건물");
        jComboBoxBuilding.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"건물 선택"}));

        jLabelFloor.setText("층");
        jComboBoxFloor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"층 선택"}));

        jButtonEdit.setText("수정하기");

        javax.swing.GroupLayout jPanelRoomInfoLayout = new javax.swing.GroupLayout(jPanelRoomInfo);
        jPanelRoomInfo.setLayout(jPanelRoomInfoLayout);
        jPanelRoomInfoLayout.setHorizontalGroup(
            jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 938, Short.MAX_VALUE)
                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                        .addComponent(jLabelDeptNo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxBuilding, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelFloor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxFloor, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonEdit)))
                .addContainerGap())
        );
        jPanelRoomInfoLayout.setVerticalGroup(
            jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelDeptNo)
                    .addComponent(jComboBoxBuilding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelFloor)
                    .addComponent(jComboBoxFloor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonEdit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("강의실 정보", jPanelRoomInfo);

        jTablePendingReservations.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "선택", "구분", "이름", "ID", "강의실 ", "목적", "예약일", "예약시간", "인원 수"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                true, true, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneReservationMng.setViewportView(jTablePendingReservations);
        if (jTablePendingReservations.getColumnModel().getColumnCount() > 0) {
            jTablePendingReservations.getColumnModel().getColumn(0).setMinWidth(50);
            jTablePendingReservations.getColumnModel().getColumn(0).setPreferredWidth(50);
            jTablePendingReservations.getColumnModel().getColumn(0).setMaxWidth(50);
        }

        jButtonApprove.setText("승인");
        jButtonApprove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonApproveActionPerformed(evt);
            }
        });

        jButtonReject.setText("거부");
        jButtonReject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRejectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelReservationMngLayout = new javax.swing.GroupLayout(jPanelReservationMng);
        jPanelReservationMng.setLayout(jPanelReservationMngLayout);
        jPanelReservationMngLayout.setHorizontalGroup(
            jPanelReservationMngLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPaneReservationMng, javax.swing.GroupLayout.DEFAULT_SIZE, 950, Short.MAX_VALUE)
            .addGroup(jPanelReservationMngLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButtonReject)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonApprove)
                .addContainerGap())
        );
        jPanelReservationMngLayout.setVerticalGroup(
            jPanelReservationMngLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelReservationMngLayout.createSequentialGroup()
                .addComponent(jScrollPaneReservationMng, javax.swing.GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelReservationMngLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonApprove)
                    .addComponent(jButtonReject))
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("예약 관리", jPanelReservationMng);

        jTableReservationList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "구분", "ID", "강의실", "목적", "예약일", "예약 시간", "상태"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPaneReservationSearch.setViewportView(jTableReservationList);

        javax.swing.GroupLayout jPanelReservationListLayout = new javax.swing.GroupLayout(jPanelReservationList);
        jPanelReservationList.setLayout(jPanelReservationListLayout);
        jPanelReservationListLayout.setHorizontalGroup(
            jPanelReservationListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReservationListLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPaneReservationSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 938, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelReservationListLayout.setVerticalGroup(
            jPanelReservationListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelReservationListLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPaneReservationSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 512, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPaneMain.addTab("예약 현황", jPanelReservationList);

        jLabelTime.setText("Current Time");

        jButtonLogout.setText("Logout");

        jButtonRefresh.setText("새로고침");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPaneMain)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonRefresh)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonLogout)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPaneMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelTime)
                    .addComponent(jButtonLogout)
                    .addComponent(jButtonRefresh))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // ==========================================
    // 거부 + 거부 사유 
    // ==========================================
    private void jButtonRejectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRejectActionPerformed
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTablePendingReservations.getModel();
        String selectedId = null;
        int checkedCount = 0;

        for (int viewRow = 0; viewRow < jTablePendingReservations.getRowCount(); viewRow++) {
            int modelRow = jTablePendingReservations.convertRowIndexToModel(viewRow);
            Boolean isChecked = (Boolean) model.getValueAt(modelRow, 0);
            if (isChecked != null && isChecked) {
                selectedId = currentPendingList.get(modelRow).getReservationId();
                checkedCount++;
            }
        }

        if (checkedCount != 1) {
            javax.swing.JOptionPane.showMessageDialog(this, "거부는 한 번에 하나의 예약만 선택 가능합니다.");
            return;
        }

        // 사유 입력 팝업
        String rejectReason = javax.swing.JOptionPane.showInputDialog(this, "거부 사유를 입력하세요:", "예약 거부", javax.swing.JOptionPane.PLAIN_MESSAGE);
        
        if (rejectReason != null && !rejectReason.trim().isEmpty()) {
            com.crsystem.systemclient.controller.ReservationController.getInstance().rejectReservation(
                selectedId, rejectReason,
                response -> {
                    if (response.isSuccess()) {
                        javax.swing.JOptionPane.showMessageDialog(this, "거부 처리 완료");
                        loadPendingReservations();
                        loadAllReservations();
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(this, "거부 실패: " + response.getMessage());
                    }
                },
                error -> javax.swing.JOptionPane.showMessageDialog(this, "통신 오류: " + error)
            );
        }
    }//GEN-LAST:event_jButtonRejectActionPerformed

    // ==========================================
    // 승인 
    // ==========================================
    private void jButtonApproveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonApproveActionPerformed
        // TODO add your handling code here:
        
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTablePendingReservations.getModel();
        java.util.List<String> idsToApprove = new java.util.ArrayList<>();

        // 테이블에 ID 컬럼이 없으므로 currentPendingList에서 행 인덱스로 직접 꺼냄
        // 정렬이 적용된 경우 뷰 인덱스 → 모델 인덱스 변환 필요
        for (int viewRow = 0; viewRow < jTablePendingReservations.getRowCount(); viewRow++) {
            int modelRow = jTablePendingReservations.convertRowIndexToModel(viewRow);
            Boolean isChecked = (Boolean) model.getValueAt(modelRow, 0);
            if (isChecked != null && isChecked) {
                idsToApprove.add(currentPendingList.get(modelRow).getReservationId());
            }
        }

        if (idsToApprove.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "승인할 예약을 선택해주세요.");
            return;
        }

        com.crsystem.systemclient.controller.ReservationController.getInstance().approveReservations(
            idsToApprove,
            response -> {
                if (response.isSuccess()) {
                    javax.swing.JOptionPane.showMessageDialog(this, "일괄 승인 완료");
                    loadPendingReservations(); // 갱신
                    loadAllReservations();     // 현황 탭도 갱신
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "승인 실패: " + response.getMessage());
                }
            },
            error -> javax.swing.JOptionPane.showMessageDialog(this, "통신 오류: " + error)
        );
    }//GEN-LAST:event_jButtonApproveActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        loadPendingReservations();
        loadAllReservations();
        System.out.println("새로고침 완료!");
    }//GEN-LAST:event_jButtonRefreshActionPerformed

    private void jButtonLogoutActionPerformed(java.awt.event.ActionEvent evt) {
        int reply = javax.swing.JOptionPane.showConfirmDialog(this,
                "로그아웃 하시겠습니까?", "로그아웃 확인",
                javax.swing.JOptionPane.YES_NO_OPTION);

        if (reply == javax.swing.JOptionPane.YES_OPTION) {
            com.crsystem.systemclient.controller.SessionManager.getInstance().logout();
            this.dispose();
            new com.crsystem.systemclient.view.LoginGUI().setVisible(true);
            System.out.println("로그아웃 완료. 로그인 화면으로 이동합니다.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new AssistantGUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonApprove;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonLogout;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JButton jButtonReject;
    private javax.swing.JComboBox<String> jComboBoxBuilding;
    private javax.swing.JComboBox<String> jComboBoxFloor;
    private javax.swing.JLabel jLabelDeptNo;
    private javax.swing.JLabel jLabelFloor;
    private javax.swing.JLabel jLabelTime;
    private javax.swing.JPanel jPanelReservationList;
    private javax.swing.JPanel jPanelReservationMng;
    private javax.swing.JPanel jPanelRoomInfo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneReservationMng;
    private javax.swing.JScrollPane jScrollPaneReservationSearch;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTablePendingReservations;
    private javax.swing.JTable jTableReservationList;
    // End of variables declaration//GEN-END:variables
}
