package com.quicklist.app.models;

import com.google.firebase.Timestamp;

public class Item {
    public String text;
    public boolean checked;
    public String createdBy;
    public Timestamp createdAt;

    public Item() {} // Needed by Firestore

    public Item(String text, String uid) {
        this.text = text;
        this.checked = false;
        this.createdBy = uid;
        this.createdAt = Timestamp.now();
    }
}
