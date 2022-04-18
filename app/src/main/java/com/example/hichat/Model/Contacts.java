package com.example.hichat.Model;

public class Contacts {

    public String uid,name,status,profileImage;

    public Contacts()
    {

    }

    public Contacts(String uid,String name,String status, String profileImage) {
        this.uid=uid;
        this.name = name;
        this.status=status;
        this.profileImage = profileImage;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
