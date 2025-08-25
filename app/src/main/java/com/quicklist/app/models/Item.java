package com.quicklist.app.models;

import com.google.firebase.Timestamp;

public class Item {
    public String text;
    public String createdBy;
    public Timestamp createdAt;
    public boolean checked;
    public int quantity;

    // Firestore needs a no-arg constructor
    public Item() {}

    // Default: quantity = 1
    public Item(String text, String uid) {
        this.text = text;
        this.createdBy = uid;
        this.createdAt = Timestamp.now();
        this.checked = false;
        this.quantity = 1;
    }

    // Optional ctor if you want to pass a specific quantity
    public Item(String text, String uid, int quantity) {
        this.text = text;
        this.createdBy = uid;
        this.createdAt = Timestamp.now();
        this.checked = false;
        this.quantity = Math.max(1, quantity);
    }
}
