package com.byronworkshop.ui.mainactivity.pojo;

import android.net.Uri;

import java.io.Serializable;

public class ByronUser implements Serializable {

    final private String uid;
    final private String name;
    final private String email;
    final private Uri photoUrl;

    public ByronUser(String uid, String name, String email, Uri photoUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Uri getPhotoUrl() {
        return photoUrl;
    }
}
