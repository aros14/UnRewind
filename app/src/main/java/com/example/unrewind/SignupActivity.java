package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText firstName, email, birthday, username, password;
    Button createAccount;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        firstName = findViewById(R.id.etFirstName);
        email = findViewById(R.id.etEmail);
        birthday = findViewById(R.id.etBirthday);
        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        createAccount = findViewById(R.id.btnCreateAccount);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        createAccount.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = firstName.getText().toString().trim();
        String mail = email.getText().toString().trim();
        String bday = birthday.getText().toString().trim();
        String user = username.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (name.isEmpty() || mail.isEmpty() || bday.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(mail, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", name);
                        userData.put("email", mail);
                        userData.put("birthday", bday);
                        userData.put("username", user);
                        userData.put("uid", uid);

                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignupActivity.this,
                                            "Account Created!",
                                            Toast.LENGTH_SHORT).show();

                                    // Redirect to MainActivity
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish(); // closes SignupActivity so user can't go back
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(SignupActivity.this,
                                                "Error saving data",
                                                Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(SignupActivity.this,
                                "Signup Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}