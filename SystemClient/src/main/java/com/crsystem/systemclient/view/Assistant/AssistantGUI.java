/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.crsystem.systemclient.view.Assistant;

import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemclient.controller.SessionManager;

/**
 *
 * @author wonsik
 */
public class AssistantGUI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AssistantGUI.class.getName());
    
    // 대기 목록과 전체 목록을 각각 저장해둘 리스트
    private java.util.List<com.crsystem.common.dto.ReservationDTO.Response> currentPendingList = new java.util.ArrayList<>();
    private java.util.List<com.crsystem.common.dto.ReservationDTO.Response> currentAllList = new java.util.ArrayList<>();

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
        this.setLocationRelativeTo(null); // 창을 화면 중앙에 배치

        // 컬럼 클릭으로 정렬
        jTablePendingReservations.setAutoCreateRowSorter(true);
        jTableReservationList.setAutoCreateRowSorter(true);

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
    private void loadAllReservations() {
        // "ALL" (또는 null)을 보내서 전체 예약을 달라고 컨트롤러에 요청
        com.crsystem.systemclient.controller.ReservationController.getInstance().getReservationList(
            "ALL", 
            (com.crsystem.common.dto.ResponseDTO response) -> {
                if (response.isSuccess()) {
                    currentAllList = (java.util.List<com.crsystem.common.dto.ReservationDTO.Response>) response.getPayload();
                    
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        javax.swing.table.DefaultTableModel model = 
                            (javax.swing.table.DefaultTableModel) jTableReservationList.getModel();
                        
                        model.setRowCount(0); // 테이블 초기화
                        
                        if (currentAllList != null) {
                            for (com.crsystem.common.dto.ReservationDTO.Response res : currentAllList) {
                                // 넷빈즈 디자이너에서 설정한 컬럼 7개 순서에 맞춰 배열 생성
                                Object[] rowData = {
                                    res.getRoleType(), // 0: 구분
                                    res.getUserId(),   // 1: ID
                                    res.getRoomName(),   // 2: 강의실
                                    res.getPurpose(),  // 3: 목적
                                    res.getDate() != null ? res.getDate().toString() : "",     // 4: 예약일
                                    res.getPeriodInfo(),     // 5: 예약 시간
                                    res.getStatus()    // 6: 상태 (APPROVED, PENDING 등)
                                };
                                model.addRow(rowData);
                            }
                        }
                    });
                } else {
                    System.err.println("전체 예약 목록 조회 실패: " + response.getMessage());
                }
            },
            (String errorMessage) -> {
                System.err.println("네트워크 통신 에러: " + errorMessage);
            }
        );
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

        jPanel1 = new javax.swing.JPanel();
        jPanelEtc = new javax.swing.JPanel();
        jTabbedPaneMain = new javax.swing.JTabbedPane();
        jPanelRoomInfo = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jButtonEdit = new javax.swing.JButton();
        jLabelDeptNo = new javax.swing.JLabel();
        jTextFieldDeptNo = new javax.swing.JTextField();
        jLabelFloor = new javax.swing.JLabel();
        jTextFieldFloor = new javax.swing.JTextField();
        jLabelRoomNo = new javax.swing.JLabel();
        jTextFieldRoomNo = new javax.swing.JTextField();
        jLabelMaxCap = new javax.swing.JLabel();
        jSpinnerMaxCap = new javax.swing.JSpinner();
        jLabelComputerCnt = new javax.swing.JLabel();
        jSpinnerComputerCnt = new javax.swing.JSpinner();
        jLabelUseable = new javax.swing.JLabel();
        jRadioButtonUseable = new javax.swing.JRadioButton();
        jRadioButtonUnusable = new javax.swing.JRadioButton();
        jLabelFeature = new javax.swing.JLabel();
        jTextFieldFeature = new javax.swing.JTextField();
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

        jPanel1.setPreferredSize(new java.awt.Dimension(850, 500));

        javax.swing.GroupLayout jPanelEtcLayout = new javax.swing.GroupLayout(jPanelEtc);
        jPanelEtc.setLayout(jPanelEtcLayout);
        jPanelEtcLayout.setHorizontalGroup(
            jPanelEtcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 838, Short.MAX_VALUE)
        );
        jPanelEtcLayout.setVerticalGroup(
            jPanelEtcLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 24, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelEtc, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(470, Short.MAX_VALUE)
                .addComponent(jPanelEtc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButtonEdit.setText("수정");

        jLabelDeptNo.setText("건물 번호");

        jTextFieldDeptNo.setText("jTextField1");

        jLabelFloor.setText("층 수");

        jTextFieldFloor.setText("jTextField1");

        jLabelRoomNo.setText("강의실 번호");

        jTextFieldRoomNo.setText("jTextField1");

        jLabelMaxCap.setText("수용 인원");

        jLabelComputerCnt.setText("컴퓨터 개수");

        jLabelUseable.setText("사용 가능 여부");

        jRadioButtonUseable.setText("사용 가능");

        jRadioButtonUnusable.setText("사용 불가");

        jLabelFeature.setText("특이사항");

        jTextFieldFeature.setText("jTextField1");

        javax.swing.GroupLayout jPanelRoomInfoLayout = new javax.swing.GroupLayout(jPanelRoomInfo);
        jPanelRoomInfo.setLayout(jPanelRoomInfoLayout);
        jPanelRoomInfoLayout.setHorizontalGroup(
            jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                        .addGap(219, 219, 219)
                        .addComponent(jButtonEdit))
                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelFeature)
                            .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                                    .addComponent(jLabelDeptNo)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTextFieldDeptNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                                    .addComponent(jLabelFloor)
                                    .addGap(66, 66, 66)
                                    .addComponent(jTextFieldFloor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelRoomNo)
                                    .addComponent(jLabelMaxCap)
                                    .addComponent(jLabelComputerCnt)
                                    .addComponent(jLabelUseable))
                                .addGap(18, 18, 18)
                                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldRoomNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                                        .addComponent(jRadioButtonUseable)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jRadioButtonUnusable))
                                    .addComponent(jSpinnerComputerCnt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jSpinnerMaxCap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addComponent(jTextFieldFeature, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelRoomInfoLayout.setVerticalGroup(
            jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelRoomInfoLayout.createSequentialGroup()
                .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(jPanelRoomInfoLayout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelDeptNo)
                            .addComponent(jTextFieldDeptNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelFloor)
                            .addComponent(jTextFieldFloor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelRoomNo)
                            .addComponent(jTextFieldRoomNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelMaxCap)
                            .addComponent(jSpinnerMaxCap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelComputerCnt)
                            .addComponent(jSpinnerComputerCnt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelRoomInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelUseable)
                            .addComponent(jRadioButtonUseable)
                            .addComponent(jRadioButtonUnusable))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabelFeature)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldFeature, javax.swing.GroupLayout.DEFAULT_SIZE, 229, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonEdit)))
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
        jButtonLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLogoutActionPerformed(evt);
            }
        });

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
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
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
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
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
    private javax.swing.JLabel jLabelComputerCnt;
    private javax.swing.JLabel jLabelDeptNo;
    private javax.swing.JLabel jLabelFeature;
    private javax.swing.JLabel jLabelFloor;
    private javax.swing.JLabel jLabelMaxCap;
    private javax.swing.JLabel jLabelRoomNo;
    private javax.swing.JLabel jLabelTime;
    private javax.swing.JLabel jLabelUseable;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelEtc;
    private javax.swing.JPanel jPanelReservationList;
    private javax.swing.JPanel jPanelReservationMng;
    private javax.swing.JPanel jPanelRoomInfo;
    private javax.swing.JRadioButton jRadioButtonUnusable;
    private javax.swing.JRadioButton jRadioButtonUseable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneReservationMng;
    private javax.swing.JScrollPane jScrollPaneReservationSearch;
    private javax.swing.JSpinner jSpinnerComputerCnt;
    private javax.swing.JSpinner jSpinnerMaxCap;
    private javax.swing.JTabbedPane jTabbedPaneMain;
    private javax.swing.JTable jTablePendingReservations;
    private javax.swing.JTable jTableReservationList;
    private javax.swing.JTextField jTextFieldDeptNo;
    private javax.swing.JTextField jTextFieldFeature;
    private javax.swing.JTextField jTextFieldFloor;
    private javax.swing.JTextField jTextFieldRoomNo;
    // End of variables declaration//GEN-END:variables
}
