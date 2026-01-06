package com.example.myapplication.model;

public class User {
    private String userId;
    private String fullName;
    private String email;
    private String password;

    // Required for Firestore serialization
    public User() {}

    public User(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }
    
    // Constructor for legacy calls (if any)
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }
 
    public void setUserId(String userId) {
        this.userId = userId;
    }
 
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isValid() {
        return email != null && !email.trim().isEmpty() && password != null && !password.isEmpty();
    }
}

