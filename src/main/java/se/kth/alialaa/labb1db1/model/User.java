package se.kth.alialaa.labb1db1.model;

import java.util.ArrayList;

public class User {
    private String nameOfUser;
    private String password;
    private int userId;

    public User(String name, String password, int userId) {
        this.nameOfUser = name;
        this.password = password;
        this.userId = userId;
    }

    public String getNameOfUser() {
        return nameOfUser;
    }

    public String getPassword() {
        return password;
    }

    public int getUserId() {
        return userId;
    }

    public void setNameOfUser(String nameOfUser) {
        this.nameOfUser = nameOfUser;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }



    @Override
    public String toString() {
        return "User{" +
                "nameOfUser='" + nameOfUser + '\'' +
                ", password='" + password + '\'' +
                ", userId=" + userId +
                '}';
    }
}
