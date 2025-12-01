package com.example.unrewind;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText username, password;
    Button loginButton, signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.etUsername);
        password = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        signupButton = findViewById(R.id.btnsignup);  // add your signup button

        // Login button
        loginButton.setOnClickListener(v -> checkCredentials());

        // Signup button → go to SignupActivity
        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void checkCredentials() {
        String emailTxt = username.getText().toString().trim();
        String passTxt = password.getText().toString().trim();

        FirebaseAuth.getInstance().signInWithEmailAndPassword(emailTxt, passTxt)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid Login!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}