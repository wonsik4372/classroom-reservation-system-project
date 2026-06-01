/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.crsystem.systemserver.model;

import com.crsystem.systemserver.model.User;
import com.crsystem.systemserver.dao.UserFileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 사용자 목록
 *
 * ClientHandler 스레드들이 동시에 조회/추가/삭제할 수 있으므로
 * CopyOnWriteArrayList로 Thread Safety를 보장한다.
 *
 * @author wonsik
 */
public class UserCatalog {
    private final CopyOnWriteArrayList<User> userList;

    public UserCatalog(List<User> initialUsers) {
        this.userList = new CopyOnWriteArrayList<>(initialUsers);
        System.out.println("Catalog 초기화 완료. 현재 회원 수: " + userList.size());
    }

    // ====================
    // 목록 복사본 불러오기
    // ====================
    public List<User> getAllUsers() {
        return new ArrayList<>(this.userList);
    }

    // ====================
    // id에 해당하는 user 찾기
    // ====================
    public User findUser(String id) {
        for (User user : userList) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    // ====================
    // 사용자 추가
    // ====================
    public synchronized boolean addUser(User user) {
        if (findUser(user.getId()) != null) {
            System.out.println("가입 실패: 이미 존재하는 아이디입니다. (" + user.getId() + ")");
            return false;
        }
        this.userList.add(user);
        System.out.println("추가 성공! : (" + user.getId() + " : " + user.getName() + ") 이 추가되었습니다. ");
        return true;
    }

    // ====================
    // 사용자 삭제
    // ====================
    public boolean deleteUser(User user) {
        this.userList.remove(user);
        System.out.println("삭제 성공! : (" + user.getId() + " : " + user.getName() + ") 이 삭제되었습니다. ");
        return true;
    }
}
