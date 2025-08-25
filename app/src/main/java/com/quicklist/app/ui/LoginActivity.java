package com.quicklist.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.quicklist.app.R;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText emailInput, passwordInput;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button signupBtn = findViewById(R.id.signupBtn);

        if (auth.getCurrentUser() != null) goHome();

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(res -> { ensureUserDoc(); goHome(); })
                    .addOnFailureListener(e -> toast(e.getMessage()));
        });

        signupBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener((OnSuccessListener<AuthResult>) res -> {
                        ensureUserDoc();
                        goHome();
                    })
                    .addOnFailureListener(e -> toast(e.getMessage()));
        });
    }

    private void ensureUserDoc() {
        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("createdAt", com.google.firebase.Timestamp.now());
        db.collection("users").document(uid).set(data);
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}