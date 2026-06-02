/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.crsystem.systemclient.view.Admin;

import com.crsystem.common.dto.RequestDTO;
import com.crsystem.common.dto.ResponseDTO;
import com.crsystem.common.dto.UserDTO;
import com.crsystem.systemclient.controller.SessionManager;
import com.crsystem.systemclient.controller.UserController;
import com.crsystem.systemclient.main.CRSystemClient;
import com.crsystem.systemclient.view.LoginGUI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 관리자 화면
 * @author wonsik
 */
public class AdminGUI extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AdminGUI.class.getName());
    
    public AdminGUI() {
        initComponents();
        this.setLocationRelativeTo(null);

        // 컬럼 클릭으로 정렬
        jTableUserList.setAutoCreateRowSorter(true);

        requestUserListFromServer();

        UserDTO.Response currentUser = SessionManager.getInstance().getCurrentUser();
        this.setTitle("관리자 시스템 - [" + currentUser.getName() + "]님 로그인 중");
    }

    // ==========================================
    // 비동기 순차 삭제
    // ==========================================
    private void deleteUsersSequentially(List<String> ids, int index) {
        // 선택한거 한 바퀴 돌아야함 
        if (index >= ids.size()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "선택한 사용자의 일괄 삭제 처리가 완료되었습니다.");
                requestUserListFromServer(); // 지우기가 완전히 끝났으므로 목록 최신화 새로고침!
            });
            return;
        }

        String currentTargetId = ids.get(index);

        UserController.getInstance().deleteUser(
            currentTargetId,
            (ResponseDTO response) -> {
                // 한 명 하고 나면 성공/실패 상관 없이 다음으로 넘어감 
                if (response.isSuccess()) {
                    System.out.println("ID [" + currentTargetId + "] 삭제 완료.");
                } else {
                    System.err.println("ID [" + currentTargetId + "] 삭제 실패: " + response.getMessage());
                }
                // 다음 사람 처리 
                deleteUsersSequentially(ids, index + 1);
            }, 
            (String errorMessage) -> {
                System.err.println("ID [" + currentTargetId + "] 통신 오류로 취소됨: " + errorMessage);
                // 에러나도 다음사람 처리 
                deleteUsersSequentially(ids, index + 1);
            }
        );
    }
    
    // ==========================================
    // 테이블 업데이트 
    // ==========================================
    public void updateTableData(List<UserDTO.Response> userList) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.table.DefaultTableModel model = 
                (javax.swing.table.DefaultTableModel) jTableUserList.getModel();

            // 테이블 지우기
            model.setRowCount(0);

            if (userList == null || userList.isEmpty()) return;

            // 새 데이터 채워넣기
            for (UserDTO.Response user : userList) {
                String roleString = (user.getRole() != null) ? user.getRole().name() : "미지정";

                // 초기상태 
                Object[] rowData = {
                    false,          // 1열: 선택
                    roleString,     // 2열: 구분
                    user.getName() != null ? user.getName() : "이름 없음", // 3열: 이름
                    user.getId() != null ? user.getId() : "ID 없음"        // 4열: ID
                };
                
                // 에러 방지 
                model.addRow(rowData);
            }
        });
    }
    
    // ==========================================
    // 리스트 새로고침 
    // ==========================================
    private void requestUserListFromServer() {
        // 비동기 요청 
        UserController.getInstance().getUserList( 
            (ResponseDTO response) -> {
                // 성공 콜백
                if (response.isSuccess()) {
                    // 서버가 Payload에 담아준 List<UserDto.Response>를 꺼내서 캐스팅
                    List<UserDTO.Response> userList = (List<UserDTO.Response>) response.getPayload();
                    
                    // 테이블 업데이트 함수 호출
                    updateTableData(userList);
                    System.out.println("사용자 목록 갱신 완료");
                } else {
                    JOptionPane.showMessageDialog(this, "조회 실패: " + response.getMessage());
                }
            }, 
            (String errorMessage) -> {
                // 실패 콜백 (네트워크 단절 등)
                JOptionPane.showMessageDialog(this, "통신 오류: " + errorMessage, "네트워크 에러", JOptionPane.ERROR_MESSAGE);
            }
        );
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPaneUserList = new javax.swing.JScrollPane();
        jTableUserList = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabelTitle = new javax.swing.JLabel();
        jButtonRefresh = new javax.swing.JButton();
        jButtonLogout = new javax.swing.JButton();
        jButtonAddUser = new javax.swing.JButton();
        jButtonDeleteUser = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTableUserList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "선택", "구분", "이름", "ID"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPaneUserList.setViewportView(jTableUserList);

        jLabelTitle.setFont(new java.awt.Font("Helvetica Neue", 0, 24)); // NOI18N
        jLabelTitle.setText("User Management");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelTitle)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButtonRefresh.setText("새로고침");
        jButtonRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshActionPerformed(evt);
            }
        });

        jButtonLogout.setText("Logout");
        jButtonLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLogoutActionPerformed(evt);
            }
        });

        jButtonAddUser.setText("사용자 추가");
        jButtonAddUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddUserActionPerformed(evt);
            }
        });

        jButtonDeleteUser.setText("사용자 삭제");
        jButtonDeleteUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteUserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPaneUserList, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 116, Short.MAX_VALUE)
                                .addComponent(jButtonRefresh)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonLogout))
                            .addComponent(jButtonAddUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonDeleteUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneUserList, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonAddUser, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteUser, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonRefresh)
                            .addComponent(jButtonLogout))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonAddUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddUserActionPerformed
        // TODO add your handling code here:
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "새 사용자 추가", true);
        
        // 2. 만들어둔 도화지(AddUserGUI 패널) 생성
        AddUserGUI addUserPanel = new AddUserGUI();
        
        // 3. 팝업창 뼈대 안에 도화지를 조립
        dialog.add(addUserPanel);
        
        // 4. [핵심] 도화지(AddUserGUI)가 가진 원래 크기만큼 팝업창 크기를 자동으로 맞춰줌!
        dialog.pack();
        
        // 5. 화면 정중앙에 띄우고 보이게 설정
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        // --- 이 아래 코드는 팝업창(JDialog)이 완전히 꺼진 직후에만 실행됩니다 ---
        // (사용자 추가가 끝났으니, 방금 추가된 최신 데이터를 서버에서 다시 불러와서 표를 새로고침!)
        System.out.println("사용자 추가 팝업 종료. 테이블 목록을 새로고침합니다.");
        requestUserListFromServer();
        
    }//GEN-LAST:event_jButtonAddUserActionPerformed

    private void jButtonLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLogoutActionPerformed
        // TODO add your handling code here:
        int reply = JOptionPane.showConfirmDialog(this, 
                "로그아웃 하시겠습니까?", "로그아웃 확인", 
                JOptionPane.YES_NO_OPTION);
                
        if (reply == JOptionPane.YES_OPTION) {
            this.dispose(); // 현재 창 닫기 
            
            // 로그인 초기 프레임 
            new LoginGUI().setVisible(true); 
            System.out.println("로그아웃 완료. 로그인 화면으로 이동합니다.");
        }
    }//GEN-LAST:event_jButtonLogoutActionPerformed

    private void jButtonDeleteUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteUserActionPerformed
        // TODO add your handling code here:
        javax.swing.table.DefaultTableModel model = 
                (javax.swing.table.DefaultTableModel) jTableUserList.getModel();
        
        // 체크된거 담을 곳 
        List<String> idsToDelete = new ArrayList<>();

        // 체크된거 수집 
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean isChecked = (Boolean) model.getValueAt(i, 0); // 체크여부 추출
            
            if (isChecked != null && isChecked) {
                String id = (String) model.getValueAt(i, 3); // ID 값 가져오기 
                
                // 관리자계정 삭제 방지 
                if (id != null && !"ID 없음".equals(id) && !"admin".equals(id)) {
                    idsToDelete.add(id);
                }
            }
        }

        // 체크가 없는 경우 
        if (idsToDelete.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "삭제할 사용자를 체크박스로 선택해주세요.", 
                    "선택된 사용자 없음", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 최종 확인 팝업
        int reply = JOptionPane.showConfirmDialog(this, 
                idsToDelete.size() + "명의 사용자를 완전히 삭제하시겠습니까?", 
                "일괄 삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                
        if (reply == JOptionPane.YES_OPTION) {
            System.out.println("일괄 삭제 시도 총 개수: " + idsToDelete.size());
            // 비동기 순차 삭제 헬퍼 함수 작동 호출 (0번 인덱스부터 출발)
            deleteUsersSequentially(idsToDelete, 0);
        }
    }//GEN-LAST:event_jButtonDeleteUserActionPerformed

    private void jButtonRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshActionPerformed
        // TODO add your handling code here:
        requestUserListFromServer(); 
    }//GEN-LAST:event_jButtonRefreshActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new AdminGUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddUser;
    private javax.swing.JButton jButtonDeleteUser;
    private javax.swing.JButton jButtonLogout;
    private javax.swing.JButton jButtonRefresh;
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPaneUserList;
    private javax.swing.JTable jTableUserList;
    // End of variables declaration//GEN-END:variables
}
